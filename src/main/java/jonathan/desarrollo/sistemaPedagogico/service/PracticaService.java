package jonathan.desarrollo.sistemaPedagogico.service;

import jonathan.desarrollo.sistemaPedagogico.model.Intento;
import jonathan.desarrollo.sistemaPedagogico.model.Leccion;
import jonathan.desarrollo.sistemaPedagogico.model.SesionLeccion;
import jonathan.desarrollo.sistemaPedagogico.repository.IntentoRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.LeccionRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.SesionLeccionRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PracticaService {

    public static class PracticaException extends RuntimeException {
        private final int status;
        public PracticaException(int status, String mensaje) { super(mensaje); this.status = status; }
        public int getStatus() { return status; }
    }

    public record Resultado(boolean compilo, boolean exitoso, boolean timeout,
                            String consola, boolean completadaAhora) {}

    private static final int MAX_CODIGO = 20_000;
    private static final int LIMITE_SEGUNDOS = 10;   // el mismo valor que guali.timeout-segundos

    private final SesionLeccionRepository sesiones;
    private final IntentoRepository intentos;
    private final LeccionRepository lecciones;
    private final UsuarioRepository usuarios;
    private final UsuarioActualService actual;
    private final CompiladorClient compilador;
    private final TransactionTemplate tx;

    // Una operación a la vez por usuario
    private final Set<Long> enCurso = ConcurrentHashMap.newKeySet();

    public PracticaService(SesionLeccionRepository sesiones, IntentoRepository intentos,
                           LeccionRepository lecciones, UsuarioRepository usuarios,
                           UsuarioActualService actual, CompiladorClient compilador,
                           PlatformTransactionManager tm) {
        this.sesiones = sesiones;
        this.intentos = intentos;
        this.lecciones = lecciones;
        this.usuarios = usuarios;
        this.actual = actual;
        this.compilador = compilador;
        this.tx = new TransactionTemplate(tm);
    }

    /* ---------- Abrir y cerrar ---------- */

    public Long abrir(Authentication auth, Long leccionId) {
        Long uid = actual.obtener(auth).getId();
        return tx.execute(st -> {
            Leccion l = lecciones.findById(leccionId)
                    .orElseThrow(() -> new PracticaException(404, "La lección no existe."));
            LocalDateTime ahora = LocalDateTime.now();
            // Abrir otra lección abandona las sesiones que seguían abiertas
            sesiones.findByUsuarioIdAndFechaFinIsNull(uid).forEach(s -> s.setFechaFin(ahora));

            SesionLeccion s = new SesionLeccion();
            s.setUsuario(usuarios.getReferenceById(uid));
            s.setLeccion(l);
            s.setLeccionTitulo(l.getTitulo());
            return sesiones.save(s).getId();
        });
    }

    public void cerrar(Authentication auth, Long sesionId) {
        Long uid = actual.obtener(auth).getId();
        tx.executeWithoutResult(st -> {
            SesionLeccion s = propia(sesionId, uid);
            if (s.getFechaFin() == null) s.setFechaFin(LocalDateTime.now());
        });
    }

    /* ---------- Compilar ---------- */

    public Resultado compilar(Authentication auth, Long sesionId, String codigo) {
        validar(codigo);
        Long uid = actual.obtener(auth).getId();
        iniciar(uid);
        try {
            Leccion.Objetivo objetivo = tx.execute(st -> objetivoAbierto(propia(sesionId, uid)));

            CompiladorClient.Resultado r = compilador.compilar(codigo);   // fuera de la transacción

            return tx.execute(st -> {
                SesionLeccion s = propia(sesionId, uid);
                String consola = r.compilo()
                        ? "Compilación exitosa. 0 errores." + (nz(r.salida()).isBlank() ? "" : "\n" + r.salida())
                        : nz(r.salida());
                guardar(s, Intento.Tipo.COMPILAR, codigo, r.compilo(), consola);

                boolean cumple = switch (objetivo) {
                    case COMPILAR_CON_ERROR -> !r.compilo();
                    case COMPILAR_EXITOSO -> r.compilo();
                    default -> false;
                };
                return new Resultado(r.compilo(), r.compilo(), false, consola, marcar(s, cumple));
            });
        } finally {
            enCurso.remove(uid);
        }
    }

    /* ---------- Ejecutar ---------- */

    public Resultado ejecutar(Authentication auth, Long sesionId, String codigo) {
        validar(codigo);
        Long uid = actual.obtener(auth).getId();
        iniciar(uid);
        try {
            Leccion.Objetivo objetivo = tx.execute(st -> {
                SesionLeccion s = propia(sesionId, uid);
                Leccion.Objetivo o = objetivoAbierto(s);
                boolean compilado = intentos
                        .findFirstBySesionLeccionIdAndTipoOrderByIdDesc(sesionId, Intento.Tipo.COMPILAR)
                        .map(i -> i.isExito() && i.getCodigoFuente().equals(codigo))
                        .orElse(false);
                if (!compilado) {
                    throw new PracticaException(409, "Primero compila tu código. Si lo cambiaste, vuelve a compilar.");
                }
                return o;
            });

            CompiladorClient.Resultado r = compilador.ejecutar(codigo);   // fuera de la transacción

            return tx.execute(st -> {
                SesionLeccion s = propia(sesionId, uid);
                boolean exito = r.compilo() && r.exitoso() && !r.timeout();
                String consola = consolaEjecucion(r);
                guardar(s, Intento.Tipo.EJECUTAR, codigo, exito, consola);

                boolean cumple = switch (objetivo) {
                    case EJECUTAR_EXITOSO -> exito;
                    case EJECUTAR_CON_ERROR -> r.compilo() && !exito;   // el timeout cuenta como error
                    default -> false;
                };
                return new Resultado(r.compilo(), exito, r.timeout(), consola, marcar(s, cumple));
            });
        } finally {
            enCurso.remove(uid);
        }
    }

    /* ---------- Auxiliares ---------- */

    private void iniciar(Long uid) {
        if (!enCurso.add(uid)) {
            throw new PracticaException(429, "Ya hay una operación en curso. Espera a que termine.");
        }
    }

    private void validar(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new PracticaException(400, "Escribe tu código primero.");
        if (codigo.length() > MAX_CODIGO) throw new PracticaException(400, "El código es demasiado largo (máximo 20 000 caracteres).");
    }

    /** Solo devuelve sesiones del usuario; si no son suyas, responde 404 sin revelar que existen. */
    private SesionLeccion propia(Long sesionId, Long usuarioId) {
        SesionLeccion s = sesiones.findById(sesionId)
                .orElseThrow(() -> new PracticaException(404, "La sesión de lección no existe."));
        if (!s.getUsuario().getId().equals(usuarioId)) {
            throw new PracticaException(404, "La sesión de lección no existe.");
        }
        return s;
    }

    private Leccion.Objetivo objetivoAbierto(SesionLeccion s) {
        if (s.getFechaFin() != null && s.getEstado() == SesionLeccion.Estado.PENDIENTE) {
            throw new PracticaException(409, "La lección está cerrada. Ábrela de nuevo.");
        }
        if (s.getLeccion() == null) {
            throw new PracticaException(409, "La lección ya no existe. Ábrela de nuevo desde la lista.");
        }
        return s.getLeccion().getObjetivo();
    }

    private void guardar(SesionLeccion s, Intento.Tipo tipo, String codigo, boolean exito, String salida) {
        Intento i = new Intento();
        i.setSesionLeccion(s);
        i.setTipo(tipo);
        i.setCodigoFuente(codigo);
        i.setExito(exito);
        i.setSalida(salida);
        intentos.save(i);
    }

    /** Marca la sesión como completada la primera vez que se cumple el objetivo. */
    private boolean marcar(SesionLeccion s, boolean cumple) {
        if (!cumple || s.getEstado() == SesionLeccion.Estado.COMPLETADA) return false;
        s.setEstado(SesionLeccion.Estado.COMPLETADA);
        s.setFechaFin(LocalDateTime.now());
        return true;
    }

    private String consolaEjecucion(CompiladorClient.Resultado r) {
        if (!r.compilo()) return "El código ya no compila:\n" + nz(r.salida());
        if (r.timeout()) return "El programa tardó demasiado y fue detenido (límite: " + LIMITE_SEGUNDOS + " segundos).";
        StringBuilder sb = new StringBuilder(nz(r.salida()));
        if (!nz(r.errores()).isBlank()) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append('\n');
            sb.append("--- Errores de ejecución ---\n").append(r.errores());
        }
        if (sb.isEmpty()) sb.append("(El programa terminó sin imprimir nada.)");
        return sb.toString();
    }

    private static String nz(String s) { return s == null ? "" : s; }
}