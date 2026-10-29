package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.Curso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CursoRepository extends JpaRepository<Curso, UUID> {
    List<Curso> findByPlan_Id(UUID planId);
    Optional<Curso> findByPlan_IdAndCodigoIgnoreCase(UUID planId, String codigo);

    @Query("""
            select c from Curso c
            where c.plan.anio = :anio and c.plan.activo = true and c.activo = true
              and lower(trim(c.codigo)) = lower(trim(:valor))
              and (:planId is null or c.plan.id = :planId)
              and (:escuelaId is null or c.plan.areaEscuela.id = :escuelaId)
            """)
    List<Curso> buscarActivosPorAnioYCodigo(
            @Param("anio") Short anio,
            @Param("valor") String codigo,
            @Param("planId") UUID planId,
            @Param("escuelaId") UUID escuelaId);

    @Query("""
            select c from Curso c
            where c.plan.anio = :anio and c.plan.activo = true and c.activo = true
              and lower(trim(c.nombre)) = lower(trim(:valor))
              and (:planId is null or c.plan.id = :planId)
              and (:escuelaId is null or c.plan.areaEscuela.id = :escuelaId)
            """)
    List<Curso> buscarActivosPorAnioYNombre(
            @Param("anio") Short anio,
            @Param("valor") String nombre,
            @Param("planId") UUID planId,
            @Param("escuelaId") UUID escuelaId);
}
