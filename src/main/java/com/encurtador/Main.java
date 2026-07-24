package com.encurtador;

import java.util.List;

import com.encurtador.dados.Banco;
import com.encurtador.dados.tabelas.Link;

public class Main {

    public static void main(String[] args) {
        System.out.println("=========================");
        System.out.println("encurtar");
        System.out.println("=========================");

        Banco bd = new Banco("localhost", 6767, "encurtador");

        // String[] dados = {bd.hex_encode("FKFKFFFFF")};
        // bd.put("curto", dados);

        List<Link> res = bd.get("curto", "", Link.class);
        
        if (res != null){
            for (Link h : res) {
                System.err.println(h);
            }
        }


        bd.close();
        //new Api("0.0.0.0", 3000, bd);
    }
}