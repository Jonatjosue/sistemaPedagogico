package jonathan.desarrollo.sistemaPedagogico.controller;

import jonathan.desarrollo.sistemaPedagogico.service.PracticaService;
import jonathan.desarrollo.sistemaPedagogico.service.PracticaService.PracticaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practica")
public class PracticaController {

    public record AbrirDto(Long leccionId) {}
    public record CodigoDto(String codigo) {}
    public record SesionDto(Long sesionId) {}

    private final PracticaService practica;

    public PracticaController(PracticaService practica) {
        this.practica = practica;
    }

    @PostMapping("/abrir")
    public SesionDto abrir(@RequestBody AbrirDto dto, Authentication auth) {
        if (dto == null || dto.leccionId() == null) throw new PracticaException(400, "Falta la lección.");
        return new SesionDto(practica.abrir(auth, dto.leccionId()));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<Void> cerrar(@PathVariable Long id, Authentication auth) {
        practica.cerrar(auth, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/compilar")
    public PracticaService.Resultado compilar(@PathVariable Long id, @RequestBody CodigoDto dto, Authentication auth) {
        return practica.compilar(auth, id, dto == null ? null : dto.codigo());
    }

    @PostMapping("/{id}/ejecutar")
    public PracticaService.Resultado ejecutar(@PathVariable Long id, @RequestBody CodigoDto dto, Authentication auth) {
        return practica.ejecutar(auth, id, dto == null ? null : dto.codigo());
    }
}