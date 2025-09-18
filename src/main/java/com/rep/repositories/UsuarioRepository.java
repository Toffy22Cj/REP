package com.rep.repositories;

import com.rep.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByNombre(String nombre);
    Optional<Usuario> findByIdentificacion(String identificacion);

    boolean existsByCorreo(String correo);
    boolean existsByNombre(String nombre);
    boolean existsByIdentificacion(String identificacion);

    // Consultas mejoradas
    List<Usuario> findByRol(Usuario.Rol rol);
    Page<Usuario> findByRol(Usuario.Rol rol, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.nombre) LIKE LOWER(concat('%', :query, '%'))")
    List<Usuario> buscarPorNombre(@Param("query") String query);

    String identificacion(String identificacion);
}