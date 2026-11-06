package jonathan.desarrollo.sistemaPedagogico.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class EstudianteService {

    // Sin O/0 ni I/1 para evitar confusiones al copiar el código
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final SecurityContextRepository contextRepository;

    public EstudianteService(UsuarioRepository usuarios, SecurityContextRepository contextRepository) {
        this.usuarios = usuarios;
        this.contextRepository = contextRepository;
    }

    @Transactional
    public Usuario crear(String nombre) {
        Usuario u = new Usuario();
        u.setTipo(Usuario.Tipo.ESTUDIANTE);
        u.setNombre(nombre);
        u.setCodigo(generarCodigoUnico());
        return usuarios.save(u);
    }

    public Optional<Usuario> buscarPorCodigo(String codigo) {
        return usuarios.findByCodigo(codigo)
                .filter(u -> u.getTipo() == Usuario.Tipo.ESTUDIANTE && u.isActivo());
    }

    public void iniciarSesion(Usuario u, HttpServletRequest req, HttpServletResponse res) {
        var principal = new User(u.getCodigo(), "", List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")));
        var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        if (req.getSession(false) != null) req.changeSessionId(); // evita fijación de sesión

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        contextRepository.saveContext(ctx, req, res);
    }

    private String generarCodigoUnico() {
        String c;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
            c = sb.toString();
        } while (usuarios.existsByCodigo(c));
        return c;
    }
}