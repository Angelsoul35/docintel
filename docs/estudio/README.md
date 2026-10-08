# Guía de estudio — docintel

Esta carpeta explica, en español, cómo está construido `docintel` y por qué se tomó cada
decisión. Está pensada como material de estudio: se puede leer en orden para entender el
sistema de principio a fin, o por temas sueltos.

El código y la documentación principal del proyecto están en inglés (nivel B2); esta guía
es el acompañamiento en español.

## Índice

1. [Arquitectura general](01-arquitectura.md) — las capas del servicio y cómo encajan.
2. [Recuperación con TF-IDF](02-recuperacion-tfidf.md) — el núcleo determinista: tokenización,
   frecuencias, vectores TF-IDF y similitud del coseno.
3. [Cliente LLM y fallback offline](03-cliente-llm-y-fallback.md) — la abstracción agnóstica
   de proveedor, la cascada y el respaldo extractivo.
4. [Grounding y citas](04-grounding-y-citas.md) — el umbral de similitud, la escalada y por
   qué siempre devolvemos citas.
5. [Pruebas y ejecución](05-pruebas-y-ejecucion.md) — cómo se prueba todo sin red ni clave,
   y cómo se ejecuta el servicio.

## Idea central

`docintel` separa con cuidado **lo que debe ser correcto** (la recuperación) de **lo que es
opcional** (el modelo de lenguaje). La recuperación es determinista y está cubierta por
pruebas; el LLM solo redacta a partir del contexto recuperado; y si no hay clave, un
respaldo extractivo mantiene el servicio en pie. Además, si la recuperación no encuentra
nada relevante, el servicio **escala en lugar de inventar**.
