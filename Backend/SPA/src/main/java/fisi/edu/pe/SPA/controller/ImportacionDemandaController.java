package fisi.edu.pe.SPA.controller;

import fisi.edu.pe.SPA.dto.importacion.AplicacionImportacionDto;
import fisi.edu.pe.SPA.dto.importacion.VistaPreviaImportacionDto;
import fisi.edu.pe.SPA.services.CalcularOfertaAcademicaService;
import fisi.edu.pe.SPA.services.ExtractorPDFtoJsonService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ImportacionDemandaController {
    private final ExtractorPDFtoJsonService extractorService;
    private final CalcularOfertaAcademicaService calcularOfertaAcademicaService;

    public ImportacionDemandaController(
            ExtractorPDFtoJsonService extractorService,
            CalcularOfertaAcademicaService calcularOfertaAcademicaService) {
        this.extractorService = extractorService;
        this.calcularOfertaAcademicaService = calcularOfertaAcademicaService;
    }

    @PostMapping(path = "/planificaciones/{planificacionId}/importaciones/demanda", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VistaPreviaImportacionDto importarDemanda(
            @PathVariable("planificacionId") UUID planificacionId,
            @RequestParam("usuarioId") UUID usuarioId,
            @RequestParam("archivo") MultipartFile archivo) {
        return extractorService.extraerYValidar(planificacionId, usuarioId, archivo);
    }

    @GetMapping("/importaciones/{importacionId}")
    public VistaPreviaImportacionDto obtenerVistaPrevia(@PathVariable("importacionId") UUID importacionId) {
        return extractorService.obtenerVistaPrevia(importacionId);
    }

    @PostMapping("/importaciones/{importacionId}/confirmar")
    public AplicacionImportacionDto confirmar(@PathVariable("importacionId") UUID importacionId) {
        return calcularOfertaAcademicaService.procesarImportacion(importacionId);
    }
}
