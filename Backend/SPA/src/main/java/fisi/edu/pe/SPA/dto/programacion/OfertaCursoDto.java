package fisi.edu.pe.SPA.dto.programacion;

import fisi.edu.pe.SPA.domain.enums.SituacionOferta;
import java.util.UUID;

public record OfertaCursoDto(
        UUID id,
        UUID cursoId,
        String codigoCurso,
        String nombreCurso,
        Integer estudiantesEstimados,
        Integer capacidadAplicada,
        Integer seccionesCalculadas,
        Integer seccionesPropuestas,
        SituacionOferta situacion,
        String motivoDecision,
        Long version) {
}
