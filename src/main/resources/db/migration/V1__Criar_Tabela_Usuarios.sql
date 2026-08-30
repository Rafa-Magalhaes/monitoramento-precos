CREATE TABLE tb_usuarios (
                             id UUID PRIMARY KEY,
                             nome VARCHAR(100) NOT NULL,
                             email VARCHAR(150) NOT NULL UNIQUE,
                             senha VARCHAR(255) NOT NULL,
                             telefone VARCHAR(20) NOT NULL,
                             role VARCHAR(50) NOT NULL,
                             ativo BOOLEAN NOT NULL DEFAULT TRUE,
                             data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             data_atualizacao TIMESTAMP
);

ALTER TABLE tb_usuarios ENABLE ROW LEVEL SECURITY;