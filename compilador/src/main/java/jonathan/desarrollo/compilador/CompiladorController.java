package jonathan.desarrollo.compilador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CompiladorController {

    private static final Logger log = LoggerFactory.getLogger(CompiladorController.class);

    public record Peticion(String codigo) {}

    private final CompiladorService servicio;
    private final String token;

    public CompiladorController(CompiladorService servicio, @Value("${guali.token:}") String token) {
        this.servicio = servicio;
        this.token = token;
    }

    @PostMapping("/compilar")
    public ResponseEntity<?> compilar(@RequestBody Peticion p,
                                      @RequestHeader(value = "X-Token", required = false) String t) {
        return manejar(p, t, false);
    }

    @PostMapping("/ejecutar")
    public ResponseEntity<?> ejecutar(@RequestBody Peticion p,
                                      @RequestHeader(value = "X-Token", required = false) String t) {
        return manejar(p, t, true);
    }

    private ResponseEntity<?> manejar(Peticion p, String recibido, boolean ejecutar) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(401).body(Map.of("mensaje", "No autorizado."));
        }
        if (p == null || p.codigo() == null || p.codigo().isBlank() || p.codigo().length() > 20_000) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Código inválido."));
        }
        try {
            return servicio.procesar(p.codigo(), ejecutar)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(503).body(Map.of("mensaje", "Servicio ocupado.")));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(503).body(Map.of("mensaje", "Operación interrumpida."));
        } catch (RuntimeException e) {
            log.error("Error al compilar o ejecutar", e);
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error interno del compilador."));
        }
    }

    private boolean autorizado(String recibido) {
        if (token.isBlank()) return true;   // solo para desarrollo local
        if (recibido == null) return false;
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }
}