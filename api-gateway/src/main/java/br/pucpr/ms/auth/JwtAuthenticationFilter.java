package br.pucpr.ms.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    // Endpoints públicos que não requerem autenticação
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
        "/auth/login",
        "/auth/validate",
        "/actuator/health",
        "/actuator/info",
        "/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        
        // Verificar se é um endpoint público
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extrair token do header Authorization
        String token = extractTokenFromRequest(request);

        if (token == null) {
            sendUnauthorizedResponse(response, "Token não fornecido. Use o header: Authorization: Bearer <token>");
            return;
        }

        // Validar token
        if (!jwtService.validateToken(token)) {
            sendUnauthorizedResponse(response, "Token inválido ou expirado");
            return;
        }

        // Adicionar informações do usuário ao request para uso posterior
        try {
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            request.setAttribute("username", username);
            request.setAttribute("role", role);
        } catch (Exception e) {
            sendUnauthorizedResponse(response, "Erro ao processar token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}

