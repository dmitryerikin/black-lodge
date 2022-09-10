package com.dmitryerikin.android.blacklodge;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class AudioReverser {

    private static final String TAG = AudioReverser.class.getSimpleName();

    private Thread mThread;
    private File mOriginalFile;
    private File mReversedFile;
    private AudioConfig mConfig;

    private ArrayList<OnCompletionListener> mOnCompletionListenerArrayList;

    public AudioReverser(File originalFile, File reversedFile, AudioConfig config) {
        Log.d(TAG, "AudioReverser: ");
        mOriginalFile = originalFile;
        mReversedFile = reversedFile;
        mConfig = config;
        mOnCompletionListenerArrayList = new ArrayList<>();
    }

    public void reverse() {
        Log.d(TAG, "reverse: ");
        if(mConfig.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    reverse16bitStereoPCM(mOriginalFile, mReversedFile);
                    Log.d(TAG, "run: end");
                }
            });
            mThread.start();
        }
    }

    private synchronized void reverse16bitStereoPCM(File originalFile, File reversedFile) {
        Log.d(TAG, "reverse16bitStereoPCM: ");
        try (RandomAccessFile raf = new RandomAccessFile(originalFile, "r");
             FileOutputStream fos = new FileOutputStream(reversedFile)) {
            int bufferSize = AudioRecord.getMinBufferSize(mConfig.getSampleRate(),
                    mConfig.getChannelConfig(), mConfig.getAudioFormat());
            int bytesRead;
            int totalBytesRead = 0;
            if ((long) bufferSize > raf.length()) {
                bufferSize = (int) raf.length();
            }
            long rafPosition = raf.length();
            byte[] buffer = new byte[bufferSize];
            do {
                rafPosition -= (long) bufferSize;
                try {
                    raf.seek(rafPosition);
                } catch (IOException ioe) {
                    buffer = new byte[Math.abs((int) rafPosition) + bufferSize];
                    rafPosition = 0;
                    raf.seek(rafPosition);
                }
                bytesRead = raf.read(buffer);
                totalBytesRead += bytesRead;
                reverseArray16BitStereoPcm(buffer);
                fos.write(buffer);
            } while (rafPosition != 0);
            Log.d(TAG, "reverse16bitStereoPCM: totalBytesRead:" + totalBytesRead);
            Log.d(TAG, "reverse16bitStereoPCM: " + mOriginalFile.getName() + " size in bytes: " + mOriginalFile.length());
            Log.d(TAG, "reverse16bitStereoPCM: " + mReversedFile.getName() + " size in bytes: " + mOriginalFile.length());
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "reverse16bitStereoPCM: ", fnfe);
        } catch (IOException ioe) {
            Log.e(TAG, "reverse16bitStereoPCM: ", ioe);
        }

        for(OnCompletionListener listener : mOnCompletionListenerArrayList)
            listener.onComplete();
        Log.d(TAG, "reverse16bitStereoPCM: end");
    }

    private void reverseArray16BitStereoPcm(byte[] array) {
        Log.d(TAG, "reverseArray16BitStereoPcm: ");
        if(array.length == 0)
            return;
        byte temp;
        int originalPos;
        int reversedPos;
        for (int i = 0; i < array.length / 2; i += 4) {
            for (int j = 0; j < 4; j++) {
                originalPos = i + j;
                reversedPos = array.length - i + j - 4;
                temp = array[originalPos];
                array[originalPos] = array[reversedPos];
                array[reversedPos] = temp;
            }
        }
    }

    public File getOriginalFile() {
        return mOriginalFile;
    }

    public void setOriginalFile(File originalFile) {
        mOriginalFile = originalFile;
    }

    public File getReversedFile() {
        return mReversedFile;
    }

    public void setReversedFile(File reversedFile) {
        mReversedFile = reversedFile;
    }

    public AudioConfig getConfig() {
        return mConfig;
    }

    public void setConfig(AudioConfig config) {
        mConfig = config;
    }

    public void removeOnCompletionListener(OnCompletionListener onCompletionListener) {
        mOnCompletionListenerArrayList.remove(onCompletionListener);
    }

    public void addOnCompletionListener(OnCompletionListener onCompletionListener) {
        mOnCompletionListenerArrayList.add(onCompletionListener);
    }

    public interface OnCompletionListener {
        void onComplete();
    }
}
