# Planificación, edición y exportación de oferta

El usuario puede crear una planificación, importar y validar la demanda desde PDF, confirmar el cálculo, revisar y editar la oferta/secciones/horarios, y descargar el resultado actualizado en Excel. Las modificaciones están permitidas en todos los estados de planificación. Los endpoints de escritura requieren `usuarioId` para completar la auditoría de base de datos. Actualmente no hay autenticación: ese campo identifica al usuario que se registra en auditoría, pero no autentica ni autoriza al solicitante.

## Preparación y planificación

- `GET /api/escuelas`: escuelas activas disponibles.
- `GET /api/periodos-academicos`: periodos académicos disponibles.
- `POST /api/planificaciones`: crea una planificación en estado `BORRADOR`.

Ejemplo de creación:

```json
{
  "escuelaId": "UUID-ESCUELA",
  "periodoAcademicoId": "UUID-PERIODO",
  "usuarioId": "UUID-USUARIO"
}
```

La combinación de escuela y periodo debe ser única. Para consultar una planificación se usa `GET /api/planificaciones/{id}`; para listar las de una escuela, `GET /api/planificaciones/escuela/{escuelaId}`.

## Importar y calcular demanda

El backend recibe el PDF, ejecuta el extractor local de Python, descarta los datos del plan curricular 2015, valida cada código contra los planes curriculares de la escuela y devuelve una vista previa. La confirmación es una operación distinta: crea o actualiza las ofertas y genera las secciones propuestas `S01`, `S02`, etc.

`POST /api/planificaciones/{planificacionId}/importaciones/demanda`

Solicitud `multipart/form-data`:

- `archivo`: el PDF de demanda.
- `usuarioId`: UUID del usuario que realiza la importación. Este parámetro será reemplazado por el usuario autenticado cuando se integre seguridad.

La solicitud multipart incluye `archivo` y `usuarioId`. La respuesta incluye `importacionId`, filas válidas y filas con error. La importación queda validada cuando tiene menos de 12 filas inválidas; solo las filas válidas se procesan al confirmar.

`GET /api/importaciones/{importacionId}` devuelve la misma vista previa persistida.

`POST /api/importaciones/{importacionId}/confirmar` aplica la vista previa validada mediante el servicio de cálculo de oferta académica. Las filas inválidas no se procesan. Los cursos del plan 2018 con convalidación se agrupan con su curso equivalente del plan 2023; las demandas registradas para varios cursos 2018 con el mismo equivalente se suman también.

El número de secciones se calcula con la demanda total del grupo. Cada sección guarda cupos separados para planes 2018 y 2023; la suma no supera su capacidad (la base de datos también valida esta condición). Los cupos se distribuyen proporcionalmente a la demanda de cada plan y en total conservan las cantidades recibidas. La capacidad inicial de una sección nueva es 35, mientras que las secciones existentes conservan su capacidad editable. Una equivalencia utiliza un único juego compartido de secciones, visible al consultar cualquiera de sus ofertas relacionadas.

Luego guarda las ofertas de cada curso y crea o activa las secciones compartidas necesarias. Las secciones sobrantes quedan en estado `RETIRADA`. Las ofertas de cursos no convalidados continúan generando secciones independientes.

La respuesta cuenta las secciones una sola vez por grupo convalidado. La consulta `GET /api/validar/verSeccionesCalculadas/{planificacionId}` devuelve las ofertas, secciones activas, versiones y horarios; cada sección incluye `cuposPlan2018` y `cuposPlan2023`.

## Edición de ofertas, secciones y horarios

Las operaciones permiten editar la oferta y gestionar secciones y bloques horarios en cualquier estado. Las ediciones requieren la versión devuelta en la consulta para rechazar cambios obsoletos (`409 Conflict`).

- `PUT /api/planificaciones/{planificacionId}/ofertas/{ofertaId}`: edita demanda estimada, capacidad aplicada, secciones propuestas, situación y motivo.
- `POST /api/planificaciones/{planificacionId}/ofertas/{ofertaId}/secciones`: crea una sección; capacidad inicial 35 (configurable) y cupos separados por plan.
- `PUT /api/planificaciones/{planificacionId}/secciones/{seccionId}`: edita código, profesor, capacidad, cupos y estado.
- `DELETE /api/planificaciones/{planificacionId}/secciones/{seccionId}?usuarioId={uuid}&version={version}`: retira lógicamente la sección (preserva historial y horarios).
- `POST /api/planificaciones/{planificacionId}/secciones/{seccionId}/horarios`: añade un horario.
- `PUT /api/planificaciones/{planificacionId}/secciones/{seccionId}/horarios/{horarioId}`: edita un horario usando su versión.
- `DELETE /api/planificaciones/{planificacionId}/secciones/{seccionId}/horarios/{horarioId}?usuarioId={uuid}`: elimina físicamente un bloque horario.

Ejemplo de creación de sección:

```json
{
  "usuarioId": "UUID-USUARIO",
  "codigo": "S01",
  "profesor": "Nombre del docente",
  "capacidad": 35,
  "cuposPlan2018": 15,
  "cuposPlan2023": 20
}
```

Ejemplo de horario (días 1-7; formato de hora `HH:mm:ss`):

```json
{
  "usuarioId": "UUID-USUARIO",
  "dia": 2,
  "horaInicio": "09:00:00",
  "horaFin": "11:00:00"
}
```

Las horas superpuestas en una sección se rechazan con `409 Conflict`. En grupos convalidados, la creación desde una oferta del plan 2018 se dirige a la oferta canónica del plan 2023 cuando exista, para evitar dos juegos de secciones.

## Exportación

`GET /api/planificaciones/{planificacionId}/exportaciones/cupos` descarga un `.xlsx` generado con EasyExcel y contiene las hojas `Profesores` y `Por Semana`. En `Profesores` hay un bloque por curso/grupo equivalente con el ciclo, código y nombre de ambos planes, seguido de una tabla de secciones con cupos separados y total, profesor y horario. Los campos de profesor y horario quedan vacíos cuando no se han editado. Las secciones retiradas no se exportan. `Por Semana` lista los horarios de cada sección en filas semanales. Si la planificación no existe, responde `404 Not Found`.

El libro se genera dinámicamente con esos datos y nombres de hoja; no reutiliza la plantilla física del Excel de referencia ni replica sus estilos o su cuadrícula visual semanal.

Antes de desplegar el cambio, ejecutar una vez [add-convalidation-section-quotas.sql](./sql/add-convalidation-section-quotas.sql) para agregar las columnas de cupos y fijar el default de capacidad a 35. El default para secciones nuevas se puede cambiar en `app.oferta.capacidad-seccion-default`; una sección existente conserva su capacidad individual. Si se necesita recalcular el reparto de cupos de los registros actuales, volver a aplicar una importación validada.

## Configuración

En `application.properties`:

- `app.extractor.python`: ejecutable Python; por defecto `python`.
- `app.extractor.script`: ruta al extractor; por defecto `../extraccion/script_Pdf.py` al iniciar desde la carpeta `SPA`.
- `app.importacion.directorio`: carpeta local para conservar los PDF. En producción debe reemplazarse por almacenamiento de objetos, por ejemplo Supabase Storage.

El extractor debe tener `pdfplumber` instalado en el Python configurado.
