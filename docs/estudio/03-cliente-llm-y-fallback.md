# 3. Cliente LLM y fallback offline

El servicio puede usar un modelo de lenguaje para redactar una respuesta fluida, pero **no
depende de ninguno**. Todo pasa por una interfaz pequeña y una cascada de implementaciones.

## La interfaz `LlmClient`

```java
public interface LlmClient {
    String name();                 // "anthropic", "openai", "offline"
    boolean isAvailable();         // ¿se puede usar ahora? (p. ej. hay clave)
    LlmResult answer(GroundedRequest request) throws LlmException;
}
```

- `GroundedRequest` lleva la pregunta y los pasajes recuperados: todo lo que el cliente
  necesita para responder **solo** a partir del contexto.
- `LlmResult` lleva el texto, la fuente (`LLM` o `EXTRACTIVE`) y el id del proveedor.
- `LlmException` indica que un cliente disponible no pudo responder; es la señal para que la
  cascada pruebe el siguiente.

## Adaptadores HTTP (`AnthropicLlmClient`, `OpenAiLlmClient`)

Son clientes HTTP finos construidos sobre `java.net.http.HttpClient`. Comparten la fontanería
en `HttpLlmClient` (tiempos de espera, manejo de estados, conversión de errores). Cada uno
construye el prompt con `PromptBuilder` y lo envía al endpoint del proveedor.

El punto clave: un adaptador solo está **disponible** (`isAvailable()`) cuando su clave de
API está configurada. Sin clave, `LlmClientConfig` ni siquiera lo crea. Por eso **nunca se
ejercitan en las pruebas** ni hacen red en el arranque por defecto.

## El prompt con grounding (`PromptBuilder`)

El prompt fija el modelo al contexto: *"responde ÚNICAMENTE con los pasajes numerados; si no
contienen la respuesta, di que no tienes esa información; cita los pasajes que uses, p. ej.
[1]"*. Así, una respuesta de un LLM real queda tan anclada como la del respaldo extractivo.

## La cascada (`CascadeLlmClient`)

Recorre una lista ordenada de clientes:

1. Si el cliente no está disponible, lo salta.
2. Si está disponible, lo intenta; si lanza `LlmException`, registra el aviso y pasa al
   siguiente.
3. Devuelve el primer resultado correcto.

La lista termina siempre con el cliente offline, que está **siempre disponible**, de modo que
la cascada garantiza una respuesta aunque no haya clave ni red.

## El respaldo offline (`OfflineLlmClient`)

Es determinista y no toca la red. Compone la respuesta directamente desde el **mejor pasaje**:
elige, dentro de ese pasaje, la frase con más términos en común con la pregunta (si ninguna
coincide, devuelve el pasaje entero). El texto devuelto es siempre un fragmento literal de un
pasaje recuperado, así que **no puede desviarse del corpus**. Esto es, además, lo que permite
que toda la batería de pruebas corra sin red ni clave.

## Ensamblaje (`LlmClientConfig`)

Lee `docintel.llm.providers` (el orden a probar) y, para cada id, crea el adaptador solo si
hay clave. Al final añade el `OfflineLlmClient` y envuelve todo en la `CascadeLlmClient`. Sin
claves, la cascada contiene exactamente `[offline]`.
