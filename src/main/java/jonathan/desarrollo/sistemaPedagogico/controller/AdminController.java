package jonathan.desarrollo.sistemaPedagogico.controller;

import jonathan.desarrollo.sistemaPedagogico.service.AdminService;
import jonathan.desarrollo.sistemaPedagogico.service.AdminService.AdminDto;
import jonathan.desarrollo.sistemaPedagogico.service.AdminService.GuardarDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record PasswordDto(String actual, String nueva) {}

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('PERM_GESTIONAR_USUARIOS')")
    public List<AdminDto> listar(Authentication auth) {
        return admin.listar(auth);
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasAuthority('PERM_GESTIONAR_USUARIOS')")
    public AdminDto crear(@RequestBody GuardarDto dto, Authentication auth) {
        return admin.crear(dto, auth);
    }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('PERM_GESTIONAR_USUARIOS')")
    public AdminDto editar(@PathVariable Long id, @RequestBody GuardarDto dto, Authentication auth) {
        return admin.editar(id, dto, auth);
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('PERM_GESTIONAR_USUARIOS')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        admin.desactivar(id, auth);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/usuarios/{id}/reactivar")
    @PreAuthorize("hasAuthority('PERM_GESTIONAR_USUARIOS')")
    public ResponseEntity<Void> reactivar(@PathVariable Long id) {
        admin.reactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mi-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cambiarPassword(@RequestBody PasswordDto dto, Authentication auth) {
        admin.cambiarMiPassword(auth, dto.actual(), dto.nueva());
        return ResponseEntity.noContent().build();
    }
}