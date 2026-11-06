package jonathan.desarrollo.sistemaPedagogico.controller;

import jonathan.desarrollo.sistemaPedagogico.service.HistorialService;
import jonathan.desarrollo.sistemaPedagogico.service.HistorialService.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/historial")
public class HistorialController {

    private final HistorialService historial;

    public HistorialController(HistorialService historial) {
        this.historial = historial;
    }

    @GetMapping("/registros")
    public Pagina<RegistroDto> registros(@RequestParam(defaultValue = "0") int pagina, Authentication auth) {
        return historial.registros(auth, pagina);
    }

    @GetMapping("/intentos")
    public Pagina<IntentoDto> intentos(@RequestParam(defaultValue = "0") int pagina, Authentication auth) {
        return historial.intentos(auth, pagina);
    }

    @GetMapping("/ingresos")
    @PreAuthorize("hasRole('ADMIN')")
    public Pagina<IngresoDto> ingresos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return historial.ingresos(pagina, desde, hasta);
    }
}