# 1. Arquitectura general

`docintel` es un único servicio Spring Boot con un flujo de dependencias en una sola
dirección: la capa web llama al servicio de respuestas, que coordina la recuperación y la
cascada de LLM sobre un corpus en memoria. Nada por debajo de la capa web depende de HTTP, y
el núcleo de recuperación no depende del LLM.

## Capas (paquetes Java)

- **`web`** — adaptadores HTTP. `AskController` expone `POST /ask`; `DocumentController`
  expone `GET/POST /documents`; `GlobalExceptionHandler` traduce las excepciones a respuestas
  JSON de error. Los DTO (`AskRequest`, `AskResponse`, `CitationDto`, …) viven en `web/dto`.
- **`answer`** — orquestación. `AnswerService` aplica el patrón *ground-then-answer*:
  recupera, comprueba el umbral de grounding, pide la respuesta a la cascada y adjunta las
  citas. `Answer` y `Citation` son los modelos de dominio del resultado.
- **`retrieval`** — el núcleo determinista. `Tokenizer`, `TfIdfIndex` (frecuencias, IDF
  suavizado, coseno) y `TfIdfRetriever` (reconstruye el índice cuando cambia el corpus).
- **`llm`** — respuesta agnóstica de proveedor. La interfaz `LlmClient`, la
  `CascadeLlmClient`, los adaptadores HTTP (`AnthropicLlmClient`, `OpenAiLlmClient`), el
  `OfflineLlmClient` extractivo y el `PromptBuilder`.
- **`corpus`** — almacén en memoria. `DocumentStore` guarda documentos y pasajes;
  `PassageSplitter` divide un documento en pasajes.
- **`config`** — propiedades y ensamblaje. `LlmProperties` y `RetrievalProperties` enlazan
  `application.yml`; `LlmClientConfig` construye la cascada.

## Por qué esta separación

El objetivo es que la parte que **tiene que ser correcta** (recuperación, grounding, citas)
no dependa de ningún servicio externo. Así se puede probar de forma determinista y ejecutar
sin red. El LLM queda detrás de una interfaz pequeña y es intercambiable: añadir un proveedor
es escribir un adaptador y una línea de configuración.

## Flujo de una petición `POST /ask`

1. `AskController` valida que exista `question` y llama a `AnswerService.ask(...)`.
2. `AnswerService` pide a `TfIdfRetriever` los `top-k` pasajes.
3. Si el mejor pasaje no supera el umbral de similitud, devuelve una respuesta de escalada
   (`source=none`, sin citas) **sin consultar ningún LLM**.
4. Si hay grounding suficiente, construye un `GroundedRequest` y se lo pasa a la cascada.
5. La cascada devuelve texto etiquetado como `llm` o `extractive`; el servicio añade las
   citas y la confianza, y responde.

El diagrama de componentes y el de secuencia están en
[`docs/architecture.md`](../architecture.md).
