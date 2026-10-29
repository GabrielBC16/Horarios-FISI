package fisi.edu.pe.SPA.controller;

import fisi.edu.pe.SPA.services.ExportarOfertaAcademicaService;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planificaciones/{planificacionId}/exportaciones")
public class ExportarOfertaAcademicaController {
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportarOfertaAcademicaService exportarOfertaAcademicaService;

    public ExportarOfertaAcademicaController(ExportarOfertaAcademicaService exportarOfertaAcademicaService) {
        this.exportarOfertaAcademicaService = exportarOfertaAcademicaService;
    }

    @GetMapping("/cupos")
    public ResponseEntity<byte[]> exportarCupos(@PathVariable("planificacionId") UUID planificacionId) {
        byte[] archivo = exportarOfertaAcademicaService.exportar(planificacionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX_MEDIA_TYPE);
        headers.setContentLength(archivo.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("oferta-academica-" + planificacionId + ".xlsx")
                .build());
        return ResponseEntity.ok().headers(headers).body(archivo);
    }
}

