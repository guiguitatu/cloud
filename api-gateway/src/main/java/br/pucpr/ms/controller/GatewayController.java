package br.pucpr.ms.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@RestController
public class GatewayController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/")
    public String index() {
        return "Microsserviço de gateway!";
    }

    @RequestMapping("/ms-kotlin/**")
    public ResponseEntity<String> routeToKotlin(HttpServletRequest request,
                                               @RequestBody(required = false) String body) {
        // Usar portas dinâmicas descobertas - tenta portas comuns
        return routeToServiceWithFallback("ms-kotlin", request, body,
                                        new int[]{8081, 8082, 50000, 50100, 50200, 50300});
    }

    @RequestMapping("/ms-python/**")
    public ResponseEntity<String> routeToPython(HttpServletRequest request,
                                               @RequestBody(required = false) String body) {
        // Usar portas dinâmicas descobertas - tenta portas comuns
        return routeToServiceWithFallback("ms-python", request, body,
                                        new int[]{8082, 8081, 50001, 50101, 50201, 50301});
    }

    private ResponseEntity<String> routeToServiceWithFallback(String serviceName, HttpServletRequest request,
                                                            String body, int[] possiblePorts) {
        // Tentar portas conhecidas primeiro
        for (int port : possiblePorts) {
            try {
                String baseUrl = "http://localhost:" + port;
                String path = request.getRequestURI().replace("/" + serviceName, "");
                String queryString = request.getQueryString();
                String url = baseUrl + path + (queryString != null ? "?" + queryString : "");

                HttpHeaders headers = new HttpHeaders();
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (!headerName.equalsIgnoreCase("host") && !headerName.equalsIgnoreCase("content-length")) {
                        headers.add(headerName, request.getHeader(headerName));
                    }
                }

                HttpEntity<String> entity = new HttpEntity<>(body, headers);
                HttpMethod method = HttpMethod.valueOf(request.getMethod());

                ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
                return response;

            } catch (Exception e) {
                // Porta não funciona, tentar próxima
                continue;
            }
        }

        // Se nenhuma porta funcionou, tentar descoberta dinâmica
        try {
            int dynamicPort = findServicePort(serviceName);
            if (dynamicPort != -1) {
                String baseUrl = "http://localhost:" + dynamicPort;

                // Construir o caminho relativo removendo o prefixo do serviço
                String path = request.getRequestURI().replace("/" + serviceName, "");
                String queryString = request.getQueryString();
                String url = baseUrl + path + (queryString != null ? "?" + queryString : "");

                // Copiar headers da requisição original
                HttpHeaders headers = new HttpHeaders();
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (!headerName.equalsIgnoreCase("host") && !headerName.equalsIgnoreCase("content-length")) {
                        headers.add(headerName, request.getHeader(headerName));
                    }
                }

                HttpEntity<String> entity = new HttpEntity<>(body, headers);
                HttpMethod method = HttpMethod.valueOf(request.getMethod());

                ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
                return response;
            }
        } catch (Exception e) {
            // Descoberta dinâmica falhou
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Service " + serviceName + " not available on any known port");
    }

    private int findServicePort(String serviceName) {
        // Lista de portas mais prováveis baseada em execuções anteriores
        int[] commonPorts = {8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090,
                           50000, 50001, 50002, 50003, 50004, 50005, 50006, 50007, 50008, 50009,
                           50100, 50200, 50300, 50400, 50500, 50600, 50700, 50800, 50900, 51000};

        for (int port : commonPorts) {
            try {
                String healthUrl = serviceName.equals("ms-kotlin") ?
                    "http://localhost:" + port + "/actuator/health" :
                    "http://localhost:" + port + "/health";

                ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);
                if (healthResponse.getStatusCode() == HttpStatus.OK) {
                    String responseBody = healthResponse.getBody();
                    if (responseBody != null && responseBody.contains("UP")) {
                        System.out.println("Found " + serviceName + " on port " + port);
                        return port;
                    }
                }
            } catch (Exception e) {
                // Porta não responde, continuar procurando
            }
        }
        return -1; // Serviço não encontrado
    }
}
