package fisi.edu.pe.SPA.dto.autenticacion;

public record CreateUserRequest(String email, String clave, String nombre, String area, String rol) {
}
