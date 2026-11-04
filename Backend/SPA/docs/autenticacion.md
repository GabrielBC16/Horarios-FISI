# Autenticación

## Inicio de sesión

`POST /api/auth/login` recibe las credenciales en el cuerpo JSON:

```json
{
  "identificador": "usuario",
  "clave": "contraseña"
}
```

Cuando son válidas y el usuario está activo, responde con los datos básicos y
el UUID que el frontend puede usar como `usuarioId`:

```json
{
  "usuarioId": "00000000-0000-0000-0000-000000000000",
  "identificador": "usuario",
  "nombre": "Nombre del usuario",
  "rol": "DOCENTE"
}
```

Las credenciales incompletas producen `400`; las credenciales no válidas o el
usuario inactivo producen `401`.

Las claves se almacenan como hashes BCrypt en `academico.usuario.clave_hash`;
no se guarda la clave original. La columna se incorpora mediante la actualización
del esquema de JPA. Los usuarios existentes deben recibir un hash de clave antes
de poder iniciar sesión; esto debe hacerse desde un proceso administrativo de
confianza, nunca almacenando la clave en texto plano.

Este endpoint valida las credenciales y devuelve el UUID, pero no crea una
sesión ni un token. Por tanto, no sustituye la autorización de las demás rutas.
