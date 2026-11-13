/**
 * Validación de formularios en el navegador. Replica las reglas del backend
 * (RegistroUsuarioDTO y DocumentoIdentidadPolicy) para avisar antes de enviar;
 * el backend sigue siendo la fuente de verdad.
 */
export const Reglas = {
    MAYORIA_EDAD: 18,
    CC_MIN: 6,
    CC_MAX: 10,
    TI_DIGITOS: 10,
    PASSWORD_MIN: 8,
};

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Devuelve { campo: mensaje } con los errores encontrados (vacío si es válido). */
export function validarUsuario(u) {
    const e = {};
    if (!u.nombre || u.nombre.trim().length < 2) e.nombre = 'Mínimo 2 caracteres';
    if (!u.apellido || u.apellido.trim().length < 2) e.apellido = 'Mínimo 2 caracteres';
    if (!EMAIL.test(u.correo || '')) e.correo = 'Correo no válido';
    if (!u.contraseña || u.contraseña.length < Reglas.PASSWORD_MIN) e.contraseña = `Mínimo ${Reglas.PASSWORD_MIN} caracteres`;

    const id = (u.identificacion || '').trim();
    const edad = u.edad === null || u.edad === '' ? null : Number(u.edad);
    if (!/^\d+$/.test(id)) {
        e.identificacion = 'Solo dígitos';
    } else if (u.tipoIdentificacion === 'CC' && (id.length < Reglas.CC_MIN || id.length > Reglas.CC_MAX)) {
        e.identificacion = `La CC debe tener entre ${Reglas.CC_MIN} y ${Reglas.CC_MAX} dígitos (tiene ${id.length})`;
    } else if (u.tipoIdentificacion === 'TI' && id.length !== Reglas.TI_DIGITOS) {
        e.identificacion = `La TI debe tener exactamente ${Reglas.TI_DIGITOS} dígitos (tiene ${id.length})`;
    }

    if (edad !== null) {
        if (Number.isNaN(edad) || edad < 5 || edad > 120) e.edad = 'Entre 5 y 120';
        else if (u.tipoIdentificacion === 'CC' && edad < Reglas.MAYORIA_EDAD) e.tipoIdentificacion = 'CC solo para mayores de 18';
        else if (u.tipoIdentificacion === 'TI' && edad >= Reglas.MAYORIA_EDAD) e.tipoIdentificacion = 'TI solo para menores de 18';
    }

    if (u.rol === 'ESTUDIANTE') {
        if (edad === null) e.edad = 'Obligatoria para estudiantes';
        if (!u.cursoId) e.cursoId = 'Seleccione un curso (cree uno en Académico si no hay)';
    }
    return e;
}

/** Pinta los errores bajo cada campo. `mapa` relaciona nombre de campo -> id del input. */
export function mostrarErrores(form, errores, mapa) {
    limpiarErrores(form);
    Object.entries(errores).forEach(([campo, mensaje]) => {
        const input = form.querySelector('#' + (mapa[campo] || campo));
        if (!input) return;
        input.classList.add('input-error');
        const small = document.createElement('small');
        small.className = 'field-error';
        small.textContent = mensaje;
        input.insertAdjacentElement('afterend', small);
    });
    const primero = form.querySelector('.input-error');
    if (primero) primero.focus();
}

export function limpiarErrores(form) {
    form.querySelectorAll('.field-error').forEach(el => el.remove());
    form.querySelectorAll('.input-error').forEach(el => el.classList.remove('input-error'));
}
