package jonathan.desarrollo.sistemaPedagogico.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jonathan.desarrollo.sistemaPedagogico.service.AdminDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Para administradores: recarga permisos y estado en cada petición, y cierra la sesión si quedaron inactivos. */
public class AdminVigenteFilter extends OncePerRequestFilter {

    private final AdminDetailsService detalles;

    public AdminVigenteFilter(AdminDetailsService detalles) {
        this.detalles = detalles;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        return p.startsWith("/css/") || p.startsWith("/js/") || p.startsWith("/img/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = a != null && a.isAuthenticated()
                && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));

        if (admin) {
            try {
                UserDetails ud = detalles.loadUserByUsername(a.getName());
                if (!ud.isEnabled()) throw new UsernameNotFoundException("inactivo");
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(ud, null, ud.getAuthorities()));
                SecurityContextHolder.setContext(ctx);
            } catch (UsernameNotFoundException e) {
                SecurityContextHolder.clearContext();
                HttpSession s = req.getSession(false);
                if (s != null) s.invalidate();
                String p = req.getRequestURI();
                if (p.startsWith("/api/") || p.startsWith("/admin/")) {
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    res.sendRedirect(req.getContextPath() + "/ingreso");
                }
                return;
            }
        }
        chain.doFilter(req, res);
    }
}