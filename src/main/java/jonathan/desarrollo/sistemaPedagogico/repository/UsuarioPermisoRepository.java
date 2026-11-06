package jonathan.desarrollo.sistemaPedagogico.repository;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.model.UsuarioPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioPermisoRepository extends JpaRepository<UsuarioPermiso, UsuarioPermiso.Clave> {

    List<UsuarioPermiso> findByIdUsuarioIdAndActivoTrue(Long usuarioId);

    List<UsuarioPermiso> findByIdUsuarioId(Long usuarioId);

    @Query("""
            select count(p) from UsuarioPermiso p
            where p.id.permiso = :permiso and p.activo = true
              and p.usuario.activo = true and p.usuario.tipo = :tipo
            """)
    long contarAdminsActivosConPermiso(@Param("permiso") UsuarioPermiso.Permiso permiso,
                                       @Param("tipo") Usuario.Tipo tipo);
}