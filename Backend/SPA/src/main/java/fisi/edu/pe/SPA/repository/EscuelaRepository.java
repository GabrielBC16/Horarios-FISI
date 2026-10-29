package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.Escuela;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscuelaRepository extends JpaRepository<Escuela, UUID> {
    Optional<Escuela> findByCodigo(String codigo);

    List<Escuela> findByActivoTrueOrderByNombreAsc();
}
