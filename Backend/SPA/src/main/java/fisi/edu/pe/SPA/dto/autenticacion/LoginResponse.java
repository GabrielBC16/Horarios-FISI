package fisi.edu.pe.SPA.dto.autenticacion;

import java.util.UUID;

public record LoginResponse(UUID usuarioId, String identificador, String nombre, String rol) {
}
