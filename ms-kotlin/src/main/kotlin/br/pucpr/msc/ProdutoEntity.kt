package br.pucpr.msc

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "produtos")
class ProdutoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "codigo_produto", nullable = false, unique = true)
    var codigoProduto: Int = 0,

    @Column(nullable = false)
    var descricao: String = "",

    @Column(nullable = false)
    var preco: Double = 0.0,

    @Column(name = "cod_gru_est", nullable = false)
    var codGruEst: Long = 0,

    @Column(name = "descricao_grupo_estoque", nullable = false)
    var descricaoGrupoEstoque: String = ""
)

fun ProdutoEntity.toProduto() = Produto(
    id = requireNotNull(id) { "ProdutoEntity.id não pode ser nulo" },
    codigoProduto = codigoProduto,
    descricao = descricao,
    preco = preco,
    codGruEst = codGruEst,
    descricaoGrupoEstoque = descricaoGrupoEstoque
)
