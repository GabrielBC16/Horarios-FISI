# Convalidaciones entre planes 2018 y 2023

La API permite crear y editar una equivalencia individual. Cada curso del plan 2018 solo puede tener una convalidación; un curso del plan 2023 puede recibir varias, por lo que varios registros representan la equivalencia de varios cursos antiguos con uno nuevo.

## Endpoints

- `POST /api/convalidaciones`: crea una convalidación y responde `201 Created`.
- `PUT /api/convalidaciones/{id}`: reemplaza los cursos de la convalidación y responde `200 OK`.
- `GET /api/convalidaciones`: lista todas las convalidaciones.
- `GET /api/convalidaciones/ciclo/{ciclo}`: lista las convalidaciones cuyo curso del plan 2018 pertenece al ciclo indicado.

Cada curso incluido en la respuesta muestra código, nombre y ciclo, junto con el año del plan y sus identificadores.

El cuerpo acepta `codigo` o `nombre` por cada curso, exactamente uno. La búsqueda por nombre es completa, insensible a mayúsculas/minúsculas y requiere coincidencia única. `planId` en la referencia del curso y `escuelaId` en el cuerpo son opcionales para desambiguar códigos o nombres repetidos. Solo se aceptan planes y cursos activos, y ambos cursos deben pertenecer a la misma escuela.

```json
{
  "curso2018": { "codigo": "MAT101" },
  "curso2023": { "nombre": "Matemáticas I" }
}
```

Para mapear dos cursos de 2018 al mismo curso de 2023, enviar dos `POST` con distinta referencia `curso2018` y la misma referencia `curso2023`.

Si una referencia no existe, el endpoint responde `422`; si es ambigua o el curso 2018 ya está asignado, responde `409`; si el formato no es válido, responde `400`.

## Ajuste de esquema

La relación de `curso_2023_id` cambió de uno-a-uno a muchos-a-uno. Si la tabla se creó anteriormente con una restricción `UNIQUE` sobre `curso_2023_id`, ejecutar una vez el script [allow-many-courses-per-equivalence.sql](./sql/allow-many-courses-per-equivalence.sql) contra PostgreSQL antes de registrar equivalencias 2:1. El script no elimina la unicidad de `curso_2018_id`.

Para que el cálculo de oferta consolide cursos convalidables en secciones compartidas, ejecutar también [add-convalidation-section-quotas.sql](./sql/add-convalidation-section-quotas.sql). La generación suma la demanda, reparte cupos por plan proporcionalmente y conserva el límite editable de capacidad de cada sección, cuyo valor inicial es 35.
