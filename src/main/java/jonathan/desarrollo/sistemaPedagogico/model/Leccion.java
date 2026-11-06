package jonathan.desarrollo.sistemaPedagogico.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leccion")
public class Leccion {

    public enum Objetivo { COMPILAR_CON_ERROR, COMPILAR_EXITOSO, EJECUTAR_CON_ERROR, EJECUTAR_EXITOSO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 350)
    private String titulo;

    @Lob @Column(name = "contenido_leccion", nullable = false)
    private String contenidoLeccion;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Objetivo objetivo;

    @Column(name = "fecha_importacion", nullable = false, updatable = false)
    private LocalDateTime fechaImportacion;

    @PrePersist
    void antesDeGuardar() { if (fechaImportacion == null) fechaImportacion = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenidoLeccion() { return contenidoLeccion; }
    public void setContenidoLeccion(String c) { this.contenidoLeccion = c; }
    public Objetivo getObjetivo() { return objetivo; }
    public void setObjetivo(Objetivo objetivo) { this.objetivo = objetivo; }
    public LocalDateTime getFechaImportacion() { return fechaImportacion; }
}