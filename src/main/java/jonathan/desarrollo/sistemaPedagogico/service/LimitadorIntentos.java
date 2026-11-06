package jonathan.desarrollo.sistemaPedagogico.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimitadorIntentos {

    private static final int MAX_FALLOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(10);

    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public boolean bloqueado(String clave) {
        Deque<Instant> d = fallos.get(clave);
        if (d == null) return false;
        synchronized (d) {
            purgar(d);
            return d.size() >= MAX_FALLOS;
        }
    }

    public void registrarFallo(String clave) {
        Deque<Instant> d = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (d) {
            purgar(d);
            d.addLast(Instant.now());
        }
    }

    public void limpiar(String clave) {
        fallos.remove(clave);
    }

    private void purgar(Deque<Instant> d) {
        Instant limite = Instant.now().minus(VENTANA);
        while (!d.isEmpty() && d.peekFirst().isBefore(limite)) d.pollFirst();
    }
}