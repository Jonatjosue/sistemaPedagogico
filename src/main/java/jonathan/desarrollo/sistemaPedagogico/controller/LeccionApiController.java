package jonathan.desarrollo.sistemaPedagogico.controller;

import jonathan.desarrollo.sistemaPedagogico.repository.LeccionRepository;
import jonathan.desarrollo.sistemaPedagogico.service.MarkdownService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lecciones")
public class LeccionApiController {

    public record Resumen(Long id, String titulo, String objetivo) {}
    public record Detalle(Long id, String titulo, String objetivo, String html) {}

    private final LeccionRepository lecciones;
    private final MarkdownService markdown;

    public LeccionApiController(LeccionRepository lecciones, MarkdownService markdown) {
        this.lecciones = lecciones;
        this.markdown = markdown;
    }

    @GetMapping
    public List<Resumen> listar() {
        return lecciones.findAllByOrderByTituloAsc().stream()
                .map(r -> new Resumen(r.getId(), r.getTitulo(), r.getObjetivo().name()))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Detalle> ver(@PathVariable Long id) {
        return lecciones.findById(id)
                .map(l -> ResponseEntity.ok(new Detalle(l.getId(), l.getTitulo(), l.getObjetivo().name(),
                        markdown.aHtml(l.getContenidoLeccion(), l.getId()))))
                .orElse(ResponseEntity.notFound().build());
    }
}