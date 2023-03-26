package com.txtnet.txtnetbrowser.basest;

import java.util.Arrays;

public class Base10Conversions {

    public static String[] explode(String s) {
        String[] arr = new String[s.length()];
        for(int i = 0; i < s.length(); i++)
        {
            arr[i] = String.valueOf((s.charAt(i)));
        }
        return arr;
    }

    public static int r2v(String data){
        return(r2v(explode(data)));
    }


    //public static final String[] SYMBOL_TABLE = {"@", "£", "$", "¥", "è", "é", "ù", "ì", "ò", "Ç", "\n", "Ø", "ø", "Å", "å", "Δ", "_", "Φ", "Γ", "Λ", "Ω", "Π", "Ψ", "Σ", "Θ", "Ξ", "Æ", "æ", "ß", "É", "!", "\"", "#", "¤", "%", "&", "'", "(", ")", "*", "+", ",", "-", ".", "/", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ":", ";", "<", "=", ">", "?", "¡", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "Ä", "Ö", "Ñ", "Ü", "`", "¿", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "ä", "ö", "ñ", "ü", "à"};
    //public static final String[] SYMBOL_TABLE_OLD = {"@","£","$","¥","è","é","ù","ì","ò","Ç","\n","Ø","ø","Å","å","_","Æ","æ","ß","É","!","\"","#","¤","%","&","'","(",")","*","+",",","-",".","/","0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?","¡","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Ä","Ö","Ñ","Ü","\u00A7","¿","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ä","ö","ñ","ü","à"};
    public static final String[] SYMBOL_TABLE = {"@","£","$","¥","è","é","ù","ì","ò","Ç","\n","Ø","ø","Å","å","_","Æ","æ","ß","É","!","\"","#","¤","%","&","'","(",")","*","+",",","-",".","/","0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?","¡","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Ä","Ö","Ñ","Ü","\u00A7","¿","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ä","ö","ñ","ü","à"};
    public static String[] v2r(int[] numbers){
        int[] tempNum = numbers;
        int alphabet_length = SYMBOL_TABLE.length;
        String[] result = new String[numbers.length];

        for(int i = 0; i < tempNum.length; i++) {
            result[i] = "";
            int num = tempNum[i];
            int originalNum = num;
            if (num == 0) {
                result[i] = (SYMBOL_TABLE[0] + SYMBOL_TABLE[0]);
            }
            while (num > 0) {
               // String[] tempArr = new String[result.length + 1];
                result[i] = SYMBOL_TABLE[num % alphabet_length] + result[i];
                //System.arraycopy(result, 0, tempArr, 1, result.length);
                //System.arraycopy(tempArr, 0, result, 0, result.length);
                num /= alphabet_length;
               //result = tempArr;
            }
            if (originalNum < alphabet_length && originalNum != 0) {
                //String[] tempArr = new String[result.length + 1];
                //tempArr[0] = SYMBOL_TABLE[0];
                //System.arraycopy(result, 0, tempArr, 1, result.length);
                //result = tempArr;
                result[i] = SYMBOL_TABLE[0] + result[i];
            }
        }
        return result;
    }

    public static int r2v(String[] data){
        int alphabet_length = SYMBOL_TABLE.length;
        int num = 0;

        for(int i = 0; i < data.length; i++) {
                //String c = data[i];
                num = alphabet_length * num + Arrays.asList(SYMBOL_TABLE).indexOf(data[i]);

        }
        return num;
    }
    /*
        public static int[] r2v(String[] data){
        int alphabet_length = SYMBOL_TABLE.length;
        int[] nums = new int[data.length];
        for(int num : nums) {
            for (int i = 0, n = data.length; i < n; i++) {
                char c = data[i].charAt(0);
                //String c = data[i];
                num = alphabet_length * num + Arrays.binarySearch(SYMBOL_TABLE, String.valueOf(c));
            }
        }
        return nums;
    }
     */
}
