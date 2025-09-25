package br.pucpr.ms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayController {

    @GetMapping("/")
    public String index() {
        return "Microsserviço de gateway!";
    }
}
