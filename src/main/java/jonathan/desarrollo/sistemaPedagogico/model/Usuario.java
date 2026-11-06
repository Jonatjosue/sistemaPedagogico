package jonathan.desarrollo.sistemaPedagogico.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    public enum Tipo { ADMIN, ESTUDIANTE }
    public enum Tema { CLARO, OSCURO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Tipo tipo;

    @Column(nullable = false, length = 250)
    private String nombre;

    @Column(length = 8, unique = true)
    private String codigo;

    @Column(name = "acceso_usuario", length = 50, unique = true)
    private String accesoUsuario;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "tamano_fuente", nullable = false)
    private int tamanoFuente = 16;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Tema tema = Tema.CLARO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void antesDeGuardar() { if (fechaCreacion == null) fechaCreacion = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getAccesoUsuario() { return accesoUsuario; }
    public void setAccesoUsuario(String a) { this.accesoUsuario = a; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String p) { this.passwordHash = p; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public int getTamanoFuente() { return tamanoFuente; }
    public void setTamanoFuente(int t) { this.tamanoFuente = t; }
    public Tema getTema() { return tema; }
    public void setTema(Tema tema) { this.tema = tema; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}