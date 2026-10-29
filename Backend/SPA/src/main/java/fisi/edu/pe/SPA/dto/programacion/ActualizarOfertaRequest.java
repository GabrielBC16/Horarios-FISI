package fisi.edu.pe.SPA.dto.programacion;

import fisi.edu.pe.SPA.domain.enums.SituacionOferta;
import java.util.UUID;

public record ActualizarOfertaRequest(
        UUID usuarioId,
        Long version,
        Integer estudiantesEstimados,
        Integer capacidadAplicada,
        Integer seccionesPropuestas,
        SituacionOferta situacion,
        String motivoDecision) {
}
