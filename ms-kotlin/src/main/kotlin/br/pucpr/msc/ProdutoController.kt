package br.pucpr.msc

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@Validated
@RequestMapping("/produto")
@Tag(name = "Produtos", description = "Operações de catálogo de produtos inspiradas no AuthServer")
class ProdutoController(
    private val produtoService: ProdutoService
) {

    @Operation(summary = "Cria um ou mais produtos")
    @PostMapping
    fun criarProdutos(@Valid @RequestBody requisicoes: List<ProdutoRequest>): ResponseEntity<List<ProdutoResponse>> {
        val criados = produtoService.criarProdutos(requisicoes).map { it.toResponse() }
        return ResponseEntity.status(HttpStatus.CREATED).body(criados)
    }

    @Operation(summary = "Busca um produto pelo identificador")
    @GetMapping("/{id}")
    fun obterProduto(@PathVariable("id") id: Long): ResponseEntity<ProdutoResponse> {
        val produto = produtoService.obterPorId(id)?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado")
        return ResponseEntity.ok(produto)
    }

    @Operation(summary = "Lista todos os produtos cadastrados")
    @GetMapping
    fun listarProdutos(): ResponseEntity<List<ProdutoResponseBusca>> {
        val produtos = produtoService.listar().map { it.toBuscaResponse() }
        return ResponseEntity.ok(produtos)
    }

    @Operation(summary = "Busca um produto pelo código do produto")
    @GetMapping("/codigo/{codigoProduto}")
    fun obterProdutoPorCodigo(@PathVariable("codigoProduto") codigoProduto: Int): ResponseEntity<ProdutoResponse> {
        val produto = produtoService.obterPorCodigo(codigoProduto)?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado")
        return ResponseEntity.ok(produto)
    }

    @Operation(summary = "Verifica se um código de produto está cadastrado")
    @GetMapping("/cod/{codigoProduto}")
    fun obterCodigo(@PathVariable("codigoProduto") codigoProduto: Int): ResponseEntity<Int> {
        val codigo = produtoService.obterCodigoPorCodigo(codigoProduto)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado")
        return ResponseEntity.ok(codigo)
    }

    @Operation(summary = "Atualiza um produto existente")
    @PutMapping("/{id}")
    fun atualizarProduto(
        @PathVariable("id") id: Long,
        @Valid @RequestBody requisicao: ProdutoRequest
    ): ResponseEntity<ProdutoResponse> {
        val atualizado = produtoService.atualizar(id, requisicao).toResponse()
        return ResponseEntity.ok(atualizado)
    }

    @Operation(summary = "Remove um produto do catálogo")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removerProduto(@PathVariable("id") id: Long) {
        produtoService.remover(id)
    }
}
