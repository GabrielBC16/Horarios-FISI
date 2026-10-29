# Entidades y repositorios JPA

El modelo implementa el DDL `planificacion_academica.sql` acordado para PostgreSQL. Usa Java 21, Jakarta Persistence, Hibernate y Spring Data JPA. No introduce Lombok ni modifica los datos de conexión.

## Organización

- `domain.entity`: entidades, bases UUID/versionadas y herencia de áreas.
- `domain.enums`: estados y tipos con los mismos valores de texto que el DDL.
- `repository`: interfaces `JpaRepository<Entidad, UUID>` y consultas por relaciones.

La clase abstracta `Area` usa herencia `SINGLE_TABLE`, con la columna discriminadora `area.tipo`. Los subtipos son `Escuela`, `Dac`, `Vicedecanato` y `OtraArea`. Cada subtipo tiene repositorio. Los tipos de área nuevos requieren su clase JPA o su representación como `OTRA`; los valores existentes en la base deben coincidir con los discriminadores.

Cada usuario tiene un área y un rol de texto, cuyos permisos administra el backend. Un plan curricular y una planificación pertenecen específicamente a una `Escuela`. No se crean entidades de permisos.

Las relaciones `ManyToOne` son diferidas y no tienen cascadas de borrado. Se navega de hijo a padre; los hijos se consultan mediante sus repositorios. Las entidades no deben exponerse directamente como respuestas JSON: los futuros controladores deben usar DTO para evitar consultas involuntarias de relaciones diferidas.

`curso.capacidad_seccion_default` se mapea como `capacidadSeccionDefault`. Una oferta conserva la capacidad aplicada. Una sección registra profesor, capacidad editable, cupos de planes 2018 y 2023, y estado; sus bloques están en `SeccionHorario`, con día ISO de 1 a 7 y horas `LocalTime`. Las secciones nuevas usan `app.oferta.capacidad-seccion-default` (35 por defecto); la capacidad de una sección ya creada se conserva en los recálculos. Los cupos por plan para grupos convalidados se asignan proporcionalmente a la demanda total, sin exceder la capacidad por sección.

Los JSONB usan `Map<String, Object>` y el tipo JSON de Hibernate. Las fechas con zona horaria usan `Instant`; los periodos usan `LocalDate`.

## Esquema existente y generación de valores

`spring.jpa.hibernate.ddl-auto` se cambió de `update` a `validate`. Las tablas se mapean explícitamente al esquema `academico`; no hace falta cambiar el esquema predeterminado de la conexión.

Antes de iniciar la aplicación contra Supabase hay que ejecutar el DDL acordado con sus defaults, restricciones, funciones y triggers. No se ejecutó ese DDL ni se inició la aplicación contra Supabase durante esta implementación. Hibernate no reemplaza los triggers ni puede reconstruirlos mediante `ddl-auto=update`.

`version_registro`, los timestamps y `secciones_calculadas` son valores generados por PostgreSQL. Se mapean con `@Generated`; las versiones también llevan `@Version`. No tienen setters públicos. Hibernate obtiene sus valores después de INSERT/UPDATE. Los UUID son generados por JPA al persistir una entidad nueva.

El trigger del DDL también incrementa la versión de la planificación cuando cambia un detalle. Si ya se había cargado la planificación en el mismo contexto de persistencia, se debe refrescar antes de volver a modificarla o exportar su versión. No se debe tomar la versión de una entidad desactualizada para el Excel.

## Auditoría y transacciones

Los triggers requieren `app.usuario_id` en la misma transacción y conexión que las escrituras. Un futuro servicio `@Transactional` debe establecer el contexto con una consulta nativa antes de guardar y hacer flush. Para correcciones de DAC también debe establecer `app.area_autora_id` y `app.intercambio_excel_id`.

```sql
select set_config('app.usuario_id', :usuarioId, true);
select set_config('app.motivo', :motivo, true);
```

Estos valores deben proceder de la identidad y operación verificadas por el backend. Un `repository.save` aislado sobre planificación, oferta, sección u horario fallará si no se ha establecido ese contexto. En operaciones sobre la programación hay que bloquear primero la fila de planificación de acuerdo con el protocolo descrito en la guía del DDL.

`CambioPlanificacion` lleva `@Immutable` y solo expone getters. Su repositorio es un `JpaRepository` como los demás, pero debe utilizarse únicamente para lectura: los registros los crean los triggers y la base rechaza su modificación/eliminación. La asignación del usuario importador no debe confundirse con la autoría de las correcciones externas.

Los servicios futuros deben verificar los estados del flujo, las condiciones para aprobar, los datos obligatorios de cada etapa y los conflictos de importación. La capa de entidades/repositorios no implementa esas reglas ni autenticación.

## Verificación local

`JpaModelTests` construye el modelo real de Hibernate y los 17 repositorios con sus consultas derivadas, utilizando el dialecto PostgreSQL y sin proveedor de conexiones. No lee las credenciales de la aplicación ni accede a Supabase.

```powershell
$env:GRADLE_USER_HOME = 'C:/Users/USER/.gradle'
.\gradlew.bat --offline --no-daemon compileJava test --tests '*JpaModelTests'
```

La prueba original `SpaApplicationTests` inicia el contexto completo y usa la configuración de la aplicación. No debe ejecutarse contra una base compartida sin confirmar antes el esquema y el entorno. Las pruebas de persistencia con PostgreSQL real y los triggers deben realizarse posteriormente sobre una base de pruebas aislada.
