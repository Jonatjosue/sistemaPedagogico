package jonathan.desarrollo.sistemaPedagogico.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingreso")
public class Ingreso {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_ingreso", nullable = false, updatable = false)
    private LocalDateTime fechaIngreso;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @PrePersist
    void antesDeGuardar() { if (fechaIngreso == null) fechaIngreso = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFechaIngreso() { return fechaIngreso; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String u) { this.userAgent = u; }
}