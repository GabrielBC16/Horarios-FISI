package fisi.edu.pe.SPA.controller;

import fisi.edu.pe.SPA.dto.convalidacion.ConvalidacionDto;
import fisi.edu.pe.SPA.dto.convalidacion.ConvalidacionRequestDto;
import fisi.edu.pe.SPA.services.ConvalidacionService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/convalidaciones")
public class ConvalidacionController {
    private final ConvalidacionService convalidacionService;

    public ConvalidacionController(ConvalidacionService convalidacionService) {
        this.convalidacionService = convalidacionService;
    }

    @GetMapping
    public List<ConvalidacionDto> listarTodas() {
        return convalidacionService.listarTodas();
    }

    @GetMapping("/ciclo/{ciclo}")
    public List<ConvalidacionDto> listarPorCiclo(@PathVariable Short ciclo) {
        return convalidacionService.listarPorCiclo2018(ciclo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConvalidacionDto crear(@RequestBody ConvalidacionRequestDto request) {
        return convalidacionService.crear(request);
    }

    @PutMapping("/{id}")
    public ConvalidacionDto actualizar(
            @PathVariable UUID id,
            @RequestBody ConvalidacionRequestDto request) {
        return convalidacionService.actualizar(id, request);
    }
}
