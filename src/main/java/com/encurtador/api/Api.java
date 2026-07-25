package com.encurtador.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.encurtador.dados.Banco;
import com.encurtador.dados.tabelas.Hashing;
import com.encurtador.dados.tabelas.Link;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class Api {
    private String host;
    private int port;
    private Banco bd;
    private Hash hash;

    public Api(String host, int port, Banco bd){
        this.host = host;
        this.port = port;
        this.bd = bd;

        this.hash = new Hash();

        Start();
    }

    private String[] get_pargs(HttpExchange c){
        String p = c.getRequestURI().getPath();
        return p.split("/");
    }

    private void Start(){
        try{
            HttpServer rest = HttpServer.create(new InetSocketAddress(this.host, this.port), 0);

            rest.createContext("/", c -> {

                String[] pargs = get_pargs(c);
                if (pargs.length != 2){
                    c.sendResponseHeaders(404, 0);
                    c.close();
                }
                String hash = pargs[1];

                List<Hashing> busca = bd.get("hashing", "hash == '" + hash +"'", Hashing.class);
                if (busca == null){
                    c.sendResponseHeaders(404, 0);
                    c.close();
                }
                String tabela = busca.get(0).tamanho();
                String index_tabela = String.valueOf(busca.get(0).posicao());

                List<Link> busca_link = bd.get(tabela, "index == " + index_tabela, Link.class);
                if (busca_link == null){
                    c.sendResponseHeaders(404, 0);
                    c.close();
                }
                String res = busca_link.get(0).url();
  
                c.getResponseHeaders().set("Location", res);
                c.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                c.sendResponseHeaders(301, 0);
                c.close();
            });

            rest.createContext("/novo", c -> {
                InputStream is = c.getRequestBody();
                String corpo = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                String resposta = "";

                try{
                    JSONObject j = new JSONObject(corpo);
                    String url = j.getString("url");
                    String tamanho = get_tamanho_url(url);
                    String operacao = "url == '" + url + "'";

                    List<Link> busca = bd.get(tamanho, operacao, Link.class);

                    if (busca == null){
                        String[] dados = {bd.hex_encode(url)};
                        int index = bd.put(tamanho, dados);     

                        String url_hash;
                        String salt = "";
                        while (true){
                            url_hash = this.hash.computar(url+salt);
                            salt += "67";
                            List<Hashing> b = bd.get("hashing", "hash == '" + url_hash +"'", Hashing.class);
                            if (b == null) break;
                        }
                        String[] hashing = {bd.hex_encode(url_hash), bd.hex_encode(tamanho), bd.hex_encode(String.valueOf(index))};
                        bd.put("hashing", hashing);

                        resposta = url_hash;
                    }
                    else{
                        resposta = "já existe";
                    }

                }
                catch (JSONException ex){
                    resposta = "json precisa de chave 'url'";
                }
                
                c.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                c.sendResponseHeaders(200, resposta.length());
                try (OutputStream os = c.getResponseBody()) {
                    os.write(resposta.getBytes());
                }
            });

            rest.createContext("/fds", c -> {
                String[] pargs = get_pargs(c);

                if (pargs.length != 3){
                    c.sendResponseHeaders(404, 0);
                    c.close();
                }

                String hash = pargs[2];
                List<Hashing> busca = bd.get("hashing", "hash == '" + hash +"'", Hashing.class);
                if (busca == null){
                    c.sendResponseHeaders(404, 0);
                    c.close();
                }

                int index_self = busca.get(0).index();
                String tabela = busca.get(0).tamanho();
                int index_tabela = busca.get(0).posicao();

                System.out.println(index_self + " | " + tabela + " " + index_tabela);

                int delh = bd.delete("hashing", index_self);
                int delt = bd.delete(tabela, index_tabela);

                if (delh < 0 || delt < 0){
                    c.sendResponseHeaders(500, 0);
                    c.close();
                }

                String res = "removido";

                c.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                c.sendResponseHeaders(200, res.length());
                try (OutputStream os = c.getResponseBody()) {
                    os.write(res.getBytes());
                }
            });

            rest.start();
        } 
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private static String get_tamanho_url(String url){
        int l = url.length();
        if (l <= 50) return "curto";
        if (l <= 100) return "medio";
        if (l <= 200) return "longo";
        if (l <= 500) return "ultra";

        return "wtf";
    }
}
