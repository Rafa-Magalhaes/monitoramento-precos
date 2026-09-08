package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.converter.MissaoBuscaConverter;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaUpdateTermoRequestDTO;
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

import java.util.List;
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
    void criarMissao_DeveLancarException_QuandoHouverDuplicidadeDeKeywords() {
        UUID usuarioId = UUID.randomUUID();
        MissaoBuscaCreateRequestDTO dto = MissaoBuscaCreateRequestDTO.builder()
                .termoDaBusca("hdmi cabo forca")
                .build();

        // Missão já salva no banco com as palavras em ordem diferente
        MissaoBusca missaoExistente = MissaoBusca.builder()
                .ativo(true)
                .palavrasChaveExigidas(List.of("CABO", "FORCA", "HDMI"))
                .build();

        Mockito.when(missaoBuscaRepository.findByUsuarioId(usuarioId)).thenReturn(List.of(missaoExistente));

        // Simula o conversor extraindo as palavras da nova string
        Mockito.when(missaoBuscaConverter.extrairPalavrasChave("hdmi cabo forca"))
                .thenReturn(List.of("HDMI", "CABO", "FORCA"));

        ConflictException exception = Assertions.assertThrows(ConflictException.class, () ->
                missaoBuscaService.criarMissao(dto, usuarioId));

        Assertions.assertEquals("Você já possui uma missão de busca ativa para estes mesmos termos.", exception.getMessage());
        Mockito.verify(missaoBuscaRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void criarMissao_CenarioFeliz_DeveSalvarQuandoNaoHouverDuplicidade() {
        UUID usuarioId = UUID.randomUUID();
        MissaoBuscaCreateRequestDTO dto = MissaoBuscaCreateRequestDTO.builder().termoDaBusca("Mouse Gamer").build();
        MissaoBusca missaoConvertida = MissaoBusca.builder().termoDaBusca("Mouse Gamer").build();

        Mockito.when(missaoBuscaRepository.findByUsuarioId(usuarioId)).thenReturn(List.of());
        Mockito.when(missaoBuscaConverter.extrairPalavrasChave("Mouse Gamer")).thenReturn(List.of("MOUSE", "GAMER"));
        Mockito.when(missaoBuscaConverter.toEntity(dto, usuarioId)).thenReturn(missaoConvertida);
        Mockito.when(missaoBuscaRepository.save(missaoConvertida)).thenReturn(missaoConvertida);

        MissaoBusca resultado = missaoBuscaService.criarMissao(dto, usuarioId);

        Assertions.assertNotNull(resultado);
        Mockito.verify(missaoBuscaRepository, Mockito.times(1)).save(missaoConvertida);
    }

    @Test
    void atualizarTermoBusca_DeveLancarException_QuandoHouverConflitoComOutraMissaoAtiva() {
        UUID usuarioId = UUID.randomUUID();
        String missaoIdParaAtualizar = "missao-1";

        MissaoBuscaUpdateTermoRequestDTO dto = MissaoBuscaUpdateTermoRequestDTO.builder()
                .termoDaBusca("RTX 4060")
                .build();

        MissaoBusca missaoAtual = MissaoBusca.builder()
                .id(missaoIdParaAtualizar)
                .usuarioId(usuarioId)
                .build();

        MissaoBusca outraMissao = MissaoBusca.builder()
                .id("missao-2")
                .usuarioId(usuarioId)
                .ativo(true)
                .palavrasChaveExigidas(List.of("4060", "RTX"))
                .build();

        Mockito.when(missaoBuscaRepository.findById(missaoIdParaAtualizar)).thenReturn(Optional.of(missaoAtual));
        Mockito.when(missaoBuscaRepository.findByUsuarioId(usuarioId)).thenReturn(List.of(missaoAtual, outraMissao));
        Mockito.when(missaoBuscaConverter.extrairPalavrasChave("RTX 4060")).thenReturn(List.of("RTX", "4060"));

        ConflictException exception = Assertions.assertThrows(ConflictException.class, () ->
                missaoBuscaService.atualizarTermoBusca(missaoIdParaAtualizar, usuarioId, dto));

        Assertions.assertEquals("Você já possui outra missão de busca ativa para estes mesmos termos.", exception.getMessage());
        Mockito.verify(missaoBuscaRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void atualizarTermoBusca_CenarioFeliz_DeveAtualizarQuandoNaoHouverConflito() {
        UUID usuarioId = UUID.randomUUID();
        String missaoId = "missao-1";
        MissaoBuscaUpdateTermoRequestDTO dto = MissaoBuscaUpdateTermoRequestDTO.builder()
                .termoDaBusca("Teclado Mecanico")
                .build();

        MissaoBusca missaoAtual = MissaoBusca.builder()
                .id(missaoId)
                .usuarioId(usuarioId)
                .termoDaBusca("Teclado Velho")
                .ativo(true)
                .build();

        // Mock das idas ao banco e da conversão
        Mockito.when(missaoBuscaRepository.findById(missaoId)).thenReturn(Optional.of(missaoAtual));
        Mockito.when(missaoBuscaRepository.findByUsuarioId(usuarioId)).thenReturn(List.of(missaoAtual));
        Mockito.when(missaoBuscaConverter.extrairPalavrasChave("Teclado Mecanico")).thenReturn(List.of("TECLADO", "MECANICO"));

        // Executa a atualização
        missaoBuscaService.atualizarTermoBusca(missaoId, usuarioId, dto);

        // Verifica se os dados da missão foram alterados na memória e salvos no banco
        Assertions.assertEquals("Teclado Mecanico", missaoAtual.getTermoDaBusca());
        Assertions.assertTrue(missaoAtual.getPalavrasChaveExigidas().containsAll(List.of("TECLADO", "MECANICO")));
        Mockito.verify(missaoBuscaRepository, Mockito.times(1)).save(missaoAtual);
    }

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

        ConflictException exception = Assertions.assertThrows(ConflictException.class, () ->
                missaoBuscaService.excluirMissao(idMissao, idHacker));

        Assertions.assertEquals("Acesso negado. Você não tem permissão para excluir esta missão.", exception.getMessage());
        Mockito.verify(missaoBuscaRepository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void excluirMissao_CenarioFeliz_DeveDeletarMissao() {
        String missaoId = "missao1";
        UUID usuarioId = UUID.randomUUID();
        MissaoBusca missao = MissaoBusca.builder().id(missaoId).usuarioId(usuarioId).build();

        Mockito.when(missaoBuscaRepository.findById(missaoId)).thenReturn(Optional.of(missao));

        missaoBuscaService.excluirMissao(missaoId, usuarioId);

        Mockito.verify(missaoBuscaRepository, Mockito.times(1)).delete(missao);
    }

    @Test
    void buscarPorId_DeveRetornarMissao_QuandoUsuarioForDono() {
        String idMissao = "missao-123";
        UUID usuarioId = UUID.randomUUID();

        MissaoBusca missao = MissaoBusca.builder()
                .id(idMissao)
                .usuarioId(usuarioId)
                .termoDaBusca("Placa de Vídeo")
                .build();

        Mockito.when(missaoBuscaRepository.findById(idMissao)).thenReturn(Optional.of(missao));

        MissaoBusca resultado = missaoBuscaService.buscarPorId(idMissao, usuarioId);

        Assertions.assertNotNull(resultado);
        Assertions.assertEquals(idMissao, resultado.getId());
        Assertions.assertEquals("Placa de Vídeo", resultado.getTermoDaBusca());
    }

    @Test
    void buscarPorId_DeveLancarException_QuandoUsuarioNaoForDono() {
        String idMissao = "missao-123";
        UUID donoVerdadeiro = UUID.randomUUID();
        UUID hacker = UUID.randomUUID();

        MissaoBusca missao = MissaoBusca.builder()
                .id(idMissao)
                .usuarioId(donoVerdadeiro)
                .build();

        Mockito.when(missaoBuscaRepository.findById(idMissao)).thenReturn(Optional.of(missao));

        ConflictException exception = Assertions.assertThrows(ConflictException.class, () ->
                missaoBuscaService.buscarPorId(idMissao, hacker));

        Assertions.assertEquals("Acesso negado. Você não tem permissão para acessar esta missão.", exception.getMessage());
    }
}