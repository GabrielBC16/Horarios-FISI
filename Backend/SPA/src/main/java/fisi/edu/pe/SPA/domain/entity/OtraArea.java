package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import fisi.edu.pe.SPA.domain.enums.TipoArea;

@Entity
@DiscriminatorValue("OTRA")
public class OtraArea extends Area {
    public OtraArea() {}

    @Override
    public TipoArea getTipo() { return TipoArea.OTRA; }
}
