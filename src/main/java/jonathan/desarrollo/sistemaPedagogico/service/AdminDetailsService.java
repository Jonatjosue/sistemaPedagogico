package jonathan.desarrollo.sistemaPedagogico.service;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioPermisoRepository;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;
    private final UsuarioPermisoRepository permisos;

    public AdminDetailsService(UsuarioRepository usuarios, UsuarioPermisoRepository permisos) {
        this.usuarios = usuarios;
        this.permisos = permisos;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Usuario u = usuarios.findByAccesoUsuario(username)
                .filter(x -> x.getTipo() == Usuario.Tipo.ADMIN)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        permisos.findByIdUsuarioIdAndActivoTrue(u.getId()).forEach(p ->
                autoridades.add(new SimpleGrantedAuthority("PERM_" + p.getPermiso().name())));

        return User.withUsername(u.getAccesoUsuario())
                .password(u.getPasswordHash())
                .authorities(autoridades)
                .disabled(!u.isActivo())
                .build();
    }
}