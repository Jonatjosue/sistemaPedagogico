// IngresoRepository.java
package jonathan.desarrollo.sistemaPedagogico.repository;

import jonathan.desarrollo.sistemaPedagogico.model.Ingreso;
import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IngresoRepository extends JpaRepository<Ingreso, Long> {

    interface IngresoVista {
        Long getId();
        String getNombre();
        String getCodigo();
        String getAcceso();
        Usuario.Tipo getTipo();
        java.time.LocalDateTime getFecha();
        String getIp();
        String getUserAgent();
    }

    @Query(value = """
            select ig.id as id, u.nombre as nombre, u.codigo as codigo, u.accesoUsuario as acceso,
                   u.tipo as tipo, ig.fechaIngreso as fecha, ig.ip as ip, ig.userAgent as userAgent
            from Ingreso ig join ig.usuario u
            where ig.fechaIngreso >= :desde and ig.fechaIngreso < :hasta
            order by ig.fechaIngreso desc, ig.id desc
            """,
            countQuery = "select count(ig) from Ingreso ig where ig.fechaIngreso >= :desde and ig.fechaIngreso < :hasta")
    org.springframework.data.domain.Page<IngresoVista> buscar(
            @org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta,
            org.springframework.data.domain.Pageable pageable);

    interface PeriodoVista {
        String getPeriodo();
        Long getUsuarios();
    }

    @Query(value = """
            SELECT DATE_FORMAT(ig.fecha_ingreso, :formato) AS periodo,
                   COUNT(DISTINCT ig.usuario_id) AS usuarios
            FROM ingreso ig JOIN usuario u ON u.id = ig.usuario_id
            WHERE u.tipo = 'ESTUDIANTE'
              AND ig.fecha_ingreso >= :desde AND ig.fecha_ingreso < :hasta
            GROUP BY periodo
            ORDER BY periodo
            """, nativeQuery = true)
    java.util.List<PeriodoVista> visitantesPorPeriodo(
            @org.springframework.data.repository.query.Param("formato") String formato,
            @org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta);

    @Query("""
            select count(distinct ig.usuario.id) from Ingreso ig
            where ig.usuario.tipo = :tipo and ig.fechaIngreso >= :desde and ig.fechaIngreso < :hasta
            """)
    long visitantesUnicos(
            @org.springframework.data.repository.query.Param("tipo") Usuario.Tipo tipo,
            @org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta);

}