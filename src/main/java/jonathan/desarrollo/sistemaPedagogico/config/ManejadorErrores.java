package jonathan.desarrollo.sistemaPedagogico.config;

import jonathan.desarrollo.sistemaPedagogico.service.CompiladorClient;
import jonathan.desarrollo.sistemaPedagogico.service.PracticaService.PracticaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> archivoGrande() {
        return ResponseEntity.status(413).body(Map.of("mensaje", "El archivo supera el tamaño máximo (20 MB)."));
    }

    @ExceptionHandler(PracticaException.class)
    public ResponseEntity<Map<String, String>> practica(PracticaException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(CompiladorClient.NoDisponibleException.class)
    public ResponseEntity<Map<String, String>> compiladorCaido(CompiladorClient.NoDisponibleException e) {
        return ResponseEntity.status(503).body(Map.of("mensaje", e.getMessage()));
    }
    @ExceptionHandler(jonathan.desarrollo.sistemaPedagogico.service.AdminService.AdminException.class)
    public ResponseEntity<Map<String, String>> admin(
            jonathan.desarrollo.sistemaPedagogico.service.AdminService.AdminException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("mensaje", e.getMessage()));
    }
}