package fisi.edu.pe.SPA.dto.importacion;

import java.util.List;
import java.util.UUID;

public record VistaPreviaImportacionDto(
        UUID importacionId,
        String estado,
        int totalRegistros,
        int registrosValidos,
        int registrosInvalidos,
        List<FilaImportacionDto> filas) {
}
