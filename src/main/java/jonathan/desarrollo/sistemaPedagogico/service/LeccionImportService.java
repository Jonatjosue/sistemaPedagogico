package jonathan.desarrollo.sistemaPedagogico.service;

import jonathan.desarrollo.sistemaPedagogico.model.Leccion;
import jonathan.desarrollo.sistemaPedagogico.model.LeccionRecurso;
import jonathan.desarrollo.sistemaPedagogico.repository.LeccionRecursoRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.LeccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class LeccionImportService {

    private static final Logger log = LoggerFactory.getLogger(LeccionImportService.class);

    public static class ZipInvalidoException extends RuntimeException {
        public ZipInvalidoException(String mensaje) { super(mensaje); }
    }

    public record Item(String titulo, String estado, String detalle) {}
    public record Resultado(List<Item> lecciones, List<String> avisos) {}

    private static final int MAX_ENTRADAS = 500;
    private static final long MAX_TOTAL = 50L * 1024 * 1024;
    private static final int MAX_MD = 1024 * 1024;
    private static final int MAX_CONFIG = 1024;
    private static final int MAX_IMAGEN = 5 * 1024 * 1024;

    private static final Pattern TITULO = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N}._ -]{0,349}$");
    private static final Map<String, String> MIME = Map.of(
            "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg",
            "gif", "image/gif", "webp", "image/webp");

    private static class Borrador {
        String md;
        String config;
        final Map<String, byte[]> imagenes = new LinkedHashMap<>();
    }

    private final LeccionRepository lecciones;
    private final LeccionRecursoRepository recursos;
    private final TransactionTemplate tx;

    public LeccionImportService(LeccionRepository lecciones, LeccionRecursoRepository recursos,
                                PlatformTransactionManager tm) {
        this.lecciones = lecciones;
        this.recursos = recursos;
        this.tx = new TransactionTemplate(tm);
    }

    public Resultado importar(InputStream in) throws IOException {
        List<String> avisos = new ArrayList<>();
        Map<String, Borrador> borradores = leerZip(in, avisos);
        if (borradores.isEmpty()) {
            throw new ZipInvalidoException("El archivo no es un zip válido o no contiene carpetas de lección.");
        }
        List<Item> items = new ArrayList<>();
        borradores.forEach((titulo, b) -> items.add(procesar(titulo, b)));
        return new Resultado(items, avisos);
    }

    /* ---------- Lectura del zip (todo en memoria, con límites) ---------- */

    private Map<String, Borrador> leerZip(InputStream in, List<String> avisos) throws IOException {
        Map<String, Borrador> res = new LinkedHashMap<>();
        long total = 0;
        int entradas = 0;

        try (ZipInputStream zis = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (++entradas > MAX_ENTRADAS) {
                    throw new ZipInvalidoException("El zip tiene demasiados archivos (máximo " + MAX_ENTRADAS + ").");
                }
                if (e.isDirectory()) continue;

                String ruta = e.getName().replace('\\', '/');   // algunos programas de Windows usan "\"
                List<String> seg = new ArrayList<>(Arrays.asList(ruta.split("/")));
                if (ruta.startsWith("/") || seg.contains("..")) {
                    throw new ZipInvalidoException("El zip contiene rutas no permitidas.");
                }
                seg.removeIf(s -> s.isEmpty() || s.equals("."));
                if (seg.isEmpty()) continue;

                String nombre = seg.get(seg.size() - 1);
                String minus = nombre.toLowerCase(Locale.ROOT);
                int limite = minus.equals("leccion.md") ? MAX_MD
                        : minus.equals("config.txt") ? MAX_CONFIG : MAX_IMAGEN;

                // Se lee SIEMPRE con límite, incluso lo que se va a ignorar
                byte[] datos = leerLimitado(zis, limite, nombre);
                total += datos.length;
                if (total > MAX_TOTAL) {
                    throw new ZipInvalidoException("El contenido descomprimido supera el máximo permitido (50 MB).");
                }

                boolean basura = seg.stream().anyMatch(s -> s.equals("__MACOSX") || s.startsWith("."));
                if (basura) continue;

                if (seg.size() >= 3 && seg.get(0).equalsIgnoreCase("lecciones")) seg.remove(0);
                if (seg.size() == 1) {
                    avisos.add("Archivo fuera de una carpeta de lección (ignorado): " + ruta);
                    continue;
                }
                if (seg.size() > 2) {
                    avisos.add("Subcarpetas no soportadas (ignorado): " + ruta);
                    continue;
                }

                Borrador b = res.computeIfAbsent(seg.get(0), k -> new Borrador());
                if (minus.equals("leccion.md")) {
                    b.md = texto(datos);
                } else if (minus.equals("config.txt")) {
                    b.config = texto(datos);
                } else if (MIME.containsKey(extension(minus))) {
                    if (nombre.length() > 250) avisos.add("Nombre de imagen demasiado largo (ignorado): " + nombre);
                    else b.imagenes.put(nombre, datos);
                } else {
                    avisos.add("Archivo no soportado (ignorado): " + ruta);
                }
            }
        }
        return res;
    }

    private byte[] leerLimitado(InputStream in, int limite, String nombre) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long leidos = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            leidos += n;
            if (leidos > limite) {
                throw new ZipInvalidoException("El archivo '" + nombre + "' supera el tamaño máximo permitido.");
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static String texto(byte[] datos) {
        String s = new String(datos, StandardCharsets.UTF_8);
        return s.startsWith("\uFEFF") ? s.substring(1) : s;
    }

    private static String extension(String nombreMinusculas) {
        int i = nombreMinusculas.lastIndexOf('.');
        return i < 0 ? "" : nombreMinusculas.substring(i + 1);
    }

    /* ---------- Validación y guardado de cada lección ---------- */

    private Item procesar(String titulo, Borrador b) {
        if (!TITULO.matcher(titulo).matches()) {
            return rechazo(titulo, "El nombre de la carpeta no es válido (letras, números, punto, guion, guion bajo o espacio; máximo 350).");
        }
        if (b.md == null || b.md.isBlank()) return rechazo(titulo, "Falta leccion.md o está vacío.");
        if (b.config == null) return rechazo(titulo, "Falta config.txt.");

        Leccion.Objetivo objetivo = parseObjetivo(b.config);
        if (objetivo == null) {
            return rechazo(titulo, "config.txt debe tener exactamente una línea: objetivo=COMPILAR_CON_ERROR, COMPILAR_EXITOSO, EJECUTAR_CON_ERROR o EJECUTAR_EXITOSO.");
        }

        try {
            boolean actualizada = Boolean.TRUE.equals(tx.execute(st -> guardar(titulo, b, objetivo)));
            return new Item(titulo, actualizada ? "ACTUALIZADA" : "CREADA", b.imagenes.size() + " imagen(es).");
        } catch (RuntimeException ex) {
            log.error("No se pudo guardar la lección '{}'", titulo, ex);
            return rechazo(titulo, "No se pudo guardar (revisa que no haya imágenes con nombres repetidos).");
        }
    }

    private static Leccion.Objetivo parseObjetivo(String config) {
        List<String> lineas = Arrays.stream(config.split("\\R"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (lineas.size() != 1 || !lineas.get(0).startsWith("objetivo=")) return null;
        try {
            return Leccion.Objetivo.valueOf(lineas.get(0).substring("objetivo=".length()).trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean guardar(String titulo, Borrador b, Leccion.Objetivo objetivo) {
        Optional<Leccion> existente = lecciones.findByTitulo(titulo);
        Leccion l = existente.orElseGet(Leccion::new);
        l.setTitulo(titulo);
        l.setContenidoLeccion(b.md);
        l.setObjetivo(objetivo);
        l = lecciones.save(l);

        if (existente.isPresent()) recursos.borrarPorLeccion(l.getId());

        for (Map.Entry<String, byte[]> img : b.imagenes.entrySet()) {
            LeccionRecurso r = new LeccionRecurso();
            r.setLeccion(l);
            r.setNombreArchivo(img.getKey());
            r.setTipoArchivo(MIME.get(extension(img.getKey().toLowerCase(Locale.ROOT))));
            r.setContenido(img.getValue());
            recursos.save(r);
        }
        return existente.isPresent();
    }

    private static Item rechazo(String titulo, String detalle) {
        return new Item(titulo, "RECHAZADA", detalle);
    }
}