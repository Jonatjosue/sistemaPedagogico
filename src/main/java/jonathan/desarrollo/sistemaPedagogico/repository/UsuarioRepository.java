// UsuarioRepository.java
package jonathan.desarrollo.sistemaPedagogico.repository;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    Optional<Usuario> findByAccesoUsuario(String accesoUsuario);
    List<Usuario> findByTipoOrderByNombre(Usuario.Tipo tipo);
}