package com.algorythm.security;

import com.algorythm.model.User;
import com.algorythm.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads a "Authorization: Bearer &lt;token&gt;" header, validates the JWT, and if
 * it checks out, puts an authentication (the username, plus ROLE_ADMIN when the
 * user is an admin) into the security context. The role is looked up fresh so a
 * promotion/demotion takes effect immediately, and it lets the security config
 * guard admin routes centrally with hasRole("ADMIN") instead of a manual check.
 * No token / bad token simply means no auth is set.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            String username = jwtService.validateAndGetUsername(token);
            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                boolean admin = users.findByUsername(username).map(User::isAdmin).orElse(false);
                var authorities =
                        admin
                                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                : List.<SimpleGrantedAuthority>of();
                var authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }
}
