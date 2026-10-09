# 6. Benchmark con entradas reales (SQuAD v1.1)

Los capítulos anteriores describen *cómo* funciona la recuperación. Este capítulo responde a
otra pregunta, la que haría un revisor exigente: **¿funciona de verdad, y cómo lo sabemos?**
La respuesta no son ejemplos elegidos a mano, sino una medición de extremo a extremo sobre un
conjunto de datos público y real, con métricas estándar, un baseline y un control negativo.

El benchmark vive en
[`SquadBenchmarkTest`](../../src/test/java/com/angelo/docintel/eval/SquadBenchmarkTest.java) y
se ejecuta como parte de `mvn test`.

## 6.1 La tarea

SQuAD (*Stanford Question Answering Dataset*) es un conjunto de **preguntas sobre documentos**:
cada ejemplo trae un párrafo de Wikipedia (el *contexto*), una pregunta en lenguaje natural y
una o más **respuestas de oro** (*gold*), que son fragmentos literales de ese párrafo.

Eso encaja exactamente con lo que hace docintel, y da dos preguntas medibles:

1. **Recuperación**: dada la pregunta, ¿el motor recupera el párrafo del que se escribió la
   respuesta? (recall@k, MRR)
2. **Respuesta**: el texto que el motor devuelve, ¿contiene de verdad la respuesta de oro?
   (exact-match, token-F1, *containment*)

## 6.2 El corpus y el gold reales

Se versiona una porción determinista de SQuAD v1.1 bajo
[`src/test/resources/squad/`](../../src/test/resources/squad/) (proveniencia, licencia
CC BY-SA 4.0 y regeneración exacta en su `README.md`):

| | |
|---|---|
| Fuente | SQuAD v1.1 (`rajpurkar/squad` en Hugging Face), partición `validation` |
| Porción | las **primeras 1 200 preguntas** de validación |
| Documentos | **152** párrafos distintos (`corpus.jsonl`) |
| Preguntas | **1 200** con su documento de oro y sus respuestas (`questions.jsonl`) |
| Artículos | 4: `Super_Bowl_50`, `Warsaw`, `Normans`, `Nikola_Tesla` |

La mezcla de cuatro artículos es deliberada: obliga al motor tanto a **distinguir temas**
(un párrafo de fútbol americano de uno sobre Tesla) como a lo más difícil, **distinguir
párrafos del mismo tema** (dos párrafos distintos sobre la Super Bowl 50). Como hay 152
documentos y cada pregunta tiene exactamente uno de oro, el **azar** de acertar el primero es
≈ 1/152 ≈ **0,7 %**. Ese es el listón que la recuperación debe superar con holgura.

El cargador [`SquadDataset`](../../src/test/java/com/angelo/docintel/eval/SquadDataset.java)
lee ese snapshot y lo mete por **el pipeline propio de docintel**: `DocumentStore` (ingesta y
troceado en pasajes), `TfIdfRetriever` (índice TF-IDF del capítulo 2), `AnswerService` (el
umbral de *grounding* del capítulo 4) y `OfflineLlmClient` (la respuesta extractiva del
capítulo 3). No se reimplementa el motor: se mide el que ya existe.

## 6.3 Las métricas, con fórmulas

### Recuperación: recall@k y MRR

Para una pregunta, el motor devuelve una lista ordenada de pasajes; de cada uno miramos su
**documento**. Sea `g` el documento de oro y `rango(g)` la posición (empezando en 1) del
primer pasaje que pertenece a `g`.

- **recall@k** (acierto@k): 1 si `g` aparece entre los primeros `k`, si no 0.

```
recall@k(pregunta) = 1  si  rango(g) ≤ k
                     0  en otro caso
```

- **MRR** (*Mean Reciprocal Rank*): la media del **rango recíproco**. Premia poner el
  documento correcto arriba; si `g` no aparece en la profundidad considerada (aquí 10), su
  aporte es 0.

```
RR(pregunta) = 1 / rango(g)   (0 si g no está en el top-10)
MRR          = (1/N) · Σ RR(pregunta_i)
```

El valor agregado de recall@k es la media de los 1/0 sobre las `N = 1200` preguntas.

### Respuesta: exact-match, token-F1 y containment

Las tres comparan **texto normalizado** al estilo del script oficial de SQuAD: minúsculas,
borrar la puntuación ASCII (así `Levi's` → `levis`), quitar los artículos `a`/`an`/`the` y
colapsar espacios. Con varias respuestas de oro se toma el **máximo**.

- **exact-match (EM)**: 1 si la predicción normalizada es *igual* a alguna respuesta de oro.
- **token-F1**: media armónica de precisión y cobertura sobre el **multiconjunto** de tokens
  compartidos (`compartidos` cuenta repeticiones, no como conjunto):

```
precisión = compartidos / |tokens(predicción)|
cobertura = compartidos / |tokens(oro)|
F1        = 2 · precisión · cobertura / (precisión + cobertura)
```

- **containment**: 1 si alguna respuesta de oro normalizada es **subcadena** de la predicción
  normalizada. Es la métrica que corresponde a un respondedor *extractivo por frases*: mide si
  la frase que se muestra realmente **lleva** la respuesta.

Las implementaciones están en
[`RetrievalMetrics`](../../src/test/java/com/angelo/docintel/eval/RetrievalMetrics.java) y
[`SquadMetrics`](../../src/test/java/com/angelo/docintel/eval/SquadMetrics.java), y se prueban
sobre entradas minúsculas calculadas a mano (`RetrievalMetricsTest`, `SquadMetricsTest`).

## 6.4 Ejemplo numérico trabajado (con números reales)

### recall@k y MRR sobre cinco preguntas reales

Estos son rangos reales del motor (la posición del documento de oro) para cinco preguntas del
snapshot:

| Pregunta (abreviada) | doc. de oro | rango(g) | acierto@1 | acierto@3 | RR |
|---|:--:|:--:|:--:|:--:|:--:|
| "What day was the game played on?" | d000 | 1 | 1 | 1 | 1 |
| "…NFL champion for what season?" | d000 | 1 | 1 | 1 | 1 |
| "…Panthers player named MVP?" | d001 | 1 | 1 | 1 | 1 |
| "Who did the Panthers beat in the NFC Championship Game?" | d001 | 3 | 0 | 1 | 1/3 |
| "Where did Super Bowl 50 take place?" | d000 | >10 | 0 | 0 | 0 |

- recall@1 = (1+1+1+0+0)/5 = **0,60**
- recall@3 = (1+1+1+1+0)/5 = **0,80**
- MRR = (1 + 1 + 1 + 1/3 + 0)/5 = 3,333/5 = **0,667**

La última pregunta es un fallo real e instructivo: la respuesta ("Levi's Stadium… Santa Clara")
sí está en d000, pero otros párrafos sobre sedes de la Super Bowl comparten más vocabulario con
la pregunta y se cuelan por delante. Es el límite conocido de una señal **léxica** (capítulo 2);
el `Retriever` es la costura por donde entraría un recuperador semántico que lo arreglaría.

### token-F1 y containment sobre una respuesta real

Pregunta: *"What day was the game played on?"* · oro: `February 7, 2016`.
El motor recupera d000 (rango 1) y el `OfflineLlmClient` extrae la frase de mayor solape:

> "The game was played on February 7, 2016, at Levi's Stadium in the San Francisco Bay Area at
> Santa Clara, California."

Normalizada, la predicción tiene **19 tokens**; el oro `february 7 2016` tiene **3**. Comparten
`february`, `7`, `2016` → `compartidos = 3`.

```
precisión = 3/19 = 0,158
cobertura = 3/3  = 1,000
F1        = 2 · 0,158 · 1,000 / (0,158 + 1,000) = 0,273
```

`containment = 1`, porque `february 7 2016` es subcadena de la predicción. `EM = 0`, porque la
frase completa no es igual al lapso corto. Este contraste —containment alto, F1 moderado, EM
cero— es exactamente el perfil de un respondedor que devuelve **una frase literal** en lugar de
un lapso mínimo.

## 6.5 El baseline

Para saber si el TF-IDF aporta algo hace falta un punto de comparación sin señal. El
[`FirstKRetriever`](../../src/test/java/com/angelo/docintel/eval/FirstKRetriever.java) **ignora
la pregunta** y devuelve siempre los primeros `k` pasajes del corpus. Su recall es el nivel de
azar del corpus. Si el motor no superara esto, su "inteligencia" sería una ilusión.

## 6.6 El control negativo (y por qué DEBE colapsar)

Un buen resultado podría deberse a un artefacto (por ejemplo, que casi todas las preguntas
cayeran en pocos documentos). El **control negativo** descarta esa posibilidad: se **baraja el
emparejamiento pregunta↔documento** (con una semilla fija) y se evalúa el *mismo* ranking del
motor contra ese oro barajado.

El razonamiento: el motor sigue recuperando, para cada pregunta, su documento **verdadero**;
pero ahora se le exige acertar un documento **ajeno** asignado al azar. Si la señal es real, las
coincidencias caen al nivel de azar (≈ el baseline). Si el recall **no** colapsara, significaría
que el número alto no venía del emparejamiento pregunta↔documento, sino de un sesgo del montaje.
Por eso el control negativo **debe** colapsar: es la prueba de que el motor usa *grounding* real.

## 6.7 Veredicto, con los números

Medido sobre las 1 200 preguntas reales (el test fija estos valores y falla si cambian):

**Recuperación** (oro = párrafo fuente de SQuAD; azar ≈ 0,7 %):

| Métrica | Motor DocIntel | Baseline (first-K) | Barajado (control negativo) |
|---|--:|--:|--:|
| recall@1 | **0,651** | 0,025 | 0,008 |
| recall@3 | **0,822** | 0,025 | 0,033 |
| recall@5 | **0,869** | 0,046 | 0,058 |
| MRR@10 | **0,747** | 0,032 | 0,028 |

**Respuesta** (respondedor extractivo; recuperación del motor vs. recuperación ablacionada al
primer pasaje):

| Métrica | Recuperación del motor | Recuperación ablacionada |
|---|--:|--:|
| containment | **0,608** | 0,021 |
| token-F1 | **0,110** | 0,014 |
| exact-match | 0,000 | 0,000 |

**Lectura.** La recuperación TF-IDF coloca el párrafo de oro como **primer** resultado en el
**65 %** de 1 200 preguntas (26× el baseline) y entre los tres primeros en el **82 %**. La
respuesta fundamentada lleva el lapso de oro **29×** más a menudo que cuando se ablaciona la
recuperación (0,608 vs. 0,021), lo que demuestra que es la recuperación la que hace que las
respuestas estén fundamentadas. Y el **control negativo** hunde recall@1 de 0,651 a **0,008**,
justo al nivel de azar: el resultado proviene de *grounding* genuino, no de un artefacto.

El exact-match es 0 **por diseño**: el respaldo offline devuelve una **frase** literal, nunca un
lapso mínimo, así que puntúa bajo en métricas de *lapso exacto* y alto en *containment* —la
métrica que corresponde a su diseño—. Subir el exact-match es justamente la extensión de
*extracción de lapso* descrita en el README.

## 6.8 Reproducibilidad y verificación independiente

Todo es determinista: mismo corpus y misma consulta ⇒ mismos números, ejecución tras ejecución.
Además, los valores fijados en el test no son "lo que imprime el código": se **verificaron de
forma independiente** con una segunda implementación del pipeline (en Python) sobre el mismo
snapshot, que reprodujo cada cifra con cuatro decimales de acuerdo. El test afirma esos valores
de referencia con una tolerancia de 0,005, exige que el motor supere al baseline por un margen
amplio y comprueba que el control negativo colapsa.
