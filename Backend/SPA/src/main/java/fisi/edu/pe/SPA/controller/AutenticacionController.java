package fisi.edu.pe.SPA.controller;

import fisi.edu.pe.SPA.dto.autenticacion.CreateUserRequest;
import fisi.edu.pe.SPA.dto.autenticacion.LoginRequest;
import fisi.edu.pe.SPA.dto.autenticacion.LoginResponse;
import fisi.edu.pe.SPA.services.AutenticacionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {
    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return autenticacionService.login(request);
    }

    @PostMapping("/crearUsuario")
    @ResponseStatus(HttpStatus.CREATED)
    public boolean crearUsuario(@RequestBody CreateUserRequest request) {
        return autenticacionService.crearUsuario(request);
    }

}
