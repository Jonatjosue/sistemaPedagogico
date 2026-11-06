package jonathan.desarrollo.sistemaPedagogico.repository;

import jonathan.desarrollo.sistemaPedagogico.model.Leccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LeccionRepository extends JpaRepository<Leccion, Long> {

    interface Resumen {
        Long getId();
        String getTitulo();
        Leccion.Objetivo getObjetivo();
    }

    Optional<Leccion> findByTitulo(String titulo);
    boolean existsByTitulo(String titulo);
    List<Resumen> findAllByOrderByTituloAsc();
}