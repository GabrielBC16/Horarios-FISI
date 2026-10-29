package fisi.edu.pe.SPA.services;

import org.springframework.stereotype.Service;

import fisi.edu.pe.SPA.repository.UsuarioRepository;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;

    public AutenticacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /*
     * public boolean validarCredenciales(String username, String password) {
     * 
     * return usuarioRepository.existsByUsernameAndPassword(username, password);
     * }
     */

}
