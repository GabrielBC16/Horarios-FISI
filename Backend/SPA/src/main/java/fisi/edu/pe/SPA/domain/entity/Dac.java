package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import fisi.edu.pe.SPA.domain.enums.TipoArea;

@Entity
@DiscriminatorValue("DAC")
public class Dac extends Area {
    public Dac() {}

    @Override
    public TipoArea getTipo() { return TipoArea.DAC; }
}
