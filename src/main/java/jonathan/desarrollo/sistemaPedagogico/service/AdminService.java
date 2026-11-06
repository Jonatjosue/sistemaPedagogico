package jonathan.desarrollo.sistemaPedagogico.service;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.model.UsuarioPermiso;
import jonathan.desarrollo.sistemaPedagogico.model.UsuarioPermiso.Permiso;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioPermisoRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AdminService {

    public static class AdminException extends RuntimeException {
        private final int status;
        public AdminException(int status, String mensaje) { super(mensaje); this.status = status; }
        public int getStatus() { return status; }
    }

    public record AdminDto(Long id, String acceso, String nombre, String fecha, boolean activo,
                           List<String> permisos, boolean tu) {}

    public record GuardarDto(String acceso, String nombre, String password, List<String> permisos) {}

    private static final Pattern ACCESO = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UsuarioRepository usuarios;
    private final UsuarioPermisoRepository permisos;
    private final PasswordEncoder encoder;
    private final UsuarioActualService actual;
    private final LimitadorIntentos limitador;

    public AdminService(UsuarioRepository usuarios, UsuarioPermisoRepository permisos, PasswordEncoder encoder,
                        UsuarioActualService actual, LimitadorIntentos limitador) {
        this.usuarios = usuarios;
        this.permisos = permisos;
        this.encoder = encoder;
        this.actual = actual;
        this.limitador = limitador;
    }

    @Transactional(readOnly = true)
    public List<AdminDto> listar(Authentication auth) {
        Long yo = actual.obtener(auth).getId();
        return usuarios.findByTipoOrderByNombre(Usuario.Tipo.ADMIN).stream().map(u -> aDto(u, yo)).toList();
    }

    @Transactional
    public AdminDto crear(GuardarDto d, Authentication auth) {
        String acceso = d.acceso() == null ? "" : d.acceso().trim();
        if (!ACCESO.matcher(acceso).matches()) {
            throw new AdminException(400, "El usuario debe tener de 3 a 50 caracteres: letras, números, punto, guion o guion bajo.");
        }
        String nombre = nombreValido(d.nombre());
        validarPassword(d.password());
        if (usuarios.findByAccesoUsuario(acceso).isPresent()) {
            throw new AdminException(409, "Ese usuario ya existe.");
        }
        Usuario u = new Usuario();
        u.setTipo(Usuario.Tipo.ADMIN);
        u.setNombre(nombre);
        u.setAccesoUsuario(acceso);
        u.setPasswordHash(encoder.encode(d.password()));
        u = usuarios.save(u);
        aplicarPermisos(u, parsear(d.permisos()));
        return aDto(u, actual.obtener(auth).getId());
    }

    @Transactional
    public AdminDto editar(Long id, GuardarDto d, Authentication auth) {
        Usuario u = admin(id);
        u.setNombre(nombreValido(d.nombre()));
        Set<Permiso> nuevos = parsear(d.permisos());
        proteger(u, u.isActivo(), nuevos.contains(Permiso.GESTIONAR_USUARIOS));
        if (d.password() != null && !d.password().isEmpty()) {
            validarPassword(d.password());
            u.setPasswordHash(encoder.encode(d.password()));
        }
        aplicarPermisos(u, nuevos);
        usuarios.save(u);
        return aDto(u, actual.obtener(auth).getId());
    }

    @Transactional
    public void desactivar(Long id, Authentication auth) {
        if (id.equals(actual.obtener(auth).getId())) {
            throw new AdminException(409, "No puedes eliminarte a ti mismo.");
        }
        Usuario u = admin(id);
        if (!u.isActivo()) return;
        proteger(u, false, false);
        u.setActivo(false);
        usuarios.save(u);
    }

    @Transactional
    public void reactivar(Long id) {
        Usuario u = admin(id);
        u.setActivo(true);
        usuarios.save(u);
    }

    @Transactional
    public void cambiarMiPassword(Authentication auth, String actualPw, String nueva) {
        Usuario u = actual.obtener(auth);
        String clave = "pw:" + u.getId();
        if (limitador.bloqueado(clave)) {
            throw new AdminException(429, "Demasiados intentos fallidos. Espera unos minutos.");
        }
        if (actualPw == null || !encoder.matches(actualPw, u.getPasswordHash())) {
            limitador.registrarFallo(clave);
            throw new AdminException(400, "La contraseña actual es incorrecta.");
        }
        limitador.limpiar(clave);
        validarPassword(nueva);
        if (nueva.equals(actualPw)) {
            throw new AdminException(400, "La nueva contraseña debe ser distinta de la actual.");
        }
        u.setPasswordHash(encoder.encode(nueva));
        usuarios.save(u);
    }

    /* ---------- Auxiliares ---------- */

    private Usuario admin(Long id) {
        return usuarios.findById(id).filter(u -> u.getTipo() == Usuario.Tipo.ADMIN)
                .orElseThrow(() -> new AdminException(404, "El administrador no existe."));
    }

    /** Siempre debe quedar al menos un admin activo con GESTIONAR_USUARIOS. */
    private void proteger(Usuario u, boolean quedaActivo, boolean quedaConGestion) {
        boolean tenia = u.isActivo() && permisos.findByIdUsuarioIdAndActivoTrue(u.getId()).stream()
                .anyMatch(p -> p.getPermiso() == Permiso.GESTIONAR_USUARIOS);
        if (tenia && !(quedaActivo && quedaConGestion)
                && permisos.contarAdminsActivosConPermiso(Permiso.GESTIONAR_USUARIOS, Usuario.Tipo.ADMIN) <= 1) {
            throw new AdminException(409,
                    "Debe quedar al menos un administrador activo con permiso para gestionar administradores.");
        }
    }

    private void aplicarPermisos(Usuario u, Set<Permiso> elegidos) {
        Map<Permiso, UsuarioPermiso> actuales = permisos.findByIdUsuarioId(u.getId()).stream()
                .collect(Collectors.toMap(UsuarioPermiso::getPermiso, p -> p));
        for (Permiso p : Permiso.values()) {
            UsuarioPermiso e = actuales.get(p);
            boolean quiere = elegidos.contains(p);
            if (e != null) e.setActivo(quiere);
            else if (quiere) permisos.save(new UsuarioPermiso(u, p));
        }
    }

    private Set<Permiso> parsear(List<String> lista) {
        Set<Permiso> r = EnumSet.noneOf(Permiso.class);
        if (lista != null) {
            for (String s : lista) {
                try { r.add(Permiso.valueOf(s)); }
                catch (IllegalArgumentException e) { throw new AdminException(400, "Permiso no válido."); }
            }
        }
        return r;
    }

    private String nombreValido(String n) {
        String s = n == null ? "" : n.trim();
        if (s.length() < 2 || s.length() > 120) {
            throw new AdminException(400, "El nombre debe tener entre 2 y 120 caracteres.");
        }
        return s;
    }

    private void validarPassword(String p) {
        if (p == null || p.length() < 8 || p.length() > 64) {
            throw new AdminException(400, "La contraseña debe tener entre 8 y 64 caracteres.");
        }
    }

    private AdminDto aDto(Usuario u, Long yo) {
        List<String> perms = permisos.findByIdUsuarioIdAndActivoTrue(u.getId()).stream()
                .map(p -> p.getPermiso().name()).sorted().toList();
        return new AdminDto(u.getId(), u.getAccesoUsuario(), u.getNombre(),
                FMT.format(u.getFechaCreacion()), u.isActivo(), perms, u.getId().equals(yo));
    }
}