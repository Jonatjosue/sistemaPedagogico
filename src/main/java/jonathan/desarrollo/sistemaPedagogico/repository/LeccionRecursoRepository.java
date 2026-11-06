package jonathan.desarrollo.sistemaPedagogico.repository;

import jonathan.desarrollo.sistemaPedagogico.model.LeccionRecurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeccionRecursoRepository extends JpaRepository<LeccionRecurso, Long> {

    Optional<LeccionRecurso> findByLeccionIdAndNombreArchivo(Long leccionId, String nombreArchivo);

    List<LeccionRecurso> findByLeccionId(Long leccionId);

    @Modifying(flushAutomatically = true)
    @Query("delete from LeccionRecurso r where r.leccion.id = :leccionId")
    void borrarPorLeccion(@Param("leccionId") Long leccionId);
}