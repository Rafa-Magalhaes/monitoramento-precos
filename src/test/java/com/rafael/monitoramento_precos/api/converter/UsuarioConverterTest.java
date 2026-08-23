package com.rafael.monitoramento_precos.api.converter;

import com.rafael.monitoramento_precos.api.dto.request.UsuarioCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.UsuarioResponseDTO;
import com.rafael.monitoramento_precos.domain.enums.Role;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class UsuarioConverterTest {

    private UsuarioConverter converter;

    @BeforeEach
    void setUp() {
        // Como o Converter não tem Injeção de Dependências de outras classes,
        // instanciamos ele diretamente, sem precisar de @InjectMocks ou @Mock
        converter = new UsuarioConverter();
    }

    @Test
    void toEntity_DeveHigienizarNomeEEmail_ETornarUsuarioAtivo() {
        // Cenário (Arrange): Dados sujos vindos do frontend
        UsuarioCreateRequestDTO dtoSujo = UsuarioCreateRequestDTO.builder()
                .nome("   Babi Stein   ")
                .email(" BABI@GMAIL.COM ")
                .telefone("81999999999")
                .senha("senhaFake")
                .build();

        String senhaHash = "hash12345";

        // Ação (Act)
        Usuario entidade = converter.toEntity(dtoSujo, senhaHash);

        // Verificação (Assert): A prova matemática de que o Converter limpou a sujeira
        Assertions.assertEquals("Babi Stein", entidade.getNome());
        Assertions.assertEquals("babi@gmail.com", entidade.getEmail()); // Minúsculo e sem espaço
        Assertions.assertEquals("81999999999", entidade.getTelefone());
        Assertions.assertEquals("hash12345", entidade.getSenha());
        Assertions.assertEquals(Role.ROLE_USER, entidade.getRole());
        Assertions.assertTrue(entidade.getAtivo()); // Garante que a conta nasce ativada
    }

    @Test
    void toResponseDTO_DeveMapearCorretamenteDaEntidadeParaDTO() {
        // Cenário (Arrange)
        UUID id = UUID.randomUUID();
        Usuario entidade = Usuario.builder()
                .id(id)
                .nome("Rafael")
                .email("rafael@gmail.com")
                .telefone("81900000000")
                .role(Role.ROLE_ADMIN)
                .senha("senhaSuperSecreta") // Não deve vazar!
                .build();

        // Ação (Act)
        UsuarioResponseDTO response = converter.toResponseDTO(entidade);

        // Verificação (Assert)
        Assertions.assertEquals(id, response.getId());
        Assertions.assertEquals("Rafael", response.getNome());
        Assertions.assertEquals("rafael@gmail.com", response.getEmail());
        Assertions.assertEquals("81900000000", response.getTelefone());
        Assertions.assertEquals(Role.ROLE_ADMIN, response.getRole());
    }
}