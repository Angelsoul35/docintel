# 5. Pruebas y ejecución

## Filosofía de pruebas

Toda la batería corre **sin red y sin clave de API**. El perfil de pruebas
(`src/test/resources/application.yml`) deja `docintel.llm.providers` vacío, así que la
cascada contiene solo el cliente offline y ningún adaptador HTTP llega a construirse. Donde
se necesita simular un LLM "vivo", se usan dobles de prueba (`FakeLlmClient`,
`FailingLlmClient`) que no tocan la red.

Qué cubre cada prueba:

- **`TfIdfIndexTest`** — la recuperación devuelve el pasaje correcto para una consulta, los
  ordena de forma sensata, los términos raros pesan más vía IDF, una consulta fuera de
  vocabulario no devuelve nada y se respeta el límite `top-k`.
- **`TfIdfRetrieverTest`** — el índice se reconstruye cuando cambia el corpus (un documento
  añadido se recupera).
- **`CascadeLlmClientTest`** — sin proveedor vivo usa el offline; ante un proveedor
  disponible que falla, cae al siguiente; prefiere el primero disponible y se detiene; salta
  los no disponibles sin llamarlos.
- **`OfflineLlmClientTest`** — la respuesta es extractiva y es un fragmento literal del
  pasaje; elige la frase que solapa con la pregunta; falla si no hay contexto.
- **`AnswerServiceTest`** — la respuesta offline está fundamentada y siempre lleva citas;
  usa el LLM vivo cuando lo hay pero sigue citando; una pregunta fuera del corpus activa el
  umbral y escala; una pregunta vacía escala; la confianza refleja la similitud del mejor
  pasaje.
- **`AskControllerTest` / `DocumentControllerTest`** — `POST /ask` de extremo a extremo
  sobre el contexto real (recuperador real, corpus sembrado, respaldo offline): una pregunta
  dentro del corpus devuelve respuesta extractiva fundamentada con citas; una fuera del
  corpus escala; falta de `question` da 400; se listan y se añaden documentos.
- **`DocIntelApplicationTests`** — el contexto arranca y, sin clave, la cascada es solo
  `[offline]`.
- **`SquadDatasetTest` / `SquadMetricsTest` / `RetrievalMetricsTest`** — el cargador lee el
  snapshot real de SQuAD con la forma esperada (152 documentos, 1 200 preguntas, cada pregunta
  apunta a un documento existente) y las métricas (recall@k, MRR, exact-match, token-F1,
  containment) dan los valores calculados a mano sobre entradas minúsculas.
- **`SquadBenchmarkTest`** — el benchmark de entradas reales (capítulo 6): mete SQuAD v1.1 por
  el pipeline de docintel y afirma que el motor supera al baseline por mucho, que reproduce los
  valores de referencia verificados de forma independiente y que el control negativo colapsa.

## Ejecutar las pruebas

```bash
mvn test        # 39 pruebas, offline, sin clave (incluye el benchmark de SQuAD)
mvn -o test     # fuerza el modo offline de Maven (solo caché local), para demostrarlo
```

El benchmark imprime su tabla de métricas por la salida estándar durante `mvn test`.

## Ejecutar el servicio

```bash
mvn spring-boot:run
# arranca en http://localhost:8080 con el corpus sembrado y sin necesidad de clave
```

Para activar un LLM real, se define la variable de entorno del proveedor antes de arrancar:

```bash
ANTHROPIC_API_KEY=sk-ant-... mvn spring-boot:run
OPENAI_API_KEY=sk-...        mvn spring-boot:run
```

El orden de proveedores, los modelos y el umbral de similitud se ajustan en
`src/main/resources/application.yml`.

## Construir el contenedor

```bash
docker build -t docintel .
docker run -p 8080:8080 docintel
```
