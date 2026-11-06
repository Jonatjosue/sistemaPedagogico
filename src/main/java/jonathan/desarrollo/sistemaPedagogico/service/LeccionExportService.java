package jonathan.desarrollo.sistemaPedagogico.service;

import jonathan.desarrollo.sistemaPedagogico.model.Leccion;
import jonathan.desarrollo.sistemaPedagogico.model.LeccionRecurso;
import jonathan.desarrollo.sistemaPedagogico.repository.LeccionRecursoRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.LeccionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class LeccionExportService {

    private final LeccionRepository lecciones;
    private final LeccionRecursoRepository recursos;

    public LeccionExportService(LeccionRepository lecciones, LeccionRecursoRepository recursos) {
        this.lecciones = lecciones;
        this.recursos = recursos;
    }

    public byte[] exportar() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            for (Leccion l : lecciones.findAll(Sort.by("titulo"))) {
                String carpeta = "lecciones/" + l.getTitulo().replaceAll("[/\\\\]", "-") + "/";
                escribir(zip, carpeta + "leccion.md", l.getContenidoLeccion().getBytes(StandardCharsets.UTF_8));
                escribir(zip, carpeta + "config.txt",
                        ("objetivo=" + l.getObjetivo().name() + "\n").getBytes(StandardCharsets.UTF_8));
                for (LeccionRecurso r : recursos.findByLeccionId(l.getId())) {
                    escribir(zip, carpeta + r.getNombreArchivo(), r.getContenido());
                }
            }
        }
        return bos.toByteArray();
    }

    private void escribir(ZipOutputStream zip, String ruta, byte[] datos) throws IOException {
        zip.putNextEntry(new ZipEntry(ruta));
        zip.write(datos);
        zip.closeEntry();
    }
}