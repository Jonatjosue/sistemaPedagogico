package jonathan.desarrollo.sistemaPedagogico.service;

import jonathan.desarrollo.sistemaPedagogico.repository.IngresoRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.IntentoRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.SesionLeccionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HistorialService {

    public record Pagina<T>(List<T> items, int pagina, int totalPaginas, long total) {}

    public record RegistroDto(String usuario, String identificador, String leccion, String inicio,
                              String fin, String estado, long acertados, long fallidos) {}

    public record IntentoDto(String usuario, String identificador, long sesionId, String leccion,
                             String fecha, String tipo, boolean exito, String estado, String salida) {}

    public record IngresoDto(String usuario, String identificador, String tipo, String fecha,
                             String ip, String userAgent) {}

    private static final int TAMANO = 15;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final SesionLeccionRepository sesiones;
    private final IntentoRepository intentos;
    private final IngresoRepository ingresos;
    private final UsuarioActualService actual;

    public HistorialService(SesionLeccionRepository sesiones, IntentoRepository intentos,
                            IngresoRepository ingresos, UsuarioActualService actual) {
        this.sesiones = sesiones;
        this.intentos = intentos;
        this.ingresos = ingresos;
        this.actual = actual;
    }

    @Transactional(readOnly = true)
    public Pagina<RegistroDto> registros(Authentication auth, int pagina) {
        Page<SesionLeccionRepository.RegistroVista> p = sesiones.registros(alcance(auth), pageable(pagina));
        List<RegistroDto> items = p.getContent().stream().map(v -> new RegistroDto(
                v.getNombre(), ident(v.getCodigo(), v.getAcceso()), v.getLeccion(),
                f(v.getInicio()), f(v.getFin()), v.getEstado().name(),
                nz(v.getAcertados()), nz(v.getFallidos()))).toList();
        return new Pagina<>(items, p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Pagina<IntentoDto> intentos(Authentication auth, int pagina) {
        Page<IntentoRepository.IntentoVista> p = intentos.historial(alcance(auth), pageable(pagina));
        List<IntentoDto> items = p.getContent().stream().map(v -> new IntentoDto(
                v.getNombre(), ident(v.getCodigo(), v.getAcceso()), v.getSesionId(), v.getLeccion(),
                f(v.getFecha()), v.getTipo().name(), Boolean.TRUE.equals(v.getExito()),
                v.getEstado().name(), v.getSalida())).toList();
        return new Pagina<>(items, p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    /** Solo para administradores: el controlador lo protege con @PreAuthorize. */
    @Transactional(readOnly = true)
    public Pagina<IngresoDto> ingresos(int pagina, LocalDate desde, LocalDate hasta) {
        LocalDateTime d = (desde == null ? LocalDate.of(2000, 1, 1) : desde).atStartOfDay();
        LocalDateTime h = (hasta == null ? LocalDate.now().plusDays(1) : hasta.plusDays(1)).atStartOfDay();
        Page<IngresoRepository.IngresoVista> p = ingresos.buscar(d, h, pageable(pagina));
        List<IngresoDto> items = p.getContent().stream().map(v -> new IngresoDto(
                v.getNombre(), ident(v.getCodigo(), v.getAcceso()), v.getTipo().name(),
                f(v.getFecha()), v.getIp(), v.getUserAgent())).toList();
        return new Pagina<>(items, p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    /** 0 = ver todo (admin); si no, solo lo del propio usuario. */
    private long alcance(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin ? 0L : actual.obtener(auth).getId();
    }

    private static Pageable pageable(int pagina) {
        return PageRequest.of(Math.max(0, pagina), TAMANO);
    }

    private static String f(LocalDateTime t) { return t == null ? null : FMT.format(t); }
    private static String ident(String codigo, String acceso) { return codigo != null ? codigo : acceso; }
    private static long nz(Long n) { return n == null ? 0 : n; }
}