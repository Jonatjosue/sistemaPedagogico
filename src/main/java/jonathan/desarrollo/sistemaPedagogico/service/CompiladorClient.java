package jonathan.desarrollo.sistemaPedagogico.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class CompiladorClient {

    public record Resultado(boolean compilo, boolean exitoso, boolean timeout, String salida, String errores) {}

    public static class NoDisponibleException extends RuntimeException {
        public NoDisponibleException(Throwable causa) {
            super("El servicio de compilación no está disponible o está ocupado. Intenta de nuevo en unos segundos.", causa);
        }
    }

    private final RestClient http;

    public CompiladorClient(@Value("${compilador.url}") String url,
                            @Value("${compilador.token:}") String token) {
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(45));

        RestClient.Builder b = RestClient.builder().baseUrl(url).requestFactory(fabrica);
        if (!token.isBlank()) b.defaultHeader("X-Token", token);
        this.http = b.build();
    }

    public Resultado compilar(String codigo) { return llamar("/api/compilar", codigo); }

    public Resultado ejecutar(String codigo) { return llamar("/api/ejecutar", codigo); }

    private Resultado llamar(String ruta, String codigo) {
        try {
            Resultado r = http.post().uri(ruta)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("codigo", codigo))
                    .retrieve()
                    .body(Resultado.class);
            if (r == null) throw new NoDisponibleException(null);
            return r;
        } catch (RestClientException e) {
            org.slf4j.LoggerFactory.getLogger(CompiladorClient.class)
                    .warn("Fallo al llamar al compilador ({}): {}", ruta, e.toString());
            throw new NoDisponibleException(e);
        }
    }
}