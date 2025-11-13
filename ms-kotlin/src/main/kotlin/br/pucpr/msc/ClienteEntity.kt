package br.pucpr.msc

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "clientes")
class ClienteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    var cpf: String = "",

    @Column(nullable = false)
    var nome: String = "",

    @Column(nullable = false)
    var email: String = "",

    @Column(name = "telefone", nullable = true)
    var telefone: String? = null,

    @Column(name = "endereco", nullable = true)
    var endereco: String? = null,

    @Column(name = "cidade", nullable = true)
    var cidade: String? = null,

    @Column(name = "estado", nullable = true, length = 2)
    var estado: String? = null,

    @Column(name = "cep", nullable = true, length = 8)
    var cep: String? = null,

    @Column(name = "ativo", nullable = false)
    var ativo: Boolean = true
)

fun ClienteEntity.toCliente() = Cliente(
    id = requireNotNull(id) { "ClienteEntity.id não pode ser nulo" },
    cpf = cpf,
    nome = nome,
    email = email,
    telefone = telefone,
    endereco = endereco,
    cidade = cidade,
    estado = estado,
    cep = cep,
    ativo = ativo
)

