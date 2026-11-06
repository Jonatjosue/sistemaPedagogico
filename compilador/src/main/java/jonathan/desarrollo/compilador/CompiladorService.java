package jonathan.desarrollo.compilador;

import edu.umg.guali.compiler.CompileResult;
import edu.umg.guali.compiler.ExecuteResult;
import edu.umg.guali.compiler.GualiDev;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CompiladorService {

    public record Resultado(boolean compilo, boolean exitoso, boolean timeout, String salida, String errores) {}

    private static final int MAX_SALIDA = 10_000;

    // Una operación a la vez: la librería usa siempre el nombre de clase "app25"
    private final ReentrantLock candado = new ReentrantLock(true);

    private final String jdkHome;
    private final int timeoutSegundos;

    public CompiladorService(@Value("${guali.jdk-home:}") String jdkHome,
                             @Value("${guali.timeout-segundos:10}") int timeoutSegundos) {
        this.jdkHome = jdkHome.isBlank() ? System.getProperty("java.home") : jdkHome;
        this.timeoutSegundos = timeoutSegundos;
    }

    /** Devuelve vacío si el servicio está ocupado. */
    public Optional<Resultado> procesar(String codigo, boolean ejecutar) throws InterruptedException {
        if (!candado.tryLock(20, TimeUnit.SECONDS)) return Optional.empty();
        try {
            CompileResult cr = GualiDev.compile(codigo, jdkHome);
            if (!cr.isExitoso()) {
                return Optional.of(new Resultado(false, false, false, recortar(cr.getSalida()), ""));
            }
            if (!ejecutar) {
                return Optional.of(new Resultado(true, true, false, recortar(cr.getSalida()), ""));
            }
            ExecuteResult er = GualiDev.execute(cr.getNombreClase(), jdkHome, timeoutSegundos);
            return Optional.of(new Resultado(true, er.isExitoso(), er.isTimeoutAlcanzado(),
                    recortar(er.getSalida()), recortar(er.getErrores())));
        } finally {
            candado.unlock();
        }
    }

    private static String recortar(String s) {
        if (s == null) return "";
        return s.length() <= MAX_SALIDA ? s : s.substring(0, MAX_SALIDA) + "\n[salida recortada]";
    }
}