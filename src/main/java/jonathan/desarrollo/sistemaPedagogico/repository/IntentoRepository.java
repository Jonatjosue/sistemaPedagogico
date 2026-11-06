// IntentoRepository.java
package jonathan.desarrollo.sistemaPedagogico.repository;

import jonathan.desarrollo.sistemaPedagogico.model.Intento;
import jonathan.desarrollo.sistemaPedagogico.model.SesionLeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IntentoRepository extends JpaRepository<Intento, Long> {
    List<Intento> findBySesionLeccionIdOrderByFechaHoraAsc(Long sesionLeccionId);

    Optional<Intento> findFirstBySesionLeccionIdAndTipoOrderByIdDesc(Long sesionLeccionId, Intento.Tipo tipo);

    interface IntentoVista {
        Long getId();
        Long getSesionId();
        String getNombre();
        String getCodigo();
        String getAcceso();
        String getLeccion();
        java.time.LocalDateTime getFecha();
        Intento.Tipo getTipo();
        Boolean getExito();
        SesionLeccion.Estado getEstado();
        String getSalida();
    }

    @Query(value = """
            select i.id as id, s.id as sesionId, u.nombre as nombre, u.codigo as codigo, u.accesoUsuario as acceso,
                   s.leccionTitulo as leccion, i.fechaHora as fecha, i.tipo as tipo, i.exito as exito,
                   s.estado as estado, i.salida as salida
            from Intento i join i.sesionLeccion s join s.usuario u
            where (:uid = 0L or u.id = :uid)
            order by i.fechaHora desc, i.id desc
            """,
            countQuery = "select count(i) from Intento i where (:uid = 0L or i.sesionLeccion.usuario.id = :uid)")
    org.springframework.data.domain.Page<IntentoVista> historial(
            @org.springframework.data.repository.query.Param("uid") Long uid,
            org.springframework.data.domain.Pageable pageable);
}