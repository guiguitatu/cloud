package br.pucpr.ms.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Autenticação simples (em produção, validar contra banco de dados)
        if (isValidCredentials(request.getUsername(), request.getPassword())) {
            String role = getUserRole(request.getUsername());
            String token = jwtService.generateToken(request.getUsername(), role);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("tokenType", "Bearer");
            response.put("username", request.getUsername());
            response.put("role", role);
            response.put("expiresIn", 3600); // 1 hora em segundos

            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Credenciais inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody TokenValidationRequest request) {
        try {
            if (jwtService.validateToken(request.getToken())) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("username", jwtService.extractUsername(request.getToken()));
                response.put("role", jwtService.extractRole(request.getToken()));
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("error", "Token inválido ou expirado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", "Erro ao validar token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // Autenticação simples - em produção, validar contra banco de dados
    private boolean isValidCredentials(String username, String password) {
        // Usuários de exemplo para demonstração
        Map<String, String> users = Map.of(
            "admin", "admin123",
            "user", "user123",
            "manager", "manager123"
        );
        return users.containsKey(username) && users.get(username).equals(password);
    }

    private String getUserRole(String username) {
        // Mapeamento de roles simples - em produção, buscar do banco de dados
        if ("admin".equals(username)) {
            return "ADMIN";
        } else if ("manager".equals(username)) {
            return "MANAGER";
        } else {
            return "USER";
        }
    }

    // Classes internas para requests
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class TokenValidationRequest {
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}

