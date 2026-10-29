package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.Dac;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DacRepository extends JpaRepository<Dac, UUID> {
    Optional<Dac> findByCodigo(String codigo);
}
