package com.dmitryerikin.android.blacklodge;

import android.content.DialogInterface;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.dmitryerikin.android.blacklodge.Exceptions.AudioRecordException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String APP_DIRECTORY_NAME = "BlackLodge";
    private static final String ORIGINAL_AUDIO_FILENAME = "original_audio";
    private static final String REVERSED_AUDIO_FILENAME = "reversed_audio";

    private String mAbsoluteAppDirectoryPath;
    private File mAppDirectory;
    private File mOriginalAudioFile;
    private File mReversedAudioFile;
    private AudioConfig mAudioRecorderConfig;
    private AudioConfig mAudioPlayerConfig;
    private AudioRecorder mAudioRecorder;
    private AudioPlayer mAudioPlayer;
    private AudioReverser mAudioReverser;

    private ImageButton mRecordButton;
    private ImageButton mPlayButton;
    private ImageButton mReverseButton;
    private EditText mEditText;

    private boolean mReversed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initApp();
    }

    private void initApp() {
        initFiles();
        initAudioTools();
        initViews();
        setupAudioToolsListeners();
    }

    private void initFiles() {
        mAbsoluteAppDirectoryPath = Environment.getExternalStorageDirectory() + "/" + APP_DIRECTORY_NAME;
        mAppDirectory = new File(mAbsoluteAppDirectoryPath);
        if(!mAppDirectory.exists()) {
            mAppDirectory.mkdir();
        }
        mOriginalAudioFile = new File(mAppDirectory, ORIGINAL_AUDIO_FILENAME);
        try {
            mOriginalAudioFile.createNewFile();
        } catch (IOException ioe) {
            Log.e(TAG, "initFiles: IOException while creating " + ORIGINAL_AUDIO_FILENAME + " file", ioe);
        }
        mReversedAudioFile = new File(mAppDirectory, REVERSED_AUDIO_FILENAME);
        try {
            mOriginalAudioFile.createNewFile();
        } catch (IOException ioe) {
            Log.e(TAG, "initFiles: IOException while creating " + REVERSED_AUDIO_FILENAME + " file", ioe);
        }


    }

    private void initAudioTools() {
        mAudioRecorderConfig = AudioRecorder.getDefaultAudioConfig();
        mAudioPlayerConfig = AudioPlayer.getDefaultAudioConfig();
        try {
            mAudioRecorder = new AudioRecorder(mAudioRecorderConfig, mOriginalAudioFile);
        } catch (AudioRecordException are) {
            showAppClosingDialog("Initialization error, app will be closed");
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        if(mAudioRecorder != null)
            Log.d(TAG, "AudioRecorder state is " + String.valueOf(mAudioRecorder.getState()));
        try {
            mAudioPlayer = new AudioPlayer(mAudioPlayerConfig, mAppDirectory.getPath() + "/" + ORIGINAL_AUDIO_FILENAME);
        } catch (AudioRecordException are) {
            showAppClosingDialog("Initialization error, app will be closed");
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }

        if(mAudioPlayer != null)
            Log.d(TAG, "AudioPlayer state is " + String.valueOf(mAudioPlayer.getState()));

        mAudioReverser = new AudioReverser(mOriginalAudioFile, mReversedAudioFile,
                mAudioRecorderConfig);
    }

    private void initViews() {
        mRecordButton = findViewById(R.id.record_button);
        mPlayButton = findViewById(R.id.play_button);
        mReverseButton = findViewById(R.id.reverse_button);
        mEditText = findViewById(R.id.edit_text);

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAudioPlayer.getState() != AudioTrack.PLAYSTATE_PLAYING) {
                    if (mAudioRecorder.getState() == AudioRecord.RECORDSTATE_STOPPED) {
                        mRecordButton.setImageResource(R.drawable.ic_button_stop);
                        mPlayButton.setEnabled(!mPlayButton.isEnabled());
                        mReverseButton.setEnabled(!mReverseButton.isEnabled());
                        shortToast("Record has been started");
                        mAudioRecorder.record();
                    } else if (mAudioRecorder.getState() == AudioRecord.RECORDSTATE_RECORDING) {
                        mRecordButton.setImageResource(R.drawable.ic_button_record);
                        mPlayButton.setEnabled(!mPlayButton.isEnabled());
                        mReverseButton.setEnabled(!mReverseButton.isEnabled());
                        shortToast("Record has been stopped");
                        mAudioRecorder.stop();
                    }
                }
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAudioRecorder.getState() != AudioRecord.RECORDSTATE_RECORDING) {
                    if (mAudioPlayer.getState() == AudioTrack.PLAYSTATE_STOPPED) {
                        if(mReversed)
                            mAudioPlayer.setFile(mReversedAudioFile);
                        else
                            mAudioPlayer.setFile(mOriginalAudioFile);

                        shortToast("Playing has been started");
                        mAudioPlayer.play();
                        mPlayButton.setImageResource(R.drawable.ic_button_stop);
                        mRecordButton.setEnabled(!mRecordButton.isEnabled());
                        mReverseButton.setEnabled(!mReverseButton.isEnabled());
                    } else if (mAudioPlayer.getState() == AudioTrack.PLAYSTATE_PLAYING) {
                        mAudioPlayer.stop();
                    }
                }
            }
        });

        mReverseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioReverser.reverse();
                mReversed = !mReversed;
                mReverseButton.setEnabled(!mReverseButton.isEnabled());
                mRecordButton.setEnabled(!mRecordButton.isEnabled());
                mPlayButton.setEnabled(!mPlayButton.isEnabled());
                shortToast("Reversing has been started");
            }
        });

        mEditText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mEditText.setText(new StringBuilder(mEditText.getText().toString()).reverse());
                mEditText.setHint(new StringBuilder(mEditText.getHint().toString()).reverse());
                return true;
            }
        });
    }

    private void setupAudioToolsListeners() {
        mAudioPlayer.setOnPlayEndListener(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlayButton.setImageResource(R.drawable.ic_button_play);
                        mRecordButton.setEnabled(!mRecordButton.isEnabled());
                        mReverseButton.setEnabled(!mReverseButton.isEnabled());
                        shortToast("Playing has been stopped");
                    }
                });
            }
        });

        mAudioReverser.addOnCompletionListener(new AudioReverser.OnCompletionListener() {
            @Override
            public void onComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mReverseButton.setEnabled(!mReverseButton.isEnabled());
                        mRecordButton.setEnabled(!mRecordButton.isEnabled());
                        mPlayButton.setEnabled(!mPlayButton.isEnabled());
                        shortToast("Reversing has been ended");
                    }
                });
            }
        });
    }

    private void shortToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void showAppClosingDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioRecorder.destroy();
        mAudioPlayer.destroy();
    }
}
