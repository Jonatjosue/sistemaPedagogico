package jonathan.desarrollo.sistemaPedagogico.model;

import jakarta.persistence.*;

@Entity
@Table(name = "leccion_recurso")
public class LeccionRecurso {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leccion_id")
    private Leccion leccion;

    @Column(name = "nombre_archivo", nullable = false, length = 250)
    private String nombreArchivo;

    @Column(name = "tipo_archivo", nullable = false, length = 60)
    private String tipoArchivo;

    @Lob @Column(nullable = false)
    private byte[] contenido;

    public Long getId() { return id; }
    public Leccion getLeccion() { return leccion; }
    public void setLeccion(Leccion leccion) { this.leccion = leccion; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String n) { this.nombreArchivo = n; }
    public String getTipoArchivo() { return tipoArchivo; }
    public void setTipoArchivo(String t) { this.tipoArchivo = t; }
    public byte[] getContenido() { return contenido; }
    public void setContenido(byte[] contenido) { this.contenido = contenido; }
}