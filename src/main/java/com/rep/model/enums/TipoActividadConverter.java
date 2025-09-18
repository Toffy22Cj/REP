package com.rep.model.enums;

import com.rep.model.Actividad;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoActividadConverter implements AttributeConverter<Actividad.TipoActividad, String> {

    @Override
    public String convertToDatabaseColumn(Actividad.TipoActividad tipo) {
        if (tipo == null) {
            return null;
        }
        return tipo.getDbValue();
    }

    @Override
    public Actividad.TipoActividad convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Actividad.TipoActividad.forValue(dbData);
    }
}