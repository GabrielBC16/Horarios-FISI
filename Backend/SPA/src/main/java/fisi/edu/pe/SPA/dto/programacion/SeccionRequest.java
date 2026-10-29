package fisi.edu.pe.SPA.dto.programacion;

import java.util.UUID;

public record SeccionRequest(
        UUID usuarioId,
        String codigo,
        String profesor,
        Integer capacidad,
        Integer cuposPlan2018,
        Integer cuposPlan2023) {
}
