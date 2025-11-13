package br.pucpr.ms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;

@RestController
public class GatewayController {

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate = new RestTemplate();

    // Cache das portas descobertas para performance
    private Integer kotlinPort = null;
    private Integer pythonPort = null;

    @GetMapping("/")
    public String index() {
        return "Microsserviço de gateway!";
    }

    @RequestMapping("/ms-kotlin/**")
    public ResponseEntity<String> routeToKotlin(HttpServletRequest request,
                                               @RequestBody(required = false) String body) {
        // Usar porta conhecida do ms-kotlin (vamos descobrir dinamicamente na primeira chamada)
        return routeToService("ms-kotlin", request, body);
    }

    @RequestMapping("/ms-kotlin")
    public ResponseEntity<String> routeToKotlinRoot(HttpServletRequest request,
                                                   @RequestBody(required = false) String body) {
        // Para requisições exatamente para /ms-kotlin (sem barra no final)
        return routeToService("ms-kotlin", request, body);
    }

    @RequestMapping("/ms-python/**")
    public ResponseEntity<String> routeToPython(HttpServletRequest request,
                                               @RequestBody(required = false) String body) {
        // Usar porta conhecida do ms-python (56859 do Consul)
        return routeToService("ms-python", request, body);
    }

    @RequestMapping("/ms-python")
    public ResponseEntity<String> routeToPythonRoot(HttpServletRequest request,
                                                   @RequestBody(required = false) String body) {
        // Para requisições exatamente para /ms-python (sem barra no final)
        return routeToService("ms-python", request, body);
    }

    private ResponseEntity<String> routeToService(String serviceName, HttpServletRequest request, String body) {
        try {
            // Usar cache se disponível, senão descobrir
            Integer port = getServicePort(serviceName);

            if (port == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Service " + serviceName + " not available");
            }

            String baseUrl = "http://localhost:" + port;

            // Construir o caminho relativo removendo o prefixo do serviço
            String originalUri = request.getRequestURI();
            String path = originalUri.replace("/" + serviceName, "");
            // Garantir que o path comece com /
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

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

        } catch (Exception e) {
            // Se falhou, limpar cache para forçar rediscoberta na próxima requisição
            if (serviceName.equals("ms-kotlin")) {
                kotlinPort = null;
            } else {
                pythonPort = null;
            }

            System.out.println("Gateway routing failed for " + serviceName + ", cleared cache: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error routing to " + serviceName + ": " + e.getMessage());
        }
    }

    private Integer getServicePort(String serviceName) {
        Integer cachedPort = serviceName.equals("ms-kotlin") ? kotlinPort : pythonPort;

        if (cachedPort != null) {
            return cachedPort; // Retornar do cache
        }

        // Primeiro tentar usar Consul (DiscoveryClient)
        if (discoveryClient != null) {
            try {
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
                if (!instances.isEmpty()) {
                    ServiceInstance instance = instances.get(0);
                    int consulPort = instance.getPort();

                    // Verificar se a porta do Consul realmente funciona
                    try {
                        String healthUrl = serviceName.equals("ms-kotlin") ?
                            "http://localhost:" + consulPort + "/actuator/health" :
                            "http://localhost:" + consulPort + "/health";

                        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);
                        if (healthResponse.getStatusCode() == HttpStatus.OK) {
                            String responseBody = healthResponse.getBody();
                            if (responseBody != null && responseBody.contains("UP")) {
                                // Porta do Consul funciona, usar ela
                                if (serviceName.equals("ms-kotlin")) {
                                    kotlinPort = consulPort;
                                } else {
                                    pythonPort = consulPort;
                                }
                                System.out.println("Found " + serviceName + " via Consul on port " + consulPort);
                                return consulPort;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Consul port " + consulPort + " for " + serviceName + " not responding, will try auto-discovery");
                    }
                }
            } catch (Exception e) {
                System.out.println("Consul discovery failed for " + serviceName + ": " + e.getMessage());
            }
        }

        // Fallback: usar descoberta automática de portas
        int discoveredPort = findServicePort(serviceName);
        if (discoveredPort != -1) {
            // Salvar no cache
            if (serviceName.equals("ms-kotlin")) {
                kotlinPort = discoveredPort;
            } else {
                pythonPort = discoveredPort;
            }
            return discoveredPort;
        }

        return null; // Não encontrado
    }

    private int findServicePort(String serviceName) {
        // Lista abrangente de portas prováveis (porta dinâmica do Windows começa em 49152)
        int[] allPorts = {
            // Portas conhecidas dos serviços (prioridade máxima)
            8082, // ms-kotlin conhecido
            61087, // ms-python conhecido

            // Portas tradicionais (prioridade alta)
            8081, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090,
            9000, 9090,

            // Portas dinâmicas do Windows (range completo)
            49152, 49153, 49154, 49155, 49156, 49157, 49158, 49159, 49160,
            49664, 49665, 49666, 49667, 49668, 49669, 49670, 49671, 49672,
            50000, 50001, 50002, 50003, 50004, 50005, 50006, 50007, 50008, 50009,
            50100, 50200, 50300, 50400, 50500, 50600, 50700, 50800, 50900, 51000,
            51100, 51200, 51300, 51400, 51500, 51600, 51700, 51800, 51900, 52000,
            55000, 55100, 55200, 55300, 55400, 55500, 55600, 55700, 55800, 55900,
            56000, 56100, 56200, 56300, 56400, 56500, 56600, 56700, 56800, 56900,
            57000, 57100, 57200, 57300, 57400, 57500, 57600, 57700, 57800, 57900,
            58000, 58100, 58200, 58300, 58400, 58500, 58600, 58700, 58800, 58900,
            59000, 59100, 59200, 59300, 59400, 59500, 59600, 59700, 59800, 59900,
            60000, 60100, 60200, 60300, 60400, 60500, 60600, 60700, 60800, 60900,
            61000, 61100, 61200, 61300, 61400, 61500, 61600, 61700, 61800, 61900,
            62000, 62100, 62200, 62300, 62400, 62500, 62600, 62700, 62800, 62900,
            63000, 63100, 63200, 63300, 63400, 63500, 63600, 63700, 63800, 63900,
            64000, 64100, 64200, 64300, 64400, 64500, 64600, 64700, 64800, 64900,
            65000, 65100, 65200, 65300, 65400, 65500, 65501, 65502, 65503, 65504
        };

        for (int port : allPorts) {
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
