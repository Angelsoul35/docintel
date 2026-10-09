# 4. Grounding y citas

Dos decisiones protegen la fiabilidad del servicio: **no responder sin fundamento** y
**devolver siempre las pruebas**.

## El umbral de grounding

El peor fallo de un servicio de preguntas y respuestas es responder con seguridad a algo que
el corpus no cubre. Si la recuperación solo devuelve pasajes débiles e irrelevantes y aun así
se los pasamos al LLM, el modelo tenderá a "responder" rellenando huecos con su conocimiento
interno: una respuesta sin fundamento que parece fiable.

Para evitarlo, `AnswerService` aplica el patrón *ground-then-answer*:

1. Recupera los `top-k` pasajes.
2. Si el corpus está vacío o **la similitud del mejor pasaje está por debajo del umbral**
   configurable (`docintel.retrieval.similarity-floor`), devuelve un mensaje fijo —
   *"I don't have that information…"* — con `source=none`, `escalated=true`, sin citas y
   `confidence=0`, **sin consultar ningún LLM**.
3. Si hay fundamento suficiente, continúa hacia la cascada con esos pasajes.

La comprobación ocurre **antes** del LLM, así que se aplica igual al camino con modelo y al
offline, y no cuesta nada cuando se dispara. El umbral es configuración, de modo que se puede
calibrar por despliegue. Es un umbral global sobre una puntuación léxica: un instrumento
contundente, no fino; en producción se complementaría con comprobaciones de fidelidad a nivel
de respuesta.

### Elegir el umbral

Es un compromiso entre precisión y exhaustividad: demasiado alto y se escalan preguntas
válidas; demasiado bajo y se cuela contexto pobre. El valor por defecto (0.05) separa con
claridad "ningún término en común" (puntuación 0) de las coincidencias reales del corpus
sembrado.

## Por qué siempre devolvemos citas

Una respuesta que el usuario no puede verificar vale poco en un servicio documental. Como
tanto el camino con LLM como el extractivo trabajan sobre los mismos pasajes recuperados, la
evidencia siempre está disponible en el momento de responder.

Por eso, para toda respuesta con fundamento, `AnswerService` adjunta los pasajes como
`Citation` (`passageId`, `documentId`, `documentTitle`, `snippet`, `score`), venga el texto de
un LLM o del extractor. La confianza devuelta es la similitud del mejor pasaje. Cuando el
servicio escala, la lista de citas va vacía, lo que marca sin ambigüedad "sin respuesta
fundamentada".

Matiz importante: las citas muestran **la evidencia que se recuperó**, no demuestran por sí
solas que el texto redactado por un LLM refleje fielmente los pasajes. Medir esa fidelidad es
un paso aparte; el benchmark sobre SQuAD (capítulo 06) ya cuantifica cuánto de la respuesta
sale realmente del pasaje recuperado. En el camino extractivo la garantía es estructural: el
texto es un fragmento literal de un pasaje citado.
