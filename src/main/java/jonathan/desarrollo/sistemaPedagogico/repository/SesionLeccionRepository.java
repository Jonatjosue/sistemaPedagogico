// SesionLeccionRepository.java
package jonathan.desarrollo.sistemaPedagogico.repository;

import jonathan.desarrollo.sistemaPedagogico.model.SesionLeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SesionLeccionRepository extends JpaRepository<SesionLeccion, Long> {
    List<SesionLeccion> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);

    List<SesionLeccion> findByUsuarioIdAndFechaFinIsNull(Long usuarioId);

    interface RegistroVista {
        Long getId();
        String getNombre();
        String getCodigo();
        String getAcceso();
        String getLeccion();
        java.time.LocalDateTime getInicio();
        java.time.LocalDateTime getFin();
        SesionLeccion.Estado getEstado();
        Long getAcertados();
        Long getFallidos();
    }

    @Query(value = """
            select s.id as id, u.nombre as nombre, u.codigo as codigo, u.accesoUsuario as acceso,
                   s.leccionTitulo as leccion, s.fechaInicio as inicio, s.fechaFin as fin, s.estado as estado,
                   (select count(i) from Intento i where i.sesionLeccion = s and i.exito = true) as acertados,
                   (select count(i) from Intento i where i.sesionLeccion = s and i.exito = false) as fallidos
            from SesionLeccion s join s.usuario u
            where (:uid = 0L or u.id = :uid)
            order by s.fechaInicio desc, s.id desc
            """,
            countQuery = "select count(s) from SesionLeccion s where (:uid = 0L or s.usuario.id = :uid)")
    org.springframework.data.domain.Page<RegistroVista> registros(
            @org.springframework.data.repository.query.Param("uid") Long uid,
            org.springframework.data.domain.Pageable pageable);
    interface UsoVista {
        String getLeccion();
        Long getUsos();
        Long getAcertados();
        Long getFallidos();
    }

    @Query("""
            select s.leccionTitulo as leccion, count(distinct s.id) as usos,
                   coalesce(sum(case when i.exito = true then 1 else 0 end), 0L) as acertados,
                   coalesce(sum(case when i.exito = false then 1 else 0 end), 0L) as fallidos
            from SesionLeccion s left join Intento i on i.sesionLeccion = s
            where s.usuario.tipo = :tipo and s.fechaInicio >= :desde and s.fechaInicio < :hasta
            group by s.leccionTitulo
            """)
    java.util.List<UsoVista> uso(
            @org.springframework.data.repository.query.Param("tipo") jonathan.desarrollo.sistemaPedagogico.model.Usuario.Tipo tipo,
            @org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta);

}