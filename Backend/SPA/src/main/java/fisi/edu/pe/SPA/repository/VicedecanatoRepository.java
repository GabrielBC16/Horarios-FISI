package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.Vicedecanato;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VicedecanatoRepository extends JpaRepository<Vicedecanato, UUID> {
    Optional<Vicedecanato> findByCodigo(String codigo);
}
