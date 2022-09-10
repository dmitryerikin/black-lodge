package com.dmitryerikin.android.blacklodge.Utilities;

import android.util.Log;

public class ArrayConverter {

    /**
    Ни один метод не работает правильно.
     */
    private static final String TAG = ArrayConverter.class.getSimpleName();

    public static byte[] shortArrayToByteArray(short[] shortArray) {
        byte[] byteArray = new byte[shortArray.length * 2];

        for(int i = 0; i < shortArray.length; i += 2) {
            Log.d(TAG, String.valueOf(shortArray[i]));
            byteArray[i] = (byte) (shortArray[i]);
            byteArray[i+1] = (byte) (shortArray[i] >>> 8);
        }
        return byteArray;
    }

    public static short[] byteArrayToShortArray(byte[] byteArray){
        short[] shortArray = new short[byteArray.length / 2];

        for(int i = 0; i != shortArray.length; i+=2) {
            shortArray[i] = (short) (byteArray[i]  << 8);
            shortArray[i] += ((short) byteArray[i + 1] );
        }

        return shortArray;
    }
}
