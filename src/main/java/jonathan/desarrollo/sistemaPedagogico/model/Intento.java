package jonathan.desarrollo.sistemaPedagogico.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intento")
public class Intento {

    public enum Tipo { COMPILAR, EJECUTAR }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_leccion_id")
    private SesionLeccion sesionLeccion;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Tipo tipo;

    @Lob @Column(name = "codigo_fuente", nullable = false)
    private String codigoFuente;

    @Column(nullable = false)
    private boolean exito;

    @Lob
    private String salida;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    void antesDeGuardar() { if (fechaHora == null) fechaHora = LocalDateTime.now(); }

    public Long getId() { return id; }
    public SesionLeccion getSesionLeccion() { return sesionLeccion; }
    public void setSesionLeccion(SesionLeccion s) { this.sesionLeccion = s; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getCodigoFuente() { return codigoFuente; }
    public void setCodigoFuente(String c) { this.codigoFuente = c; }
    public boolean isExito() { return exito; }
    public void setExito(boolean exito) { this.exito = exito; }
    public String getSalida() { return salida; }
    public void setSalida(String salida) { this.salida = salida; }
    public LocalDateTime getFechaHora() { return fechaHora; }
}