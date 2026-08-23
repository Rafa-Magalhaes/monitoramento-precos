package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.converter.UsuarioConverter;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioCreateRequestDTO;
import com.rafael.monitoramento_precos.domain.exception.ConflictException;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.infrastructure.repository.UsuarioRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @InjectMocks
    private UsuarioService usuarioService;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MissaoBuscaService missaoBuscaService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsuarioConverter usuarioConverter;
    // NOVA INJEÇÃO: Mocado para os testes não tentarem enviar mensagens reais!
    @Mock
    private NotificacaoWhatsAppService notificacaoWhatsAppService;

    @BeforeEach
    void setUp() {
        // Injeta a variável @Value manualmente para o teste unitário
        ReflectionTestUtils.setField(usuarioService, "pepper", "pimenta-teste");
    }

    @Test
    void criarUsuario_DeveLancarException_QuandoEmailJaExiste() {
        UsuarioCreateRequestDTO dto = UsuarioCreateRequestDTO.builder().email("teste@teste.com").build();

        // Simula que o repositório já encontrou alguém com este e-mail
        Mockito.when(usuarioRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(new Usuario()));

        Assertions.assertThrows(ConflictException.class, () -> usuarioService.criarUsuario(dto));

        // Garante que o repositório NUNCA tentou salvar o usuário
        Mockito.verify(usuarioRepository, Mockito.never()).save(Mockito.any());
        // Garante que não manda WhatsApp se falhar
        Mockito.verify(notificacaoWhatsAppService, Mockito.never()).enviarBoasVindas(Mockito.any());
    }

    @Test
    void criarUsuario_DeveHigienizarEmail_E_EnviarBoasVindas() {
        // Cenário (Arrange)
        UsuarioCreateRequestDTO dto = UsuarioCreateRequestDTO.builder()
                .nome(" Babi ")
                .email("   TESTE@TESTE.COM ")
                .senha("senha123")
                .telefone("81999999999")
                .build();

        Usuario usuarioMock = Usuario.builder().email("teste@teste.com").build();

        // Ensinamos o repositório a aceitar APENAS a versão limpa
        Mockito.when(usuarioRepository.findByEmail("teste@teste.com")).thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn("senhaHash");
        Mockito.when(usuarioConverter.toEntity(Mockito.eq(dto), Mockito.anyString())).thenReturn(usuarioMock);
        Mockito.when(usuarioRepository.save(Mockito.any())).thenReturn(usuarioMock);

        // Ação (Act)
        usuarioService.criarUsuario(dto);

        // Verificação (Assert): Prova matemática das lógicas de negócio
        Mockito.verify(usuarioRepository, Mockito.times(1)).findByEmail("teste@teste.com");
        // PROVA MATEMÁTICA: O método de envio do novo WhatsApp FOI chamado 1 vez
        Mockito.verify(notificacaoWhatsAppService, Mockito.times(1)).enviarBoasVindas(Mockito.any(Usuario.class));
    }

    @Test
    void excluirConta_DeveOrquestrarExclusaoEmCascata() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuarioMock = Usuario.builder().id(usuarioId).build();

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioMock));

        usuarioService.excluirConta(usuarioId);

        // Verifica se a exclusão no Mongo foi chamada ANTES da exclusão no Postgres
        Mockito.verify(missaoBuscaService, Mockito.times(1)).excluirTodasMissoes(usuarioId);
        Mockito.verify(usuarioRepository, Mockito.times(1)).delete(usuarioMock);
    }
}