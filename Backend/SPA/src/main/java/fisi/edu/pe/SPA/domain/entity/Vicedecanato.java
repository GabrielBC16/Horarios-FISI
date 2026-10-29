package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import fisi.edu.pe.SPA.domain.enums.TipoArea;

@Entity
@DiscriminatorValue("VICEDECANATO")
public class Vicedecanato extends Area {
    public Vicedecanato() {}

    @Override
    public TipoArea getTipo() { return TipoArea.VICEDECANATO; }
}
