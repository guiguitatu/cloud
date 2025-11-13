package br.pucpr.ms.loadbalancer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoadBalancerService {

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate = new RestTemplate();

    // Cache de instâncias válidas por serviço
    private final Map<String, List<ServiceInstanceInfo>> serviceInstances = new ConcurrentHashMap<>();

    // Contadores para Round Robin por serviço
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    // Cache de portas descobertas manualmente (fallback quando Consul não está disponível)
    private final Map<String, Set<Integer>> discoveredPorts = new ConcurrentHashMap<>();

    public ServiceInstanceInfo getNextInstance(String serviceName) {
        // Atualizar lista de instâncias
        refreshServiceInstances(serviceName);

        List<ServiceInstanceInfo> instances = serviceInstances.get(serviceName);
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        // Implementar Round Robin
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % instances.size();

        return instances.get(index);
    }

    private void refreshServiceInstances(String serviceName) {
        List<ServiceInstanceInfo> validInstances = new ArrayList<>();

        // 1. Tentar obter instâncias do Consul (DiscoveryClient)
        if (discoveryClient != null) {
            try {
                List<ServiceInstance> consulInstances = discoveryClient.getInstances(serviceName);
                for (ServiceInstance instance : consulInstances) {
                    if (isInstanceHealthy(serviceName, instance.getPort())) {
                        validInstances.add(new ServiceInstanceInfo(
                            instance.getHost(),
                            instance.getPort(),
                            instance.getInstanceId()
                        ));
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed to get instances from Consul for " + serviceName + ": " + e.getMessage());
            }
        }

        // 2. Se Consul não está disponível ou não retornou instâncias, usar descoberta manual
        if (validInstances.isEmpty()) {
            Set<Integer> ports = discoveredPorts.get(serviceName);
            if (ports != null && !ports.isEmpty()) {
                for (Integer port : ports) {
                    if (isInstanceHealthy(serviceName, port)) {
                        validInstances.add(new ServiceInstanceInfo("localhost", port, "manual-" + port));
                    }
                }
            } else {
                // Descobrir instâncias manualmente
                Set<Integer> foundPorts = discoverServicePorts(serviceName);
                discoveredPorts.put(serviceName, foundPorts);
                for (Integer port : foundPorts) {
                    if (isInstanceHealthy(serviceName, port)) {
                        validInstances.add(new ServiceInstanceInfo("localhost", port, "manual-" + port));
                    }
                }
            }
        }

        // Atualizar cache de instâncias
        if (!validInstances.isEmpty()) {
            serviceInstances.put(serviceName, validInstances);
            System.out.println("Updated instances for " + serviceName + ": " + validInstances.size() + " instance(s)");
        } else {
            // Manter instâncias antigas se não encontrou novas (pode ser que o serviço esteja temporariamente indisponível)
            if (!serviceInstances.containsKey(serviceName)) {
                serviceInstances.put(serviceName, new ArrayList<>());
            }
        }
    }

    private boolean isInstanceHealthy(String serviceName, int port) {
        try {
            String healthUrl = serviceName.equals("ms-kotlin") ?
                "http://localhost:" + port + "/actuator/health" :
                "http://localhost:" + port + "/health";

            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                String body = response.getBody();
                return body != null && body.contains("UP");
            }
        } catch (Exception e) {
            // Instância não está saudável
        }
        return false;
    }

    private Set<Integer> discoverServicePorts(String serviceName) {
        Set<Integer> foundPorts = new HashSet<>();
        
        // Lista de portas para testar (portas dinâmicas comuns do Windows)
        int[] portsToTest = {
            8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090,
            9000, 9090,
            49152, 49153, 49154, 49155, 49156, 49157, 49158, 49159, 49160,
            49664, 49665, 49666, 49667, 49668, 49669, 49670, 49671, 49672,
            50000, 50100, 50200, 50300, 50400, 50500, 50600, 50700, 50800, 50900, 51000,
            55000, 55100, 55200, 55300, 55400, 55500, 55600, 55700, 55800, 55900,
            56000, 56100, 56200, 56300, 56400, 56500, 56600, 56700, 56800, 56900,
            57000, 57100, 57200, 57300, 57400, 57500, 57600, 57700, 57800, 57900,
            60000, 60100, 60200, 60300, 60400, 60500, 60600, 60700, 60800, 60900,
            61000, 61100, 61200, 61300, 61400, 61500, 61600, 61700, 61800, 61900
        };

        for (int port : portsToTest) {
            if (isInstanceHealthy(serviceName, port)) {
                foundPorts.add(port);
                System.out.println("Discovered " + serviceName + " instance on port " + port);
            }
        }

        return foundPorts;
    }

    public List<ServiceInstanceInfo> getAllInstances(String serviceName) {
        refreshServiceInstances(serviceName);
        return serviceInstances.getOrDefault(serviceName, new ArrayList<>());
    }

    public static class ServiceInstanceInfo {
        private final String host;
        private final int port;
        private final String instanceId;

        public ServiceInstanceInfo(String host, int port, String instanceId) {
            this.host = host;
            this.port = port;
            this.instanceId = instanceId;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String getBaseUrl() {
            return "http://" + host + ":" + port;
        }

        @Override
        public String toString() {
            return "ServiceInstanceInfo{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    ", instanceId='" + instanceId + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServiceInstanceInfo that = (ServiceInstanceInfo) o;
            return port == that.port && Objects.equals(host, that.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port);
        }
    }
}

