package com.txtnet.txtnetbrowser.basest;

import java.util.Arrays;
import java.util.Collections;

public class Decode {
    public int[] decode_raw(int input_base, int output_base, int input_ratio, int output_ratio, int[] input_data) {
        if (input_data.length % input_ratio != 0) {
            throw new IllegalArgumentException("Decoding requires input length to be an exact multiple of the input ratio, or for padding to be used to ensure this.");
        }

        int[] input_workon = input_data.clone();
        int padding_length = Collections.frequency(Arrays.asList(input_workon), input_base);
        for (int s = 0; s < input_workon.length; s++) {
            if (input_workon[s] == input_base) {
                input_workon[s] = input_base - 1;
            }else{
                //input_workon[s] = s;
            }
        }
        com.txtnet.txtnetbrowser.basest.Encode encoder = new com.txtnet.txtnetbrowser.basest.Encode();
        int[] output_data = encoder.encode_raw(input_base, output_base, input_ratio, output_ratio, input_workon);

        output_data = Arrays.copyOfRange(output_data, 0, output_data.length - padding_length);

        return output_data;
    }
}
