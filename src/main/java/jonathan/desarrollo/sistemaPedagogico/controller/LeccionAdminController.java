package jonathan.desarrollo.sistemaPedagogico.controller;

import jonathan.desarrollo.sistemaPedagogico.repository.LeccionRepository;
import jonathan.desarrollo.sistemaPedagogico.service.LeccionExportService;
import jonathan.desarrollo.sistemaPedagogico.service.LeccionImportService;
import jonathan.desarrollo.sistemaPedagogico.service.LeccionImportService.ZipInvalidoException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@RestController
@RequestMapping("/admin/lecciones")
@PreAuthorize("hasAuthority('PERM_GESTIONAR_LECCIONES')")
public class LeccionAdminController {

    public record Mensaje(String mensaje) {}

    private final LeccionImportService importService;
    private final LeccionExportService exportService;
    private final LeccionRepository lecciones;

    public LeccionAdminController(LeccionImportService importService, LeccionExportService exportService,
                                  LeccionRepository lecciones) {
        this.importService = importService;
        this.exportService = exportService;
        this.lecciones = lecciones;
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importar(@RequestParam("archivo") MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (archivo.isEmpty() || nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return ResponseEntity.badRequest().body(new Mensaje("Selecciona un archivo .zip."));
        }
        try (InputStream in = archivo.getInputStream()) {
            return ResponseEntity.ok(importService.importar(in));
        } catch (ZipInvalidoException e) {
            return ResponseEntity.badRequest().body(new Mensaje(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new Mensaje("El archivo está dañado o no es un zip válido."));
        }
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar() throws IOException {
        byte[] zip = exportService.exportar();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("lecciones.zip").build().toString())
                .body(zip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!lecciones.existsById(id)) return ResponseEntity.notFound().build();
        lecciones.deleteById(id);   // las imágenes se borran por CASCADE y el historial queda con el título
        return ResponseEntity.noContent().build();
    }
}