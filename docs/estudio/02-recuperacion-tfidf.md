# 2. Recuperación con TF-IDF

La recuperación es el **núcleo determinista** del servicio: dado el mismo corpus y la misma
consulta, siempre produce el mismo orden de resultados, sin aleatoriedad y sin llamadas
externas. Está implementada a mano en el paquete `retrieval`.

## Tokenización (`Tokenizer`)

1. Se pasa el texto a minúsculas.
2. Se separa por secuencias de caracteres que no son alfanuméricos (`[^a-z0-9]+`).
3. Se descartan los tokens de un solo carácter y una pequeña lista de *stop-words* en
   inglés (`the`, `a`, `is`, `of`, `how`, …).

Quitar las palabras vacías evita que términos muy comunes dominen los pesos y mejora el
ranking.

## Pesos TF-IDF (`TfIdfIndex`)

Para un término `t` y un pasaje `p`:

- **TF** (frecuencia de término): el número de veces que `t` aparece en `p`.
- **DF** (frecuencia de documento): en cuántos pasajes aparece `t` al menos una vez.
- **IDF suavizado**: `idf(t) = ln((N + 1) / (df(t) + 1)) + 1`, donde `N` es el número de
  pasajes. El suavizado mantiene los pesos positivos y evita divisiones por cero para
  términos de la consulta que no están en el corpus.
- **Peso**: `tf(t, p) * idf(t)` por término; el vector del pasaje es el conjunto de esos
  pesos.

Un término de la consulta que no está en el corpus recibe el IDF máximo conocido: no aporta
nada al producto escalar (ningún pasaje lo contiene), pero sí reduce correctamente el coseno
de una consulta mayoritariamente fuera de vocabulario.

## Similitud del coseno

La relevancia entre la consulta `q` y un pasaje `d` es el coseno del ángulo entre sus
vectores:

```
cos(q, d) = (q · d) / (||q|| · ||d||)
```

Si alguna norma es cero, la similitud es 0. El producto escalar se calcula iterando sobre el
vector más pequeño. Los resultados se ordenan de mayor a menor puntuación, con desempate por
el id del pasaje para que la salida sea determinista, y se devuelven los `top-k` con
puntuación mayor que cero.

## Reconstrucción del índice (`TfIdfRetriever`)

El `DocumentStore` mantiene un contador de versión que se incrementa en cada cambio. El
`TfIdfRetriever` guarda el índice y la versión con la que lo construyó; cuando llega una
consulta y la versión ha cambiado, reconstruye el índice. Así, un documento añadido en
caliente por `POST /documents` es recuperable de inmediato.

## Intuición con un ejemplo

Para la consulta *"how long are uploaded documents retained"*, tras quitar las *stop-words*
quedan términos como `uploaded`, `documents`, `retained`. El pasaje *"Uploaded documents are
retained for 90 days by default."* comparte esos términos poco comunes (IDF alto), así que su
coseno es el más alto y queda primero. Una consulta como *"sourdough bread"* no comparte
ningún término: el coseno es 0 con todos los pasajes y no se devuelve nada (eso activa la
escalada, ver el capítulo 4).
