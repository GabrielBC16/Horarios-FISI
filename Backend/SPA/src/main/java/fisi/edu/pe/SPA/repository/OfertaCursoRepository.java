package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfertaCursoRepository extends JpaRepository<OfertaCurso, UUID> {
    @EntityGraph(attributePaths = "curso")
    List<OfertaCurso> findByPlanificacion_Id(UUID planificacionId);

    @EntityGraph(attributePaths = {"curso", "planificacion"})
    List<OfertaCurso> findByPlanificacion_IdOrderByCurso_CicloAscCurso_CodigoAsc(UUID planificacionId);

    Optional<OfertaCurso> findByPlanificacion_IdAndCurso_Id(UUID planificacionId, UUID cursoId);
}
