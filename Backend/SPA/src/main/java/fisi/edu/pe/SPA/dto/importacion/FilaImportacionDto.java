package fisi.edu.pe.SPA.dto.importacion;

import java.util.UUID;

public record FilaImportacionDto(
        Integer numero,
        String codigo,
        String asignatura,
        String plan,
        Integer estudiantesEstimados,
        UUID cursoId,
        boolean valida,
        String error) {
}
