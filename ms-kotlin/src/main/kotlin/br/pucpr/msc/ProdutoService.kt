package br.pucpr.msc

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ProdutoService(
    private val produtoRepository: ProdutoRepository
) {
    private val gruposDeEstoque = mapOf(
        100L to "Bebidas e Cafés",
        200L to "Alimentos e Mercearia",
        300L to "Tecnologia e Acessórios",
        400L to "Casa e Escritório"
    )

    @EventListener(ApplicationReadyEvent::class)
    fun inicializarCatalogo() {
        if (produtoRepository.count() == 0L) {
            criarProdutos(
                listOf(
                    ProdutoRequest(101, "Café especial em grãos 1kg", 74.9, 100),
                    ProdutoRequest(202, "Caixa de barras de cereal sortidas", 39.5, 200),
                    ProdutoRequest(303, "Teclado mecânico compacto", 349.9, 300)
                )
            )
        }
    }

    @Transactional
    fun criarProdutos(requisicoes: List<ProdutoRequest>): List<Produto> {
        if (requisicoes.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A lista de produtos não pode estar vazia")
        }

        return requisicoes.map { requisicao ->
            val codigoProduto = requisicao.codigoProduto
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Código do produto inválido")
            val descricao = requisicao.descricao?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Descrição do produto inválida")
            val preco = requisicao.preco
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Preço do produto inválido")
            val codGruEst = requisicao.codGruEst
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Código do grupo de estoque inválido")

            val descricaoGrupo = gruposDeEstoque[codGruEst]
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de grupo de estoque não encontrado")

            if (produtoRepository.existsByCodigoProduto(codigoProduto)) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Já existe um produto com o código $codigoProduto")
            }

            val salvo = produtoRepository.save(
                ProdutoEntity(
                    codigoProduto = codigoProduto,
                    descricao = descricao,
                    preco = preco,
                    codGruEst = codGruEst,
                    descricaoGrupoEstoque = descricaoGrupo
                )
            )

            salvo.toProduto()
        }
    }

    @Transactional(readOnly = true)
    fun obterPorId(id: Long): Produto? = produtoRepository.findById(id).orElse(null)?.toProduto()

    @Transactional(readOnly = true)
    fun listar(): List<Produto> = produtoRepository
        .findAll(Sort.by(Sort.Direction.ASC, "id"))
        .map { it.toProduto() }

    @Transactional(readOnly = true)
    fun obterPorCodigo(codigoProduto: Int): Produto? =
        produtoRepository.findByCodigoProduto(codigoProduto)?.toProduto()

    @Transactional(readOnly = true)
    fun obterCodigoPorCodigo(codigoProduto: Int): Int? =
        if (produtoRepository.existsByCodigoProduto(codigoProduto)) codigoProduto else null

    @Transactional
    fun atualizar(id: Long, requisicao: ProdutoRequest): Produto {
        val existente = produtoRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado") }

        val codigoProduto = requisicao.codigoProduto
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Código do produto inválido")
        val descricao = requisicao.descricao?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Descrição do produto inválida")
        val preco = requisicao.preco
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Preço do produto inválido")
        val codGruEst = requisicao.codGruEst
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Código do grupo de estoque inválido")

        val descricaoGrupo = gruposDeEstoque[codGruEst]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de grupo de estoque não encontrado")

        val codigoAtual = existente.codigoProduto
        if (codigoProduto != codigoAtual && produtoRepository.existsByCodigoProduto(codigoProduto)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Já existe um produto com o código $codigoProduto")
        }

        existente.codigoProduto = codigoProduto
        existente.descricao = descricao
        existente.preco = preco
        existente.codGruEst = codGruEst
        existente.descricaoGrupoEstoque = descricaoGrupo

        return produtoRepository.save(existente).toProduto()
    }

    @Transactional
    fun remover(id: Long) {
        if (!produtoRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado")
        }

        produtoRepository.deleteById(id)
    }
}
