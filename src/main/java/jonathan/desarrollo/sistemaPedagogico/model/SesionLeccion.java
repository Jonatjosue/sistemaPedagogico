package jonathan.desarrollo.sistemaPedagogico.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesion_leccion")
public class SesionLeccion {

    public enum Estado { COMPLETADA, PENDIENTE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leccion_id")
    private Leccion leccion;

    @Column(name = "leccion_titulo", nullable = false, length = 350)
    private String leccionTitulo;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Estado estado = Estado.PENDIENTE;

    @PrePersist
    void antesDeGuardar() { if (fechaInicio == null) fechaInicio = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Leccion getLeccion() { return leccion; }
    public void setLeccion(Leccion leccion) { this.leccion = leccion; }
    public String getLeccionTitulo() { return leccionTitulo; }
    public void setLeccionTitulo(String t) { this.leccionTitulo = t; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime f) { this.fechaFin = f; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}