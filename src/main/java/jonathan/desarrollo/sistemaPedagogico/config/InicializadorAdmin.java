package jonathan.desarrollo.sistemaPedagogico.config;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.model.UsuarioPermiso;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioPermisoRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InicializadorAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdmin.class);

    private final UsuarioRepository usuarios;
    private final UsuarioPermisoRepository permisos;
    private final PasswordEncoder encoder;

    @Value("${admin.usuario:}")
    private String usuario;

    @Value("${admin.password:}")
    private String password;

    public InicializadorAdmin(UsuarioRepository usuarios, UsuarioPermisoRepository permisos,
                              PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.permisos = permisos;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!usuarios.findByTipoOrderByNombre(Usuario.Tipo.ADMIN).isEmpty()) return;

        if (usuario.isBlank() || password.length() < 8) {
            log.warn("No hay administradores y ADMIN_USUARIO / ADMIN_PASSWORD (mín. 8) no están definidos.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setTipo(Usuario.Tipo.ADMIN);
        admin.setNombre("Administrador principal");
        admin.setAccesoUsuario(usuario.trim());
        admin.setPasswordHash(encoder.encode(password));
        admin = usuarios.save(admin);

        for (UsuarioPermiso.Permiso p : UsuarioPermiso.Permiso.values()) {
            permisos.save(new UsuarioPermiso(admin, p));
        }
        log.info("Administrador inicial '{}' creado con todos los permisos.", admin.getAccesoUsuario());
    }
}