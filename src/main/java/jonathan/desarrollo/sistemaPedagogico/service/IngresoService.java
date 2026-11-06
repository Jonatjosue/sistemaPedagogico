package jonathan.desarrollo.sistemaPedagogico.service;

import jakarta.servlet.http.HttpServletRequest;
import jonathan.desarrollo.sistemaPedagogico.model.Ingreso;
import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.repository.IngresoRepository;
import org.springframework.stereotype.Service;

@Service
public class IngresoService {

    private final IngresoRepository ingresos;

    public IngresoService(IngresoRepository ingresos) {
        this.ingresos = ingresos;
    }

    public void registrar(Usuario usuario, HttpServletRequest req) {
        Ingreso i = new Ingreso();
        i.setUsuario(usuario);
        i.setIp(IpCliente.de(req));
        String ua = req.getHeader("User-Agent");
        if (ua != null && ua.length() > 255) ua = ua.substring(0, 255);
        i.setUserAgent(ua);
        ingresos.save(i);
    }
}