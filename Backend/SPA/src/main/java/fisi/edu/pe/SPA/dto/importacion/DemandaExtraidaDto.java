package fisi.edu.pe.SPA.dto.importacion;

import java.util.Map;

public record DemandaExtraidaDto(
        Integer numero,
        String codigo,
        String asignatura,
        Map<String, Integer> planes) {
}
