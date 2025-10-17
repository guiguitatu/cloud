package br.pucpr.msc

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProdutoRepository : JpaRepository<ProdutoEntity, Long> {
    fun findByCodigoProduto(codigoProduto: Int): ProdutoEntity?
    fun existsByCodigoProduto(codigoProduto: Int): Boolean
}
