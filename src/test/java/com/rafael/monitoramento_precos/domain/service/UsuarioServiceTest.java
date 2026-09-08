package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.converter.UsuarioConverter;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioUpdateEmailRequestDTO;
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(usuarioService, "pepper", "pimenta-teste");
    }

    @Test
    void criarUsuario_DeveLancarException_QuandoEmailJaExiste() {
        UsuarioCreateRequestDTO dto = UsuarioCreateRequestDTO.builder().email("teste@teste.com").build();

        Mockito.when(usuarioRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(new Usuario()));

        Assertions.assertThrows(ConflictException.class, () -> usuarioService.criarUsuario(dto));

        Mockito.verify(usuarioRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void criarUsuario_DeveHigienizarEmailAntesDeChecarDuplicidade() {
        UsuarioCreateRequestDTO dto = UsuarioCreateRequestDTO.builder()
                .nome(" Babi ")
                .email("   TESTE@TESTE.COM ")
                .senha("senha123")
                .telefone("81999999999")
                .build();

        Usuario usuarioMock = Usuario.builder().email("teste@teste.com").build();

        Mockito.when(usuarioRepository.findByEmail("teste@teste.com")).thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn("senhaHash");
        Mockito.when(usuarioConverter.toEntity(Mockito.eq(dto), Mockito.anyString())).thenReturn(usuarioMock);
        Mockito.when(usuarioRepository.save(Mockito.any())).thenReturn(usuarioMock);

        usuarioService.criarUsuario(dto);

        Mockito.verify(usuarioRepository, Mockito.times(1)).findByEmail("teste@teste.com");
    }

    @Test
    void excluirConta_DeveOrquestrarExclusaoEmCascata() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuarioMock = Usuario.builder().id(usuarioId).build();

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioMock));

        usuarioService.excluirConta(usuarioId);

        Mockito.verify(missaoBuscaService, Mockito.times(1)).excluirTodasMissoes(usuarioId);
        Mockito.verify(usuarioRepository, Mockito.times(1)).delete(usuarioMock);
    }

    @Test
    void atualizarEmail_CenarioFeliz_DeveSalvarNovoEmail() {
        UUID usuarioId = UUID.randomUUID();
        UsuarioUpdateEmailRequestDTO dto = UsuarioUpdateEmailRequestDTO.builder().email("novo@email.com").build();
        Usuario usuario = Usuario.builder().id(usuarioId).email("antigo@email.com").build();

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        Mockito.when(usuarioRepository.existsByEmail("novo@email.com")).thenReturn(false);

        usuarioService.atualizarEmail(usuarioId, dto);

        Assertions.assertEquals("novo@email.com", usuario.getEmail());
        Mockito.verify(usuarioRepository, Mockito.times(1)).save(usuario);
    }

    @Test
    void atualizarEmail_CenarioTriste_LancaExceptionSeEmailEmUso() {
        UUID usuarioId = UUID.randomUUID();
        UsuarioUpdateEmailRequestDTO dto = UsuarioUpdateEmailRequestDTO.builder().email("em-uso@email.com").build();
        Usuario usuario = Usuario.builder().id(usuarioId).email("antigo@email.com").build();

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        Mockito.when(usuarioRepository.existsByEmail("em-uso@email.com")).thenReturn(true);

        Assertions.assertThrows(ConflictException.class, () -> usuarioService.atualizarEmail(usuarioId, dto));
        Mockito.verify(usuarioRepository, Mockito.never()).save(Mockito.any());
    }
}