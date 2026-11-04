package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.CambioPlanificacion;
import fisi.edu.pe.SPA.domain.enums.EntidadAuditada;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CambioPlanificacionRepository extends JpaRepository<CambioPlanificacion, UUID> {
    List<CambioPlanificacion> findByPlanificacion_IdOrderByFechaAsc(UUID planificacionId);
    List<CambioPlanificacion> findByEntidadAndRegistroIdOrderByFechaAsc(EntidadAuditada entidad, UUID registroId);

    @Modifying
    @Query("delete from CambioPlanificacion cambio where cambio.planificacion.id = :planificacionId")
    int deleteAllByPlanificacionId(@Param("planificacionId") UUID planificacionId);
}
