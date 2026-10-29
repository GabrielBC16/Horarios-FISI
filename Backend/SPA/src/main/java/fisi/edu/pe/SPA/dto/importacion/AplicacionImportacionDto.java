package fisi.edu.pe.SPA.dto.importacion;

import java.util.UUID;

public record AplicacionImportacionDto(UUID importacionId, int ofertasActualizadas, int seccionesPropuestas) {
}
