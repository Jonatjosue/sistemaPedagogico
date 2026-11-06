package jonathan.desarrollo.sistemaPedagogico.config;

import jonathan.desarrollo.sistemaPedagogico.model.Usuario;
import jonathan.desarrollo.sistemaPedagogico.repository.UsuarioRepository;
import jonathan.desarrollo.sistemaPedagogico.service.AdminDetailsService;
import jonathan.desarrollo.sistemaPedagogico.service.IngresoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    IngresoService ingresoService,
                                    UsuarioRepository usuarios,
                                    AdminDetailsService adminDetails) throws Exception {
        http
                .addFilterAfter(new AdminVigenteFilter(adminDetails), SecurityContextHolderFilter.class)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/ingreso", "/ingreso/nuevo", "/ingreso/codigo", "/admin/login").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().hasAnyRole("ESTUDIANTE", "ADMIN"))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/ingreso")))
                .formLogin(f -> f
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .successHandler((req, res, auth) -> {
                            usuarios.findByAccesoUsuario(auth.getName())
                                    .filter(u -> u.getTipo() == Usuario.Tipo.ADMIN)
                                    .ifPresent(u -> ingresoService.registrar(u, req));
                            res.sendRedirect(req.getContextPath() + "/");
                        })
                        .failureUrl("/admin/login?error"))
                .logout(l -> l
                        .logoutUrl("/salir")
                        .logoutSuccessUrl("/ingreso")
                        .deleteCookies("JSESSIONID"));
        return http.build();
    }
}