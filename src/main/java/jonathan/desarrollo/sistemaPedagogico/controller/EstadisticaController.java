package jonathan.desarrollo.sistemaPedagogico.controller;

import jonathan.desarrollo.sistemaPedagogico.service.EstadisticaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/estadisticas")
@PreAuthorize("hasAuthority('PERM_VER_REPORTES')")
public class EstadisticaController {

    private final EstadisticaService estadisticas;

    public EstadisticaController(EstadisticaService estadisticas) {
        this.estadisticas = estadisticas;
    }

    @GetMapping
    public EstadisticaService.Resultado ver(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "dia") String agrupar) {
        return estadisticas.calcular(desde, hasta, agrupar);
    }
}