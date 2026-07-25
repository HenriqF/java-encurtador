package com.encurtador.api;

public class Hash {
    private static String letras = "abcdefghijklmnopqrstuvwxyz0123456789_";
    private static int letras_len = letras.length();

    private static int transform(int self, int other, String str, int bloco_size){
        for (int i = 0; i < bloco_size; i++){
            int m = (other >> 3) + self * (byte) str.charAt(i);
            self += m;
        }
        
        return self;
    }

    public String computar(String str){
        int nums[] = {67, 7001, 173, 127, 181, 193, 10037, 27487, 9281, 7583}; //top 10 numeros alearotios
        
        char[] from = new char[7];
        int bloco_size = from.length;
                
        int offset = 0;
        for (int i = 0; i < str.length(); i++){
            from[offset++] = str.charAt(i);

            if (offset % bloco_size == 0){
                String froms = new String(from);

                for (int k = 0; k < nums.length-1; k++){
                    nums[k] = transform(nums[k], nums[k+1], froms, bloco_size);
                    nums[k+1] = transform(nums[k+1], nums[k], froms, bloco_size);
                }
                offset = 0;
            }
        }

        if (offset != 0){
            String froms = new String(from);
            
            for (int k = 0; k < nums.length-1; k++){
                nums[k] = transform(nums[k], nums[k+1], froms, offset);
                nums[k+1] = transform(nums[k+1], nums[k], froms, offset);
            }
        }


        String res = "";
        for (int i = 0; i < nums.length; i++){
            res += letras.charAt(Math.abs(nums[i])%letras_len);
        }

        return res;
    }

    public Hash(){

    }
}
