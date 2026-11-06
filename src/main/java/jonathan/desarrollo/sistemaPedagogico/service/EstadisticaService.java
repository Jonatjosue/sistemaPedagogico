package jonathan.desarrollo.sistemaPedagogico.service;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.repository.IngresoRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.LeccionRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.SesionLeccionRepository;
import jonathan.desarrollo.sistemaPedagogico.service.AdminService.AdminException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EstadisticaService {

    public record Punto(String etiqueta, long usuarios) {}

    public record LeccionStat(String leccion, long usos, long acertados, long fallidos, Integer porcentaje) {}

    public record Resultado(long usuariosUnicos, long acertados, long fallidos, List<Punto> serie,
                            List<LeccionStat> masUsadas, List<LeccionStat> menosUsadas,
                            List<LeccionStat> masSimples, List<LeccionStat> masDificiles,
                            List<LeccionStat> porLeccion) {}

    private static final int MIN_INTENTOS = 3;
    private static final int TOP = 3;
    private static final int MAX_PUNTOS = 400;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private final IngresoRepository ingresos;
    private final SesionLeccionRepository sesiones;
    private final LeccionRepository lecciones;

    public EstadisticaService(IngresoRepository ingresos, SesionLeccionRepository sesiones,
                              LeccionRepository lecciones) {
        this.ingresos = ingresos;
        this.sesiones = sesiones;
        this.lecciones = lecciones;
    }

    @Transactional(readOnly = true)
    public Resultado calcular(LocalDate desde, LocalDate hasta, String agrupar) {
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        LocalDate d = desde == null ? h.minusDays(29) : desde;
        if (d.isAfter(h)) throw new AdminException(400, "La fecha inicial no puede ser posterior a la final.");
        LocalDateTime ini = d.atStartOfDay();
        LocalDateTime fin = h.plusDays(1).atStartOfDay();

        List<Punto> serie = serie(agrupar == null ? "dia" : agrupar, d, h, ini, fin);
        long unicos = ingresos.visitantesUnicos(Usuario.Tipo.ESTUDIANTE, ini, fin);

        Map<String, long[]> m = new TreeMap<>();
        sesiones.uso(Usuario.Tipo.ESTUDIANTE, ini, fin).forEach(u ->
                m.put(u.getLeccion(), new long[]{nz(u.getUsos()), nz(u.getAcertados()), nz(u.getFallidos())}));
        lecciones.findAllByOrderByTituloAsc().forEach(l -> m.putIfAbsent(l.getTitulo(), new long[]{0, 0, 0}));

        List<LeccionStat> todas = m.entrySet().stream().map(e -> {
            long[] v = e.getValue();
            long t = v[1] + v[2];
            Integer pct = t >= MIN_INTENTOS ? (int) Math.round(100.0 * v[1] / t) : null;
            return new LeccionStat(e.getKey(), v[0], v[1], v[2], pct);
        }).toList();

        Comparator<LeccionStat> nombre = Comparator.comparing(LeccionStat::leccion);
        List<LeccionStat> conPct = todas.stream().filter(l -> l.porcentaje() != null).toList();

        long ok = todas.stream().mapToLong(LeccionStat::acertados).sum();
        long ko = todas.stream().mapToLong(LeccionStat::fallidos).sum();

        return new Resultado(unicos, ok, ko, serie,
                todas.stream().sorted(Comparator.comparingLong(LeccionStat::usos).reversed().thenComparing(nombre)).limit(TOP).toList(),
                todas.stream().sorted(Comparator.comparingLong(LeccionStat::usos).thenComparing(nombre)).limit(TOP).toList(),
                conPct.stream().sorted(Comparator.comparingInt(LeccionStat::porcentaje).reversed().thenComparing(nombre)).limit(TOP).toList(),
                conPct.stream().sorted(Comparator.comparingInt(LeccionStat::porcentaje).thenComparing(nombre)).limit(TOP).toList(),
                todas);
    }

    private List<Punto> serie(String g, LocalDate d, LocalDate h, LocalDateTime ini, LocalDateTime fin) {
        Map<String, String> etiquetas = new LinkedHashMap<>();   // clave SQL -> etiqueta visible
        String formato;
        switch (g) {
            case "dia" -> {
                formato = "%Y-%m-%d";
                for (LocalDate x = d; !x.isAfter(h); x = x.plusDays(1)) {
                    etiquetas.put(x.toString(), x.format(DIA));
                    if (etiquetas.size() > MAX_PUNTOS) throw new AdminException(400, "Para agrupar por día elige un rango menor.");
                }
            }
            case "mes" -> {
                formato = "%Y-%m";
                for (YearMonth x = YearMonth.from(d); !x.isAfter(YearMonth.from(h)); x = x.plusMonths(1)) {
                    etiquetas.put(x.toString(), x.format(MES));
                    if (etiquetas.size() > MAX_PUNTOS) throw new AdminException(400, "El rango es demasiado grande.");
                }
            }
            case "anio" -> {
                formato = "%Y";
                for (int y = d.getYear(); y <= h.getYear(); y++) etiquetas.put(String.valueOf(y), String.valueOf(y));
                if (etiquetas.size() > MAX_PUNTOS) throw new AdminException(400, "El rango es demasiado grande.");
            }
            default -> throw new AdminException(400, "Agrupación no válida.");
        }
        Map<String, Long> cuenta = new HashMap<>();
        ingresos.visitantesPorPeriodo(formato, ini, fin).forEach(p -> cuenta.put(p.getPeriodo(), nz(p.getUsuarios())));
        return etiquetas.entrySet().stream()
                .map(e -> new Punto(e.getValue(), cuenta.getOrDefault(e.getKey(), 0L))).toList();
    }

    private static long nz(Long n) { return n == null ? 0 : n; }
}