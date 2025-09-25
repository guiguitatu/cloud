package br.pucpr.msc

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MensagemController {
    @GetMapping("/api/mensagem")
    fun mensagem(@RequestParam(defaultValue = "desenvolvedor") nome: String): String {
        return "Olá, $nome! (ms-c-kotlin)"
    }
}
