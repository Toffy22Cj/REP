package com.rep.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Data
@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = "correo"),
        @UniqueConstraint(columnNames = "identificacion")
})
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(AuditingEntityListener.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public boolean isActivo() {
        return activo != null && activo;
    }

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "Debe ser una dirección de correo válida")
    @Column(nullable = false)
    private String correo;

    @NotBlank(message = "La identificación no puede estar vacía")
    @Column(name = "identificacion", nullable = false)
    private String identificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificacion", nullable = false)
    private TipoIdentificacion tipoIdentificacion;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Column(name = "contraseña", nullable = false)
    @JsonProperty("contraseña")  // Añadir esta anotación
    @JsonIgnore
    private String contraseña;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    // En Usuario.java
    @Min(5) @Max(120)
    @Column(nullable = true) // Cambiado a nullable
    private Integer edad;
    @Transient
    public Integer getEdadCalculada() {
        return this.edad;
    }
    @CreatedDate
    @Column(name = "creado_en", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creadoEn;

    @Column(name = "fecha_ingreso")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaIngreso;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activo = true;

    public enum TipoIdentificacion {
        CC("Cédula de Ciudadanía"),
        TI("Tarjeta de Identidad");

        private final String descripcion;
        TipoIdentificacion(String descripcion) {
            this.descripcion = descripcion;
        }
        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum Rol implements GrantedAuthority {
        ADMIN("Administrador"),
        PROFESOR("Profesor"),
        ESTUDIANTE("Estudiante");

        private final String nombre;

        Rol(String nombre) {
            this.nombre = nombre;
        }

        @Override
        public String getAuthority() {
            return name(); // Devuelve "ADMIN", "PROFESOR", etc. (sin prefijo)
        }

        public String getNombre() {
            return nombre;
        }
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(rol.name())); // Sin "ROLE_"
    }
    @JsonIgnore
    @Override
    public String getPassword() {
        return this.contraseña;
    }

    @Override
    public String getUsername() {
        return this.identificacion;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    protected void prePersistUsuario() {  // Renombrado y cambiado a protected
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
        if (this.activo == null) {
            this.activo = true;
        }

    }
    // En Usuario.java
    @Enumerated(EnumType.STRING)
    @Column(nullable = true) // Puedes cambiar a false si es obligatorio
    private Sexo sexo;

    // Añade este enum dentro de la clase Usuario
    public enum Sexo {
        MASCULINO("Masculino"),
        FEMENINO("Femenino"),
        OTRO("Otro");

        private final String descripcion;

        Sexo(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }
}