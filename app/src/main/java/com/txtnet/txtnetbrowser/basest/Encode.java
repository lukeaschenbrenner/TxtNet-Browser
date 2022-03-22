package com.txtnet.txtnetbrowser.basest;

import java.math.BigInteger;
import java.util.Arrays;

public class Encode {
    public int nearest_length(int input_length, int input_ratio) {
        int overlap = input_length % input_ratio;
        if (overlap == 0) {
            return input_length;
        } else {
            return ((((input_length - overlap) / input_ratio) + 1) * input_ratio);
        }
    }

    public int[] encode_raw(int input_base, int output_base, int input_ratio, int output_ratio, int[] input_data) {
        int[] input_workon = input_data.clone();
        long input_length = input_workon.length;

        if (input_base < output_base && input_length % input_ratio != 0) {
            throw new IllegalArgumentException("Input data length must be exact multiple of input ratio when output base is larger than input base");
        }
        int input_nearest_length = nearest_length((int) input_length, input_ratio);
        long padding_length = (input_nearest_length - input_length);
        int output_length = (input_nearest_length / input_ratio) * output_ratio;
        int[] output_data = new int[output_length];
        input_workon = Arrays.copyOf(input_workon, (int) (input_workon.length + padding_length));

        for (int i = 0; i < input_nearest_length; i += input_ratio) {
            BigInteger store = new BigInteger("0");
            for (int j = 0; j < input_ratio; j++) {
                BigInteger symbol = new BigInteger(String.valueOf(input_workon[i + j]));
                symbol = symbol.multiply(BigInteger.valueOf((long)input_base).pow((input_ratio - j - 1)));
                String storeString = store.toString();

                store = store.add(symbol);
                String symbolString = symbol.toString();
            }
            for (int k = 0; k < output_ratio; k++) {
                int index = ((i / input_ratio) * output_ratio) + k;
                BigInteger symbol = store.divide(BigInteger.valueOf((long)output_base).pow(output_ratio - k - 1));

                output_data[index] = symbol.intValue();
                store = store.subtract(symbol.multiply(BigInteger.valueOf((long)output_base).pow(output_ratio-k-1)));

            }
        }

        for(int i = (int) (output_length-padding_length); i < output_length; i++){
            output_data[i] = output_base;
        }
        return output_data;

    }
}
