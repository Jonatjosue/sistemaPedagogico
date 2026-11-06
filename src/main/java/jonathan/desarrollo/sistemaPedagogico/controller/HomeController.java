package jonathan.desarrollo.sistemaPedagogico.controller;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.service.UsuarioActualService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UsuarioActualService actual;

    public HomeController(UsuarioActualService actual) {
        this.actual = actual;
    }

    @GetMapping("/")
    public String inicio(Authentication auth, Model model) {
        Usuario u = actual.obtener(auth);
        model.addAttribute("nombre", u.getNombre());
        model.addAttribute("esAdmin", u.getTipo() == Usuario.Tipo.ADMIN);
        model.addAttribute("gestionaLecciones", tiene(auth, "PERM_GESTIONAR_LECCIONES"));
        model.addAttribute("puedeUsuarios", tiene(auth, "PERM_GESTIONAR_USUARIOS"));
        model.addAttribute("puedeReportes", tiene(auth, "PERM_VER_REPORTES"));
        model.addAttribute("tamano", u.getTamanoFuente());
        model.addAttribute("tema", u.getTema().name().toLowerCase());
        return "index";
    }

    private boolean tiene(Authentication auth, String permiso) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(permiso));
    }
}