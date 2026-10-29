package fisi.edu.pe.SPA.services;

import fisi.edu.pe.SPA.domain.entity.Usuario;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaPlanificacionService {
    private final EntityManager entityManager;

    public AuditoriaPlanificacionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void establecerContexto(Usuario usuario, String motivo) {
        UUID usuarioId = usuario.getId();
        UUID areaId = usuario.getArea().getId();
        entityManager.createNativeQuery("select set_config('app.usuario_id', :valor, true)")
                .setParameter("valor", usuarioId.toString())
                .getSingleResult();
        entityManager.createNativeQuery("select set_config('app.area_autora_id', :valor, true)")
                .setParameter("valor", areaId.toString())
                .getSingleResult();
        entityManager.createNativeQuery("select set_config('app.motivo', :valor, true)")
                .setParameter("valor", motivo)
                .getSingleResult();
    }
}
