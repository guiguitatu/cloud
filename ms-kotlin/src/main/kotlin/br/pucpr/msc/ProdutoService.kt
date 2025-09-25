package br.pucpr.msc

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class ProdutoService {
    private val produtos = ConcurrentHashMap<Long, Produto>()
    private val idsPorCodigo = ConcurrentHashMap<Int, Long>()
    private val sequence = AtomicLong(0)

    private val gruposDeEstoque = mapOf(
        100L to "Bebidas e Cafés",
        200L to "Alimentos e Mercearia",
        300L to "Tecnologia e Acessórios",
        400L to "Casa e Escritório"
    )

    init {
        criarProdutos(
            listOf(
                ProdutoRequest(101, "Café especial em grãos 1kg", 74.9, 100),
                ProdutoRequest(202, "Caixa de barras de cereal sortidas", 39.5, 200),
                ProdutoRequest(303, "Teclado mecânico compacto", 349.9, 300)
            )
        )
    }

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

            if (idsPorCodigo.containsKey(codigoProduto)) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Já existe um produto com o código $codigoProduto")
            }

            val id = sequence.incrementAndGet()
            val produto = Produto(id, codigoProduto, descricao, preco, codGruEst, descricaoGrupo)
            produtos[id] = produto
            idsPorCodigo[codigoProduto] = id
            produto
        }
    }

    fun obterPorId(id: Long): Produto? = produtos[id]

    fun listar(): List<Produto> = produtos.values.sortedBy { it.id }

    fun obterPorCodigo(codigoProduto: Int): Produto? = idsPorCodigo[codigoProduto]?.let(produtos::get)

    fun obterCodigoPorCodigo(codigoProduto: Int): Int? =
        if (idsPorCodigo.containsKey(codigoProduto)) codigoProduto else null

    fun atualizar(id: Long, requisicao: ProdutoRequest): Produto {
        val existente = produtos[id]
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado")

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
        if (codigoProduto != codigoAtual && idsPorCodigo.containsKey(codigoProduto)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Já existe um produto com o código $codigoProduto")
        }

        idsPorCodigo.remove(codigoAtual)
        idsPorCodigo[codigoProduto] = id

        val atualizado = existente.copy(
            codigoProduto = codigoProduto,
            descricao = descricao,
            preco = preco,
            codGruEst = codGruEst,
            descricaoGrupoEstoque = descricaoGrupo
        )
        produtos[id] = atualizado
        return atualizado
    }

    fun remover(id: Long) {
        val removido = produtos.remove(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado")
        idsPorCodigo.remove(removido.codigoProduto)
    }
}
