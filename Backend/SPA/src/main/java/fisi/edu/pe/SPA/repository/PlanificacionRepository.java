package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.Planificacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PlanificacionRepository extends JpaRepository<Planificacion, UUID> {
    Optional<Planificacion> findByAreaEscuela_IdAndPeriodoAcademico_Id(UUID escuelaId, UUID periodoId);

    List<Planificacion> findByAreaEscuela_IdOrderByPeriodoAcademico_CodigoDesc(UUID escuelaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select planificacion from Planificacion planificacion where planificacion.id = :id")
    Optional<Planificacion> findByIdForUpdate(@Param("id") UUID id);
}
