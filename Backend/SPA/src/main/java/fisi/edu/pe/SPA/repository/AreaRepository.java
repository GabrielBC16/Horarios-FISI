package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.Area;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, UUID> {
    Optional<Area> findByCodigo(String codigo);
    List<Area> findByActivoTrue();
}
