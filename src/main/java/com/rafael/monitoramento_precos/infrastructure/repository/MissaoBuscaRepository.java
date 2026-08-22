package com.rafael.monitoramento_precos.infrastructure.repository;

import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MissaoBuscaRepository extends MongoRepository<MissaoBusca, String> {

    List<MissaoBusca> findByUsuarioId(UUID usuarioId);

    void deleteByUsuarioId(UUID usuarioId);

    List<MissaoBusca> findByAtivoTrue();

    List<MissaoBusca> findByAtivoTrueAndDataExpiracaoBefore(LocalDateTime dataAtual);
}