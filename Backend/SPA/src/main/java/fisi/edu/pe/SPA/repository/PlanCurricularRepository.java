package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.PlanCurricular;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanCurricularRepository extends JpaRepository<PlanCurricular, UUID> {
    List<PlanCurricular> findByAreaEscuela_IdAndActivoTrue(UUID escuelaId);
}
