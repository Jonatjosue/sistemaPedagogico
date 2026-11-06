package jonathan.desarrollo.sistemaPedagogico.service;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class UsuarioActualService {

    private final UsuarioRepository usuarios;

    public UsuarioActualService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario obtener(Authentication auth) {
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return (admin ? usuarios.findByAccesoUsuario(auth.getName())
                : usuarios.findByCodigo(auth.getName()))
                .orElseThrow(() -> new IllegalStateException("Usuario de la sesión no existe"));
    }
}