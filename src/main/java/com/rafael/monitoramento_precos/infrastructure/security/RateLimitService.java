package com.rafael.monitoramento_precos.infrastructure.security;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitService {

    // EXPLICAÇÃO MICRO: Estruturas ConcurrentHashMap garantem Thread-Safety (segurança para requisições paralelas).
    // Cada mapa isola o controle de um endpoint específico.
    private final Map<String, Bucket> cacheLogin = new ConcurrentHashMap<>();
    private final Map<String, Bucket> cacheMissoes = new ConcurrentHashMap<>();
    private final Map<String, Bucket> cacheCadastro = new ConcurrentHashMap<>();

    public Bucket resolverBucketLogin(String ip) {
        // EXPLICAÇÃO MICRO: computeIfAbsent cria o balde apenas se o IP ainda não existir no mapa.
        // Capacidade: 5 tokens. Reabastecimento: 5 tokens a cada 15 minutos. Extrema rigidez contra Força Bruta.
        return cacheLogin.computeIfAbsent(ip, key -> Bucket.builder()
                .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofMinutes(15)))
                .build());
    }

    public Bucket resolverBucketCriacaoMissao(String ip) {
        // EXPLICAÇÃO MICRO: Capacidade: 10 tokens. Reabastecimento: 10 a cada 1 minuto. Evita Spam financeiro no Proxy.
        return cacheMissoes.computeIfAbsent(ip, key -> Bucket.builder()
                .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)))
                .build());
    }

    public Bucket resolverBucketCadastro(String ip) {
        // EXPLICAÇÃO MICRO: Capacidade: 3 tokens. Reabastecimento: 3 a cada 1 hora. Bloqueia DDoS em criação de usuários (Argon2id consome muita CPU).
        return cacheCadastro.computeIfAbsent(ip, key -> Bucket.builder()
                .addLimit(limit -> limit.capacity(3).refillGreedy(3, Duration.ofHours(1)))
                .build());
    }
}