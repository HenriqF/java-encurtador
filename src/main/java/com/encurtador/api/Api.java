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
                String res = "https://osu.ppy.sh/";

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
                    String url_hash = this.hash.computar(url);
                    System.out.println(url_hash);


                    List<Link> busca = bd.get(tamanho, operacao, Link.class);


                    if (busca == null){
                        String[] dados = {bd.hex_encode(url)};
                        int index = bd.put(tamanho, dados);       
                        System.out.println(index);

                    }
                    else{
                        System.out.println("já existe");
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

            rest.createContext("/fds/", c -> {
                String[] pargs = get_pargs(c);
                System.err.println(pargs.length);

                String res = "";
                if (pargs.length != 3){
                    res = "sem hash do URL";
                }
                else {
                    res = pargs[2];
                }

                c.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                c.sendResponseHeaders(200, res.length());
                try (OutputStream os = c.getResponseBody()) {
                    os.write(res.getBytes());
                }
            });

            rest.createContext("/hash", c -> {
                String[] pargs = get_pargs(c);

                String res = this.hash.computar(pargs[2]);

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
