# HorariosFisiFrontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.1.2.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Configuración y mapa de la API

La URL base de la API se configura en `public/config.json`:

```json
{
  "apiBaseUrl": "http://localhost:8080/api"
}
```

La aplicación carga este archivo al iniciar. Para cambiar el servidor, actualiza `apiBaseUrl`; no es necesario recompilar. En un despliegue, publica el `config.json` correspondiente junto con los archivos del frontend. La URL debe incluir el prefijo `/api` si el backend lo usa. Se utiliza un archivo JSON de configuración en tiempo de ejecución porque las variables `.env` del frontend normalmente se incorporan al bundle durante la compilación.

Las llamadas HTTP están centralizadas en `src/app/core/auth.service.ts` y `src/app/core/planning-api.service.ts`. Todas usan la URL base configurada:

| Servicio | Método | Endpoint |
|---|---|---|
| AuthService | POST | `/auth/login` |
| AuthService | POST | `/auth/crearUsuario` |
| PlanningApiService | GET | `/escuelas` |
| PlanningApiService | GET | `/periodos-academicos` |
| PlanningApiService | POST | `/planificaciones` |
| PlanningApiService | GET | `/planificaciones/{id}` |
| PlanningApiService | GET | `/planificaciones/escuela/{escuelaId}` |
| PlanningApiService | DELETE | `/planificaciones/{id}?usuarioId={usuarioId}` |
| PlanningApiService | POST | `/planificaciones/{planificacionId}/importaciones/demanda` |
| PlanningApiService | GET | `/importaciones/{id}` |
| PlanningApiService | POST | `/importaciones/{id}/confirmar` |
| PlanningApiService | GET | `/validar/verSeccionesCalculadas/{id}` |
| PlanningApiService | PUT | `/planificaciones/{planificacionId}/ofertas/{ofertaId}` |
| PlanningApiService | POST | `/planificaciones/{planificacionId}/ofertas/{ofertaId}/secciones` |
| PlanningApiService | PUT | `/planificaciones/{planificacionId}/secciones/{seccionId}` |
| PlanningApiService | DELETE | `/planificaciones/{planificacionId}/secciones/{seccionId}?usuarioId={usuarioId}&version={version}` |
| PlanningApiService | POST | `/planificaciones/{planificacionId}/secciones/{seccionId}/horarios` |
| PlanningApiService | PUT | `/planificaciones/{planificacionId}/secciones/{seccionId}/horarios/{horarioId}` |
| PlanningApiService | DELETE | `/planificaciones/{planificacionId}/secciones/{seccionId}/horarios/{horarioId}?usuarioId={usuarioId}` |
| PlanningApiService | GET | `/planificaciones/{planificacionId}/exportaciones/cupos` |

Todos los endpoints anteriores se concatenan a `apiBaseUrl` (por ejemplo, `http://localhost:8080/api`).

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
