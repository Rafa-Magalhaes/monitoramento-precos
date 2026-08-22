package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.converter.MissaoBuscaConverter;
import com.rafael.monitoramento_precos.domain.exception.ConflictException;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.infrastructure.repository.MissaoBuscaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MissaoBuscaServiceTest {

    @InjectMocks
    private MissaoBuscaService missaoBuscaService;

    @Mock
    private MissaoBuscaRepository missaoBuscaRepository;
    @Mock
    private MissaoBuscaConverter missaoBuscaConverter;

    @Test
    void excluirMissao_DeveLancarException_QuandoUsuarioNaoForDono() {
        String idMissao = "missao-123";
        UUID idDonoVerdadeiro = UUID.randomUUID();
        UUID idHacker = UUID.randomUUID();

        MissaoBusca missao = MissaoBusca.builder()
                .id(idMissao)
                .usuarioId(idDonoVerdadeiro)
                .build();

        Mockito.when(missaoBuscaRepository.findById(idMissao)).thenReturn(Optional.of(missao));

        // Tenta excluir usando um token diferente do dono original
        ConflictException exception = Assertions.assertThrows(ConflictException.class, () ->
                missaoBuscaService.excluirMissao(idMissao, idHacker));

        Assertions.assertEquals("Acesso negado. Você não tem permissão para excluir esta missão.", exception.getMessage());
        Mockito.verify(missaoBuscaRepository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void buscarPorId_DeveRetornarMissao_QuandoUsuarioForDono() {
        // Cenário (Arrange)
        String idMissao = "missao-123";
        UUID usuarioId = UUID.randomUUID();

        MissaoBusca missao = MissaoBusca.builder()
                .id(idMissao)
                .usuarioId(usuarioId)
                .termoDaBusca("Placa de Vídeo")
                .build();

        Mockito.when(missaoBuscaRepository.findById(idMissao)).thenReturn(Optional.of(missao));

        // Ação (Act)
        MissaoBusca resultado = missaoBuscaService.buscarPorId(idMissao, usuarioId);

        // Verificação (Assert)
        Assertions.assertNotNull(resultado);
        Assertions.assertEquals(idMissao, resultado.getId());
        Assertions.assertEquals("Placa de Vídeo", resultado.getTermoDaBusca());
    }

    @Test
    void buscarPorId_DeveLancarException_QuandoUsuarioNaoForDono() {
        // Cenário (Arrange)
        String idMissao = "missao-123";
        UUID donoVerdadeiro = UUID.randomUUID();
        UUID hacker = UUID.randomUUID();

        MissaoBusca missao = MissaoBusca.builder()
                .id(idMissao)
                .usuarioId(donoVerdadeiro)
                .build();

        Mockito.when(missaoBuscaRepository.findById(idMissao)).thenReturn(Optional.of(missao));

        // Ação & Verificação (Act & Assert)
        ConflictException exception = Assertions.assertThrows(ConflictException.class, () ->
                missaoBuscaService.buscarPorId(idMissao, hacker));

        Assertions.assertEquals("Acesso negado. Você não tem permissão para acessar esta missão.", exception.getMessage());
    }
}