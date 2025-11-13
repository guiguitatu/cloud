package br.pucpr.msc

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "Payload utilizado para criar ou atualizar clientes")
data class ClienteRequest(
    @field:NotBlank(message = "O CPF é obrigatório")
    @field:Pattern(regexp = "^\\d{11}$", message = "CPF deve conter exatamente 11 dígitos numéricos")
    @Schema(description = "CPF do cliente (11 dígitos)", example = "12345678901", required = true)
    val cpf: String?,

    @field:NotBlank(message = "O nome é obrigatório")
    @field:Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    @Schema(description = "Nome completo do cliente", example = "João Silva", required = true)
    val nome: String?,

    @field:NotBlank(message = "O email é obrigatório")
    @field:Email(message = "Email deve ter formato válido")
    @Schema(description = "Email do cliente", example = "joao.silva@email.com", required = true)
    val email: String?,

    @Schema(description = "Telefone do cliente", example = "41999998888")
    val telefone: String?,

    @Schema(description = "Endereço do cliente", example = "Rua das Flores, 123")
    val endereco: String?,

    @Schema(description = "Cidade do cliente", example = "Curitiba")
    val cidade: String?,

    @Schema(description = "Estado do cliente (UF)", example = "PR")
    val estado: String?,

    @Schema(description = "CEP do cliente (8 dígitos)", example = "80010000")
    val cep: String?,

    @Schema(description = "Status ativo do cliente", example = "true")
    val ativo: Boolean? = true
)

@Schema(description = "Resposta padrão com os dados do cliente cadastrado")
data class ClienteResponse(
    @Schema(description = "Identificador único do cliente", example = "1")
    val id: Long,

    @Schema(description = "CPF do cliente", example = "12345678901")
    val cpf: String,

    @Schema(description = "Nome completo do cliente", example = "João Silva")
    val nome: String,

    @Schema(description = "Email do cliente", example = "joao.silva@email.com")
    val email: String,

    @Schema(description = "Telefone do cliente", example = "41999998888")
    val telefone: String?,

    @Schema(description = "Endereço do cliente", example = "Rua das Flores, 123")
    val endereco: String?,

    @Schema(description = "Cidade do cliente", example = "Curitiba")
    val cidade: String?,

    @Schema(description = "Estado do cliente (UF)", example = "PR")
    val estado: String?,

    @Schema(description = "CEP do cliente", example = "80010000")
    val cep: String?,

    @Schema(description = "Status ativo do cliente", example = "true")
    val ativo: Boolean
)

data class Cliente(
    val id: Long,
    val cpf: String,
    val nome: String,
    val email: String,
    val telefone: String?,
    val endereco: String?,
    val cidade: String?,
    val estado: String?,
    val cep: String?,
    val ativo: Boolean
)

internal fun Cliente.toResponse() =
    ClienteResponse(
        id = id,
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

