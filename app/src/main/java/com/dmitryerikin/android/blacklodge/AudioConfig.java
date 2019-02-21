package com.dmitryerikin.android.blacklodge;

public class AudioConfig {

    private static final String TAG = AudioConfig.class.getSimpleName();

    private int mAudioSource;
    private int mSampleRate;
    private int mChannelConfig;
    private int mAudioFormat;

    public AudioConfig(int audioSource, int sampleRate, int channelConfig, int audioFormat) {
        mAudioSource = audioSource;
        mSampleRate = sampleRate;
        mChannelConfig = channelConfig;
        mAudioFormat = audioFormat;
    }

    public int getAudioSource() {
        return mAudioSource;
    }

    public void setAudioSource(int audioSource) {
        mAudioSource = audioSource;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
    }

    public int getChannelConfig() {
        return mChannelConfig;
    }

    public void setChannelConfig(int channelConfig) {
        mChannelConfig = channelConfig;
    }

    public int getAudioFormat() {
        return mAudioFormat;
    }

    public void setAudioFormat(int audioFormat) {
        mAudioFormat = audioFormat;
    }
}
