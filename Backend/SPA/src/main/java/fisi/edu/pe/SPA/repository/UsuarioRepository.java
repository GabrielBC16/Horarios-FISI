package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByIdentificador(String identificador);

    List<Usuario> findByArea_Id(UUID areaId);

    // boolean existsByUsernameAndPassword(String username, String password);
}
