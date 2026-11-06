package jonathan.desarrollo.sistemaPedagogico.controller;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioRepository;
import jonathan.desarrollo.sistemaPedagogico.service.UsuarioActualService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferencias")
public class PreferenciaController {

    public record PreferenciasDto(int tamanoFuente, String tema) {}

    private final UsuarioActualService actual;
    private final UsuarioRepository usuarios;

    public PreferenciaController(UsuarioActualService actual, UsuarioRepository usuarios) {
        this.actual = actual;
        this.usuarios = usuarios;
    }

    @PutMapping
    public ResponseEntity<Void> guardar(@RequestBody PreferenciasDto dto, Authentication auth) {
        if (dto.tamanoFuente() < 12 || dto.tamanoFuente() > 28 || dto.tema() == null) {
            return ResponseEntity.badRequest().build();
        }
        Usuario.Tema tema;
        try {
            tema = Usuario.Tema.valueOf(dto.tema().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        Usuario u = actual.obtener(auth);
        u.setTamanoFuente(dto.tamanoFuente());
        u.setTema(tema);
        usuarios.save(u);
        return ResponseEntity.noContent().build();
    }
}