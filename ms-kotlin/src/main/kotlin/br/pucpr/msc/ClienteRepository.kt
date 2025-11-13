package br.pucpr.msc

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClienteRepository : JpaRepository<ClienteEntity, Long> {
    fun findByCpf(cpf: String): ClienteEntity?
    fun existsByCpf(cpf: String): Boolean
    fun findByEmail(email: String): ClienteEntity?
    fun existsByEmail(email: String): Boolean
    fun findByAtivo(ativo: Boolean): List<ClienteEntity>
}

