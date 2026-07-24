package com.encurtador.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;

import com.encurtador.dados.Banco;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class Api {
    private String host;
    private int port;
    // private Banco bd;

    public Api(String host, int port, Banco bd){
        this.host = host;
        this.port = port;
        // this.bd = bd;

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
                    j.getString("url");
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


            rest.start();
        } 
        catch (IOException ex){
            ex.printStackTrace();
        }
    }
}
