package fisi.edu.pe.SPA.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fisi.edu.pe.SPA.domain.entity.Usuario;
import fisi.edu.pe.SPA.dto.autenticacion.LoginRequest;
import fisi.edu.pe.SPA.repository.AreaRepository;
import fisi.edu.pe.SPA.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class AutenticacionServiceTests {
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final AreaRepository areaRepository = mock(AreaRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AutenticacionService service = new AutenticacionService(usuarioRepository, areaRepository,
            passwordEncoder);

    @Test
    void returnsUserUuidWhenCredentialsAreValid() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId, true, passwordEncoder.encode("clave-segura"));
        when(usuarioRepository.findByIdentificador("docente")).thenReturn(Optional.of(usuario));

        var response = service.login(new LoginRequest("docente", "clave-segura"));

        assertEquals(usuarioId, response.usuarioId());
        assertEquals("docente", response.identificador());
        assertEquals("Docente", response.nombre());
        assertEquals("DOCENTE", response.rol());
    }

    @Test
    void rejectsInvalidCredentialsAndInactiveUsers() {
        Usuario usuario = usuario(UUID.randomUUID(), true, passwordEncoder.encode("clave-correcta"));
        when(usuarioRepository.findByIdentificador("docente")).thenReturn(Optional.of(usuario));

        ResponseStatusException wrongPassword = assertThrows(
                ResponseStatusException.class,
                () -> service.login(new LoginRequest("docente", "clave-incorrecta")));
        assertEquals(HttpStatus.UNAUTHORIZED, wrongPassword.getStatusCode());

        usuario.setActivo(false);
        ResponseStatusException inactiveUser = assertThrows(
                ResponseStatusException.class,
                () -> service.login(new LoginRequest("docente", "clave-correcta")));
        assertEquals(HttpStatus.UNAUTHORIZED, inactiveUser.getStatusCode());
    }

    @Test
    void rejectsUsersWithoutAPasswordHash() {
        Usuario usuario = usuario(UUID.randomUUID(), true, null);
        when(usuarioRepository.findByIdentificador("docente")).thenReturn(Optional.of(usuario));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.login(new LoginRequest("docente", "clave")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void rejectsRequestsWithoutBothCredentials() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.login(new LoginRequest("docente", " ")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private static Usuario usuario(UUID id, boolean activo, String claveHash) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setIdentificador("docente");
        usuario.setClaveHash(claveHash);
        usuario.setNombre("Docente");
        usuario.setRol("DOCENTE");
        usuario.setActivo(activo);
        return usuario;
    }
}
