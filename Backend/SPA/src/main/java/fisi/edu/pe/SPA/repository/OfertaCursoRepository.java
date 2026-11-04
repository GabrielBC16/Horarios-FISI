package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfertaCursoRepository extends JpaRepository<OfertaCurso, UUID> {
    @EntityGraph(attributePaths = "curso")
    List<OfertaCurso> findByPlanificacion_Id(UUID planificacionId);

    @EntityGraph(attributePaths = {"curso", "planificacion"})
    List<OfertaCurso> findByPlanificacion_IdOrderByCurso_CicloAscCurso_CodigoAsc(UUID planificacionId);

    Optional<OfertaCurso> findByPlanificacion_IdAndCurso_Id(UUID planificacionId, UUID cursoId);

    @Modifying
    @Query("delete from OfertaCurso oferta where oferta.planificacion.id = :planificacionId")
    int deleteAllByPlanificacionId(@Param("planificacionId") UUID planificacionId);
}
