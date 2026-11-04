package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.Importacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportacionRepository extends JpaRepository<Importacion, UUID> {
    List<Importacion> findByPlanificacion_IdOrderByImportadoEnDesc(UUID planificacionId);

    @Modifying
    @Query("delete from Importacion importacion where importacion.planificacion.id = :planificacionId")
    int deleteAllByPlanificacionId(@Param("planificacionId") UUID planificacionId);
}
