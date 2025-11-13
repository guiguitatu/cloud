package br.pucpr.msc

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ClienteService(
    private val clienteRepository: ClienteRepository
) {
    @EventListener(ApplicationReadyEvent::class)
    fun inicializarClientes() {
        if (clienteRepository.count() == 0L) {
            criarClientes(
                listOf(
                    ClienteRequest(
                        cpf = "12345678901",
                        nome = "João Silva",
                        email = "joao.silva@email.com",
                        telefone = "41999998888",
                        endereco = "Rua das Flores, 123",
                        cidade = "Curitiba",
                        estado = "PR",
                        cep = "80010000",
                        ativo = true
                    ),
                    ClienteRequest(
                        cpf = "98765432100",
                        nome = "Maria Santos",
                        email = "maria.santos@email.com",
                        telefone = "41988887777",
                        endereco = "Av. Paulista, 1000",
                        cidade = "São Paulo",
                        estado = "SP",
                        cep = "01310100",
                        ativo = true
                    )
                )
            )
        }
    }

    @Transactional
    fun criarClientes(requisicoes: List<ClienteRequest>): List<Cliente> {
        if (requisicoes.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A lista de clientes não pode estar vazia")
        }

        return requisicoes.map { requisicao ->
            val cpf = requisicao.cpf?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido")
            val nome = requisicao.nome?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome inválido")
            val email = requisicao.email?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email inválido")
            val ativo = requisicao.ativo ?: true

            // Validar formato do CPF
            if (!cpf.matches(Regex("^\\d{11}$"))) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF deve conter exatamente 11 dígitos numéricos")
            }

            // Validar se CPF já existe
            if (clienteRepository.existsByCpf(cpf)) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Já existe um cliente com o CPF $cpf")
            }

            // Validar se email já existe
            if (clienteRepository.existsByEmail(email)) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Já existe um cliente com o email $email")
            }

            val salvo = clienteRepository.save(
                ClienteEntity(
                    cpf = cpf,
                    nome = nome,
                    email = email,
                    telefone = requisicao.telefone?.trim(),
                    endereco = requisicao.endereco?.trim(),
                    cidade = requisicao.cidade?.trim(),
                    estado = requisicao.estado?.trim()?.uppercase(),
                    cep = requisicao.cep?.trim(),
                    ativo = ativo
                )
            )

            salvo.toCliente()
        }
    }

    @Transactional(readOnly = true)
    fun obterPorId(id: Long): Cliente? = clienteRepository.findById(id).orElse(null)?.toCliente()

    @Transactional(readOnly = true)
    fun listar(): List<Cliente> = clienteRepository
        .findAll(Sort.by(Sort.Direction.ASC, "nome"))
        .map { it.toCliente() }

    @Transactional(readOnly = true)
    fun listarAtivos(): List<Cliente> = clienteRepository
        .findByAtivo(true)
        .map { it.toCliente() }

    @Transactional(readOnly = true)
    fun obterPorCpf(cpf: String): Cliente? =
        clienteRepository.findByCpf(cpf)?.toCliente()

    @Transactional(readOnly = true)
    fun obterPorEmail(email: String): Cliente? =
        clienteRepository.findByEmail(email)?.toCliente()

    @Transactional
    fun atualizar(id: Long, requisicao: ClienteRequest): Cliente {
        val existente = clienteRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado") }

        val cpf = requisicao.cpf?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido")
        val nome = requisicao.nome?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome inválido")
        val email = requisicao.email?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email inválido")
        val ativo = requisicao.ativo ?: existente.ativo

        // Validar formato do CPF
        if (!cpf.matches(Regex("^\\d{11}$"))) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF deve conter exatamente 11 dígitos numéricos")
        }

        // Validar se CPF já existe em outro cliente
        val cpfAtual = existente.cpf
        if (cpf != cpfAtual && clienteRepository.existsByCpf(cpf)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Já existe um cliente com o CPF $cpf")
        }

        // Validar se email já existe em outro cliente
        val emailAtual = existente.email
        if (email != emailAtual && clienteRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Já existe um cliente com o email $email")
        }

        existente.cpf = cpf
        existente.nome = nome
        existente.email = email
        existente.telefone = requisicao.telefone?.trim()
        existente.endereco = requisicao.endereco?.trim()
        existente.cidade = requisicao.cidade?.trim()
        existente.estado = requisicao.estado?.trim()?.uppercase()
        existente.cep = requisicao.cep?.trim()
        existente.ativo = ativo

        return clienteRepository.save(existente).toCliente()
    }

    @Transactional
    fun remover(id: Long) {
        if (!clienteRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado")
        }

        clienteRepository.deleteById(id)
    }

    @Transactional
    fun ativarDesativar(id: Long, ativo: Boolean): Cliente {
        val existente = clienteRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado") }

        existente.ativo = ativo
        return clienteRepository.save(existente).toCliente()
    }
}

