package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.PeriodoAcademico;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademico, UUID> {
    Optional<PeriodoAcademico> findByCodigo(String codigo);

    List<PeriodoAcademico> findAllByOrderByCodigoDesc();
}
