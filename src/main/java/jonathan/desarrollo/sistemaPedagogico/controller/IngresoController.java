package jonathan.desarrollo.sistemaPedagogico.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class IngresoController {

    private final EstudianteService estudiantes;
    private final IngresoService ingresos;
    private final LimitadorIntentos limitador;
    private final UsuarioActualService actual;

    public IngresoController(EstudianteService estudiantes, IngresoService ingresos,
                             LimitadorIntentos limitador, UsuarioActualService actual) {
        this.estudiantes = estudiantes;
        this.ingresos = ingresos;
        this.limitador = limitador;
        this.actual = actual;
    }

    @GetMapping("/ingreso")
    public String pagina(@RequestParam(defaultValue = "nuevo") String modo, Model model) {
        model.addAttribute("modo", modo.equals("codigo") ? "codigo" : "nuevo");
        return "ingreso";
    }

    @PostMapping("/ingreso/nuevo")
    public String nuevo(@RequestParam String nombre, HttpServletRequest req,
                        HttpServletResponse res, Model model) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.length() < 2 || n.length() > 250) {
            model.addAttribute("modo", "nuevo");
            model.addAttribute("error", "Escribe tu nombre (entre 2 y 250 caracteres).");
            return "ingreso";
        }
        Usuario u = estudiantes.crear(n);
        estudiantes.iniciarSesion(u, req, res);
        ingresos.registrar(u, req);
        return "redirect:/ingreso/creado";
    }

    @PostMapping("/ingreso/codigo")
    public String codigo(@RequestParam String codigo, HttpServletRequest req,
                         HttpServletResponse res, Model model) {
        String ip = IpCliente.de(req);
        model.addAttribute("modo", "codigo");

        if (limitador.bloqueado(ip)) {
            model.addAttribute("error", "Demasiados intentos fallidos. Espera unos minutos.");
            return "ingreso";
        }
        String c = codigo == null ? "" : codigo.trim().toUpperCase();
        Optional<Usuario> u = c.length() == 8 ? estudiantes.buscarPorCodigo(c) : Optional.empty();
        if (u.isEmpty()) {
            limitador.registrarFallo(ip);
            model.addAttribute("error", "No encontramos ese código. Revísalo o crea un ingreso nuevo.");
            return "ingreso";
        }
        limitador.limpiar(ip);
        estudiantes.iniciarSesion(u.get(), req, res);
        ingresos.registrar(u.get(), req);
        return "redirect:/";
    }

    @GetMapping("/ingreso/creado")
    public String creado(Authentication auth, Model model) {
        Usuario u = actual.obtener(auth);
        if (u.getTipo() != Usuario.Tipo.ESTUDIANTE) return "redirect:/";
        model.addAttribute("nombre", u.getNombre());
        model.addAttribute("codigo", u.getCodigo());
        return "ingreso-creado";
    }

    @GetMapping("/admin/login")
    public String loginAdmin() {
        return "admin-login";
    }
}