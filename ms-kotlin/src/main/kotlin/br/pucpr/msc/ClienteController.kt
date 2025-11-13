package br.pucpr.msc

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@Validated
@RequestMapping("/cliente")
@Tag(name = "Clientes", description = "Operações de gestão de clientes")
class ClienteController(
    private val clienteService: ClienteService
) {

    @Operation(summary = "Cria um ou mais clientes")
    @PostMapping
    fun criarClientes(@Valid @RequestBody requisicoes: List<ClienteRequest>): ResponseEntity<List<ClienteResponse>> {
        val criados = clienteService.criarClientes(requisicoes).map { it.toResponse() }
        return ResponseEntity.status(HttpStatus.CREATED).body(criados)
    }

    @Operation(summary = "Busca um cliente pelo identificador")
    @GetMapping("/{id}")
    fun obterCliente(@PathVariable("id") id: Long): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.obterPorId(id)?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado")
        return ResponseEntity.ok(cliente)
    }

    @Operation(summary = "Lista todos os clientes cadastrados")
    @GetMapping
    fun listarClientes(@RequestParam(required = false) ativos: Boolean?): ResponseEntity<List<ClienteResponse>> {
        val clientes = if (ativos == true) {
            clienteService.listarAtivos()
        } else {
            clienteService.listar()
        }.map { it.toResponse() }
        return ResponseEntity.ok(clientes)
    }

    @Operation(summary = "Busca um cliente pelo CPF")
    @GetMapping("/cpf/{cpf}")
    fun obterClientePorCpf(@PathVariable("cpf") cpf: String): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.obterPorCpf(cpf)?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado")
        return ResponseEntity.ok(cliente)
    }

    @Operation(summary = "Busca um cliente pelo email")
    @GetMapping("/email/{email}")
    fun obterClientePorEmail(@PathVariable("email") email: String): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.obterPorEmail(email)?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado")
        return ResponseEntity.ok(cliente)
    }

    @Operation(summary = "Atualiza um cliente existente")
    @PutMapping("/{id}")
    fun atualizarCliente(
        @PathVariable("id") id: Long,
        @Valid @RequestBody requisicao: ClienteRequest
    ): ResponseEntity<ClienteResponse> {
        val atualizado = clienteService.atualizar(id, requisicao).toResponse()
        return ResponseEntity.ok(atualizado)
    }

    @Operation(summary = "Ativa ou desativa um cliente")
    @PatchMapping("/{id}/status")
    fun ativarDesativarCliente(
        @PathVariable("id") id: Long,
        @RequestParam ativo: Boolean
    ): ResponseEntity<ClienteResponse> {
        val atualizado = clienteService.ativarDesativar(id, ativo).toResponse()
        return ResponseEntity.ok(atualizado)
    }

    @Operation(summary = "Remove um cliente do sistema")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removerCliente(@PathVariable("id") id: Long) {
        clienteService.remover(id)
    }
}

