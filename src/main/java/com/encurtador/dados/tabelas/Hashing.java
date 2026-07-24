package com.encurtador.dados.tabelas;

public record Hashing(
    int index,
    String hash,
    int tamanho,
    int posicao
) {}