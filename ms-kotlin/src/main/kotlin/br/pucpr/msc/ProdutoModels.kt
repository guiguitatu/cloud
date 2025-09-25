package br.pucpr.msc

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

@Schema(description = "Payload utilizado para criar ou atualizar produtos")
data class ProdutoRequest(
    @field:NotNull(message = "O código do produto é obrigatório")
    @field:Positive(message = "O código do produto deve ser positivo")
    @Schema(description = "Código numérico que identifica o produto", example = "101", required = true)
    val codigoProduto: Int?,

    @field:NotBlank(message = "A descrição do produto é obrigatória")
    @Schema(description = "Descrição curta do produto", example = "Teclado mecânico compacto", required = true)
    val descricao: String?,

    @field:NotNull(message = "O preço do produto é obrigatório")
    @field:PositiveOrZero(message = "O preço do produto não pode ser negativo")
    @Schema(description = "Preço de venda do produto", example = "349.90", required = true)
    val preco: Double?,

    @field:NotNull(message = "O código do grupo de estoque é obrigatório")
    @Schema(description = "Identificador do grupo de estoque ao qual o produto pertence", example = "300", required = true)
    val codGruEst: Long?
)

@Schema(description = "Resposta padrão com os dados do produto cadastrado")
data class ProdutoResponse(
    @Schema(description = "Identificador único do produto", example = "1")
    val id: Long,

    @Schema(description = "Código numérico do produto", example = "101")
    val codigoProduto: Int,

    @Schema(description = "Descrição do produto", example = "Teclado mecânico compacto")
    val descricao: String,

    @Schema(description = "Preço de venda do produto", example = "349.9")
    val preco: Double,

    @Schema(description = "Código do grupo de estoque", example = "300")
    val codGruEst: Long
)

@Schema(description = "Resposta detalhada para listagens de produtos com descrição do grupo de estoque")
data class ProdutoResponseBusca(
    @Schema(description = "Identificador único do produto", example = "1")
    val id: Long,

    @Schema(description = "Código numérico do produto", example = "101")
    val codigoProduto: Int,

    @Schema(description = "Descrição do produto", example = "Teclado mecânico compacto")
    val descricao: String,

    @Schema(description = "Preço de venda do produto", example = "349.9")
    val preco: Double,

    @Schema(description = "Código do grupo de estoque", example = "300")
    val cod: Long,

    @Schema(description = "Descrição do grupo de estoque", example = "Tecnologia e Acessórios")
    val codGruEst: String
)

internal data class Produto(
    val id: Long,
    val codigoProduto: Int,
    val descricao: String,
    val preco: Double,
    val codGruEst: Long,
    val descricaoGrupoEstoque: String
)

internal fun Produto.toResponse() =
    ProdutoResponse(
        id = id,
        codigoProduto = codigoProduto,
        descricao = descricao,
        preco = preco,
        codGruEst = codGruEst
    )

internal fun Produto.toBuscaResponse() =
    ProdutoResponseBusca(
        id = id,
        codigoProduto = codigoProduto,
        descricao = descricao,
        preco = preco,
        cod = codGruEst,
        codGruEst = descricaoGrupoEstoque
    )
