package com.dmitryerikin.android.blacklodge;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import com.dmitryerikin.android.blacklodge.Exceptions.AudioRecordException;
import com.dmitryerikin.android.blacklodge.Utilities.ArrayConverter;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AudioRecorder {

    private static final String TAG = AudioRecorder.class.getSimpleName();

    private static final String MIN_BUFFER_SIZE_ERROR_MESSAGE = "implementation was unable to query the hardware for its input properties, or the minimum buffer size expressed in bytes";
    private static final String MIN_BUFFER_SIZE_ERROR_BAD_VALUE_MESSAGE = "recording parameters are not supported by the hardware, or an invalid parameter was passed";
    private static final String UNINITIALIZED_AUDIO_RECORD_MESSAGE = "AudioRecord has not been initialized";

    private Thread mThread;

    private AudioConfig mConfig;
    private AudioRecord mAudioRecord;

    private String mFilePath;
    private File mFile;

    private int mMinBufferSize;
    private int mBufferSize;
    private int mArrayBufferSize;
    private boolean mShouldContinue;

    public static AudioConfig getDefaultAudioConfig() {
        return new AudioConfig(
                MediaRecorder.AudioSource.DEFAULT,
                44100,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );
    }

    /**
     *
     * @param config - AudioConfig object
     * @param filePath - file path where to write
     * @throws AudioRecordException - if AudioRecorder has not been initialized
     * @throws FileNotFoundException - if @param config has not been found
     */

    public AudioRecorder(AudioConfig config, String filePath) throws AudioRecordException, FileNotFoundException {
        mConfig = config;
        mFilePath = filePath;
        mFile = new File(mFilePath);
        if (!mFile.exists())
            throw new FileNotFoundException();
        mMinBufferSize = AudioRecord.getMinBufferSize(mConfig.getSampleRate(),
                mConfig.getChannelConfig(), mConfig.getAudioFormat());

        if (mMinBufferSize == AudioRecord.ERROR || mMinBufferSize == AudioRecord.ERROR_BAD_VALUE)
            mMinBufferSize = mConfig.getSampleRate() * 2;

        mBufferSize = mMinBufferSize * 10;
        mArrayBufferSize = mMinBufferSize / 4;

        mAudioRecord = new AudioRecord(mConfig.getAudioSource(), mConfig.getSampleRate(),
                mConfig.getChannelConfig(), mConfig.getAudioFormat(), mBufferSize);
        if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED)
            throw new AudioRecordException(UNINITIALIZED_AUDIO_RECORD_MESSAGE);
    }

    public void initThread() {
        mThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                        if(mConfig.getAudioFormat() == AudioFormat.ENCODING_PCM_8BIT)
                            eightBitPcmRecording();
                        else if (mConfig.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT)
                            sixteenBitPcmRecording();
                        Log.d(TAG, "run: Thread is dead");
                    }
                }
        );
    }

    public void record() {
        mShouldContinue = true;
        initThread();
        mThread.start();
    }

    public void stop() {
        mShouldContinue = false;
        mThread = null;
    }

    public void destroy() {
        mAudioRecord.release();
        mAudioRecord = null;
    }

    public int getState() {
        return mAudioRecord.getRecordingState();
    }

    private void eightBitPcmRecording() {
        byte[] buffer = new byte[mMinBufferSize / 4];
        mAudioRecord.startRecording();
        Log.d(TAG, "eightBitPcmRecording: Record started");
        try (FileOutputStream fos = new FileOutputStream(mFile)) {
            while(mShouldContinue) {
                mAudioRecord.read(buffer, 0, buffer.length);
                fos.write(buffer);
            }
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "FileNotFoundException while create FileOutputStream");
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            Log.e(TAG, "IOException while creating FileOutputStream");
            ioe.printStackTrace();
        } finally {
            mAudioRecord.stop();
        }
    }

    private void sixteenBitPcmRecording() {
        short[] shortArray = new short[mArrayBufferSize / 4];
        ByteBuffer byteBuffer = ByteBuffer.allocate(shortArray.length * 2);
        try (FileOutputStream fos = new FileOutputStream(mFile)){
            mAudioRecord.startRecording();
            while (mShouldContinue) {
                mAudioRecord.read(shortArray, 0, shortArray.length);
                byteBuffer.position(0);
                for(short s : shortArray) {
                    byteBuffer.putShort(s);
                }
                fos.write(byteBuffer.order(ByteOrder.nativeOrder()).array());
            }
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "FileNotFoundException while create FileOutputStream");
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            Log.e(TAG, "IOException while creating FileOutputStream");
            ioe.printStackTrace();
        } finally {
            mAudioRecord.stop();
        }
    }
}
