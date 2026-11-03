package fisi.edu.pe.SPA.services;

import fisi.edu.pe.SPA.domain.entity.Area;
import fisi.edu.pe.SPA.domain.entity.Usuario;
import fisi.edu.pe.SPA.dto.autenticacion.CreateUserRequest;
import fisi.edu.pe.SPA.dto.autenticacion.LoginRequest;
import fisi.edu.pe.SPA.dto.autenticacion.LoginResponse;
import fisi.edu.pe.SPA.repository.AreaRepository;
import fisi.edu.pe.SPA.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticacionService(UsuarioRepository usuarioRepository, AreaRepository areaRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.areaRepository = areaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.clave() == null || request.clave().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar email y clave.");
        }

        Usuario usuario = usuarioRepository.findByCorreo(request.email())
                .orElseThrow(AutenticacionService::credencialesInvalidas);
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getClaveHash() == null
                || !passwordEncoder.matches(request.clave(), usuario.getClaveHash())) {
            throw credencialesInvalidas();
        }

        return new LoginResponse(
                usuario.getId(),
                usuario.getIdentificador(),
                usuario.getNombre(),
                usuario.getRol());
    }

    public boolean crearUsuario(CreateUserRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.clave() == null || request.clave().isBlank()
                || request.nombre() == null || request.nombre().isBlank()
                || request.area() == null || request.area().isBlank()
                || request.rol() == null || request.rol().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar todos los campos requeridos.");
        }

        if (usuarioRepository.findByCorreo(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado.");
        }

        // Validamos el área
        if (!areaRepository.findByNombre(request.area()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El área indicada no existe.");
        }

        Area area = areaRepository.findByNombre(request.area()).get();

        Usuario usuario = new Usuario();
        usuario.setCorreo(request.email());
        usuario.setClaveHash(passwordEncoder.encode(request.clave()));
        usuario.setNombre(request.nombre());
        usuario.setArea(area);
        usuario.setRol(request.rol());

        usuarioRepository.save(usuario);
        return true;
    }

    private static ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.");
    }
}
