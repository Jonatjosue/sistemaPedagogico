package jonathan.desarrollo.sistemaPedagogico.controller;

import jonathan.desarrollo.sistemaPedagogico.repository.LeccionRecursoRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class RecursoController {

    private final LeccionRecursoRepository recursos;

    public RecursoController(LeccionRecursoRepository recursos) {
        this.recursos = recursos;
    }

    @GetMapping("/lecciones/{id}/recursos/{nombre}")
    public ResponseEntity<byte[]> recurso(@PathVariable Long id, @PathVariable String nombre) {
        return recursos.findByLeccionIdAndNombreArchivo(id, nombre)
                .map(r -> {
                    MediaType tipo;
                    try {
                        tipo = MediaType.parseMediaType(r.getTipoArchivo());
                    } catch (Exception e) {
                        tipo = MediaType.APPLICATION_OCTET_STREAM;
                    }
                    return ResponseEntity.ok()
                            .contentType(tipo)
                            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                            // por si el archivo fuera un SVG con scripts
                            .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
                            .body(r.getContenido());
                })
                .orElse(ResponseEntity.notFound().build());
    }
}