package jonathan.desarrollo.sistemaPedagogico.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "usuario_permiso")
public class UsuarioPermiso {

    public enum Permiso { GESTIONAR_LECCIONES, GESTIONAR_USUARIOS, VER_REPORTES }

    @Embeddable
    public static class Clave implements Serializable {
        @Column(name = "usuario_id")
        private Long usuarioId;

        @Enumerated(EnumType.STRING) @Column(name = "permiso")
        private Permiso permiso;

        public Clave() {}
        public Clave(Long usuarioId, Permiso permiso) { this.usuarioId = usuarioId; this.permiso = permiso; }
        public Long getUsuarioId() { return usuarioId; }
        public Permiso getPermiso() { return permiso; }

        @Override public boolean equals(Object o) {
            return o instanceof Clave c && Objects.equals(usuarioId, c.usuarioId) && permiso == c.permiso;
        }
        @Override public int hashCode() { return Objects.hash(usuarioId, permiso); }
    }

    @EmbeddedId
    private Clave id;

    @MapsId("usuarioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private boolean activo = true;

    public UsuarioPermiso() {}
    public UsuarioPermiso(Usuario usuario, Permiso permiso) {
        this.usuario = usuario;
        this.id = new Clave(usuario.getId(), permiso);
    }

    public Clave getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Permiso getPermiso() { return id.getPermiso(); }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}