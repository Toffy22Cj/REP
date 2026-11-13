package com.rep.dto.material;

import com.rep.model.Recurso;

import java.time.LocalDateTime;

/** Material de estudio tal como lo ven los clientes. */
public record MaterialDTO(
        Long id,
        String titulo,
        String descripcion,
        String nombreArchivo,
        String tipoContenido,
        Long tamanoBytes,
        boolean esImagen,
        String archivoUrl,
        Long materiaId,
        String materiaNombre,
        Long cursoId,
        String cursoNombre,
        Long profesorId,
        String profesorNombre,
        LocalDateTime fechaSubida) {

    public static MaterialDTO from(Recurso r) {
        var pm = r.getProfesorMateria();
        return new MaterialDTO(
                r.getId(),
                r.getNombre(),
                r.getDescripcion(),
                r.getNombreArchivo(),
                r.getTipo(),
                r.getTamanoBytes(),
                r.getTipo() != null && r.getTipo().startsWith("image/"),
                "/api/materiales/" + r.getId() + "/archivo",
                pm.getMateria().getId(),
                pm.getMateria().getNombre(),
                pm.getCurso().getId(),
                pm.getCurso().getNombre(),
                pm.getProfesor().getId(),
                (pm.getProfesor().getNombre() + " " + pm.getProfesor().getApellido()).trim(),
                r.getFechaSubida());
    }
}
