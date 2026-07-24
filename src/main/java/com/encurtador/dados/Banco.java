package com.encurtador.dados;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Scanner;

public class Banco {
    private static int default_num_size = 4;
    
    private static String nome_banco;
    private static Socket socket;
    private static PrintStream bd_in;
    private static Scanner bd_out;

    public Banco(String host, int port, String banco){
        try{
            nome_banco = banco;

            socket = new Socket(InetAddress.getByName(host), port);
            bd_in = new PrintStream(socket.getOutputStream(), true);
            bd_out = new Scanner(socket.getInputStream());

            init();
        }
        catch (IOException ex){
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private String send(String msg) throws IOException{
        if (socket.isConnected()){
            bd_in.println(msg);
            String res = bd_out.nextLine();

            System.out.printf("Cliente: %.25s\n", msg);
            System.out.printf("Banco  : %.25s\n", res);

            return res;
        }
        else{
            throw new IOException("banco morto");
        }
    }
    private void init() throws IOException{
        String res = send("usar " + nome_banco);
        if (res.equals("ok")){
            return;
        }
    
        throw new IOException("banco morto");
    }
    public void close(){
        bd_in.close();
        bd_out.close();
    }



    //retorna o index da nova entrada, -1 em caso de erro.
    public int put(String tabela, String[] dados){
        String comando = "put " + tabela;
        for (String d : dados) {
            comando += " " + d;
        }

        try{
            String res = send(comando);
            int index = -1;

            if (res.startsWith("ok ") && res.length() == 7){
                String resto = res.substring(3, 7);

                byte[] bytes = resto.getBytes(StandardCharsets.ISO_8859_1);
                index = ByteBuffer.wrap(bytes).getInt();
                System.out.printf("Index nova entrada: [%d]\n", index);
            }

            return index;
        }
        catch (IOException ex){
            ex.printStackTrace();
            return -1;
        }
    }

    //retorna uma lista de recordes com os resultados da pesquisa.
    public <Generic> List<Generic> get(String tabela, String operacao, Class<Generic> recorde){
        if (!recorde.isRecord() || tabela == null || operacao == null){
            return null;
        }

        String comando = "get " + tabela + " " + hex_encode(operacao);

        try{
            String res = send(comando);
            return representar(res, recorde);
        }
        catch (IOException ex){
            ex.printStackTrace();
        }

        return null;
    }

    //retorna -1 se falhou em deletar.
    public int delete(String tabela, int index){
        String comando = "del " + tabela + " " + hex_encode(String.valueOf(index));

        try{
            String res = send(comando);
            if (res.equals("ok")){
                return 0;
            }
            return -1;
        }
        catch (IOException ex){
            ex.printStackTrace();
            return -1;
        }
    }


    //Utilidade
    public <Generic> List<Generic> representar(String dados, Class<Generic> recorde){
        try{
            byte[] res = HexFormat.of().parseHex(dados);

            RecordComponent[] regras = recorde.getRecordComponents();
            Class<?>[] tipos = Arrays.stream(regras).map(RecordComponent::getType).toArray(Class<?>[]::new);
            Constructor<Generic> construtor = recorde.getDeclaredConstructor(tipos);

            Object[] valores = new Object[regras.length];

            List<Generic> resultado = new ArrayList<>();

            int counter = 0;
            int offset = 0;
            while (offset < res.length){
                byte[] len_prox = Arrays.copyOfRange(res, offset, offset+default_num_size);
                int len_prox_int = ByteBuffer.wrap(len_prox).getInt();

                byte[] prox = Arrays.copyOfRange(res, offset+default_num_size, offset+default_num_size+len_prox_int);
                String prox_str = new String(prox, StandardCharsets.UTF_8);

                Class<?> prox_tipo = regras[counter].getType();

                if (prox_tipo == int.class){
                    int n = ByteBuffer.wrap(prox_str.getBytes(StandardCharsets.ISO_8859_1)).getInt();
                    valores[counter] = n;
                }
                else if (prox_tipo == String.class){
                    valores[counter] = prox_str;
                }


        
                offset += (len_prox_int + default_num_size);
                counter++;
                if (counter % regras.length == 0){
                    resultado.add(construtor.newInstance(valores));
                    counter = 0;
                }
            }

            return resultado;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public String hex_encode(String in){
        String hex = HexFormat.of().formatHex(in.getBytes(StandardCharsets.UTF_8));
        return hex;
    }
    public String hex_decode(String in) throws IllegalArgumentException{
        byte[] bytes = HexFormat.of().parseHex(in);
        String dec = new String(bytes, StandardCharsets.UTF_8);

        return dec;
    }
}
