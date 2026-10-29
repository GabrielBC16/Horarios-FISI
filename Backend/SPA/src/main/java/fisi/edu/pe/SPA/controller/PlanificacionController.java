package fisi.edu.pe.SPA.controller;

import fisi.edu.pe.SPA.dto.planificacion.CrearPlanificacionRequest;
import fisi.edu.pe.SPA.dto.planificacion.PlanificacionDto;
import fisi.edu.pe.SPA.services.PlanificacionService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planificaciones")
public class PlanificacionController {
    private final PlanificacionService planificacionService;

    public PlanificacionController(PlanificacionService planificacionService) {
        this.planificacionService = planificacionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanificacionDto crear(@RequestBody CrearPlanificacionRequest request) {
        return planificacionService.crear(request);
    }

    @GetMapping("/{id}")
    public PlanificacionDto obtener(@PathVariable("id") UUID id) {
        return planificacionService.obtener(id);
    }

    @GetMapping("/escuela/{escuelaId}")
    public List<PlanificacionDto> listarPorEscuela(@PathVariable("escuelaId") UUID escuelaId) {
        return planificacionService.listarPorEscuela(escuelaId);
    }
}

