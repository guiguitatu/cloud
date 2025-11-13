package br.pucpr.ms.controller;

import br.pucpr.ms.loadbalancer.LoadBalancerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;

@RestController
public class GatewayController {

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Autowired
    private LoadBalancerService loadBalancerService;

    private final RestTemplate restTemplate = new RestTemplate();

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
        // Tentar múltiplas instâncias em caso de falha (retry com load balancing)
        int maxRetries = 3;
        Exception lastException = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // Obter próxima instância usando Round Robin
                LoadBalancerService.ServiceInstanceInfo instance = loadBalancerService.getNextInstance(serviceName);

                if (instance == null) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("{\"error\":\"Service " + serviceName + " not available\",\"message\":\"No healthy instances found\"}");
                }

                String baseUrl = instance.getBaseUrl();

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
                
                // Adicionar header indicando qual instância foi usada (útil para debug)
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.addAll(response.getHeaders());
                responseHeaders.add("X-Load-Balanced-Instance", instance.getInstanceId());
                responseHeaders.add("X-Load-Balanced-Port", String.valueOf(instance.getPort()));
                
                return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());

            } catch (Exception e) {
                lastException = e;
                System.out.println("Gateway routing attempt " + (attempt + 1) + " failed for " + serviceName + ": " + e.getMessage());
                
                // Forçar atualização da lista de instâncias antes de tentar novamente
                loadBalancerService.getAllInstances(serviceName);
            }
        }

        // Todas as tentativas falharam
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\":\"Error routing to " + serviceName + "\",\"message\":\"" + 
                      (lastException != null ? lastException.getMessage() : "All instances unavailable") + "\"}");
    }

    @GetMapping("/loadbalancer/instances/{serviceName}")
    public ResponseEntity<?> getServiceInstances(@PathVariable String serviceName) {
        var instances = loadBalancerService.getAllInstances(serviceName);
        return ResponseEntity.ok(Map.of(
            "service", serviceName,
            "instances", instances,
            "count", instances.size()
        ));
    }
}
