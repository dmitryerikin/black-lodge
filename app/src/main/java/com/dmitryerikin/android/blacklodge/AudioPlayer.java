package com.dmitryerikin.android.blacklodge;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import com.dmitryerikin.android.blacklodge.Exceptions.AudioRecordException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AudioPlayer {

    private static final String TAG = AudioPlayer.class.getSimpleName();

    private static final String MIN_BUFFER_SIZE_ERROR_MESSAGE = "implementation was unable to query the hardware for its input properties, or the minimum buffer size expressed in bytes";
    private static final String MIN_BUFFER_SIZE_ERROR_BAD_VALUE_MESSAGE = "recording parameters are not supported by the hardware, or an invalid parameter was passed";
    private static final String UNINITIALIZED_AUDIO_TRACK_MESSAGE = "AudioRecord has not been initialized";

    private Thread mThread;

    private AudioConfig mConfig;
    private AudioTrack mAudioTrack;

    private String mFilePath;
    private File mFile;

    private int mMinBufferSize;
    private int mBufferSize;
    private int mArrayBufferSize;
    private boolean mShouldContinue;

    private Runnable mOnPlayEndListener;

    public static AudioConfig getDefaultAudioConfig() {
        return new AudioConfig(
                MediaRecorder.AudioSource.DEFAULT,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );
    }

    public AudioPlayer(AudioConfig config, String filePath) throws AudioRecordException, FileNotFoundException {
        mConfig = config;
        mFilePath = filePath;
        mFile = new File(mFilePath);
        if(!mFile.exists())
            throw new FileNotFoundException();
        mMinBufferSize = AudioTrack.getMinBufferSize(mConfig.getSampleRate(),
                mConfig.getChannelConfig(), mConfig.getAudioFormat());

        if(mMinBufferSize == AudioTrack.ERROR || mMinBufferSize == AudioTrack.ERROR_BAD_VALUE)
            mMinBufferSize = mConfig.getSampleRate() * 2;

        mBufferSize = mMinBufferSize;
        mArrayBufferSize = mBufferSize / 4;

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mConfig.getSampleRate(), mConfig.getChannelConfig(), mConfig.getAudioFormat(), mBufferSize, AudioTrack.MODE_STREAM);
        if (mAudioTrack.getState() == AudioRecord.STATE_UNINITIALIZED)
            throw new AudioRecordException(UNINITIALIZED_AUDIO_TRACK_MESSAGE);
    }

    public void initThread() {
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(mConfig.getAudioFormat() == AudioFormat.ENCODING_PCM_8BIT)
                    eightBitPcmPlaying();
                else if (mConfig.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT)
                    sixteenBitPcmPlaying();

                Log.d(TAG, "run: Thread is dead");
            }
        });
    }

    public void play() {
        mShouldContinue = true;
        initThread();
        mThread.start();
    }

    public void stop() {
        mShouldContinue = false;
        mThread = null;
    }

    public void destroy() {
        mAudioTrack.release();
        mAudioTrack = null;
    }

    public int getState() {
        return mAudioTrack.getPlayState();
    }

    public void setFile(File file) {
        mFile = file;
    }

    public void setOnPlayEndListener(Runnable onPlayEndListener) {
        mOnPlayEndListener = onPlayEndListener;
    }

    private void eightBitPcmPlaying() {
        Log.d(TAG, "eightBitPcmPlaying: ");
        byte[] buffer = new byte[mArrayBufferSize];
        int bytesRead;
        long totalBytesRead = 0L;
        mAudioTrack.play();
        try(FileInputStream fis = new FileInputStream(mFile)) {
            while (mShouldContinue) {
                if((bytesRead = fis.read(buffer)) == -1)
                    break;
                totalBytesRead += (long) bytesRead;
                mAudioTrack.write(buffer, 0, buffer.length);
            }
            Log.d(TAG, "eightBitPcmPlaying: total bytes read: " + totalBytesRead);
            Log.d(TAG, "eightBitPcmPlaying: file size in bytes: " + mFile.length());
            if(mOnPlayEndListener != null)
                mOnPlayEndListener.run();
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "run: FileNotFountException while creating FileInputStream", fnfe);
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            Log.e(TAG, "run: IOException while creating FileInputStream", ioe);
            ioe.printStackTrace();
        } finally {
            mAudioTrack.stop();
        }
    }

    private void sixteenBitPcmPlaying() {
        Log.d(TAG, "sixteenBitPcmPlaying: ");
        byte[] byteArray = new byte[mArrayBufferSize];
        short[] shortArray = new short[byteArray.length / 2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        int bytesRead;
        long totalBytesRead = 0L;
        mAudioTrack.play();
        try(FileInputStream fis = new FileInputStream(mFile)) {
            while (mShouldContinue) {

                if((bytesRead = fis.read(byteArray)) == -1)
                    break;
                totalBytesRead += (long) bytesRead;
                ((ShortBuffer) (byteBuffer.order(ByteOrder.nativeOrder()).asShortBuffer().position(0))).get(shortArray);
                mAudioTrack.write(shortArray, 0, bytesRead / 2);
            }
            Log.d(TAG, "sixteenBitPcmPlaying: total bytes read: " + totalBytesRead + " = " + totalBytesRead / 4 + " frames ");
            Log.d(TAG, "sixteenBitPcmPlaying: file length in bytes: " + mFile.length());
            if(mOnPlayEndListener != null)
                mOnPlayEndListener.run();
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "run: FileNotFountException while creating FileInputStream", fnfe);
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            Log.e(TAG, "run: IOException while creating FileInputStream", ioe);
            ioe.printStackTrace();
        } finally {
            mAudioTrack.stop();
        }
    }
}
