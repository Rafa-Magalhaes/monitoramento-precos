package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.dto.request.LoginRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.LoginResponseDTO;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        // Injeta a variável @Value manualmente para o teste unitário
        ReflectionTestUtils.setField(authService, "pepper", "pimenta-teste");
    }

    @Test
    void autenticar_DeveHigienizarEmail_E_RetornarToken_QuandoCredenciaisValidas() {
        // 1. Cenário (Arrange): Simulamos o usuário digitando tudo bagunçado e com espaços
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .email(" BABI@GMAIL.com   ")
                .senha("senha123")
                .build();

        Usuario usuarioNoBanco = Usuario.builder().email("babi@gmail.com").senha("hashNoBanco").build();

        // Ensinamos o Mockito a esperar APENAS o e-mail 100% minúsculo e limpo
        Mockito.when(usuarioRepository.findByEmail("babi@gmail.com")).thenReturn(Optional.of(usuarioNoBanco));
        Mockito.when(passwordEncoder.matches("senha123pimenta-teste", "hashNoBanco")).thenReturn(true);
        Mockito.when(tokenService.gerarToken(usuarioNoBanco)).thenReturn("token-magico-jwt");

        // 2. Ação (Act)
        LoginResponseDTO response = authService.autenticar(dto);

        // 3. Verificação (Assert)
        Assertions.assertEquals("token-magico-jwt", response.getToken());
        // A prova final: Verifica se o repositório foi chamado com o texto limpo, ignorando a bagunça do DTO
        Mockito.verify(usuarioRepository).findByEmail("babi@gmail.com");
    }

    @Test
    void autenticar_DeveLancarException_QuandoEmailNaoExistir() {
        LoginRequestDTO dto = LoginRequestDTO.builder().email("fantasma@email.com").senha("123").build();

        Mockito.when(usuarioRepository.findByEmail("fantasma@email.com")).thenReturn(Optional.empty());

        Assertions.assertThrows(BadCredentialsException.class, () -> authService.autenticar(dto));
    }
}