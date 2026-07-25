package com.encurtador;

import com.encurtador.api.Api;
import com.encurtador.dados.Banco;

public class Main {

    public static void main(String[] args) {
        System.out.println("=========================");
        System.out.println("encurtar");
        System.out.println("=========================");

                
        Banco bd = new Banco("localhost", 6767, "encurtador");
        new Api("0.0.0.0", 3000, bd);
    }
}