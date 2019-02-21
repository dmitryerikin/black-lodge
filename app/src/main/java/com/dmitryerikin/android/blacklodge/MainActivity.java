package com.dmitryerikin.android.blacklodge;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
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
    private AudioConfig mAudioRecorderConfig;
    private AudioConfig mAudioPlayerConfig;
    private AudioRecorder mAudioRecorder;
    private AudioPlayer mAudioPlayer;

    private ImageButton mRecordButton;
    private ImageButton mPlayButton;
    private ImageButton mReverseButton;
    private EditText mEditText;

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
            Log.e(TAG, "initFiles: IOException while creating a file", ioe);
        }


    }

    private void initAudioTools() {
        mAudioRecorderConfig = AudioRecorder.getDefaultAudioConfig();
        mAudioPlayerConfig = AudioPlayer.getDefaultAudioConfig();
        try {
            mAudioRecorder = new AudioRecorder(mAudioRecorderConfig, mAppDirectory.getPath() + "/" + ORIGINAL_AUDIO_FILENAME);
        } catch (AudioRecordException are) {
            showAppClosingDialog("Initialization error, app will be closed");
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        Log.d(TAG, "AudioRecorder state is " + String.valueOf(mAudioRecorder.getState()));
        try {
            mAudioPlayer = new AudioPlayer(mAudioPlayerConfig, mAppDirectory.getPath() + "/" + ORIGINAL_AUDIO_FILENAME);
        } catch (AudioRecordException are) {
            showAppClosingDialog("Initialization error, app will be closed");
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        Log.d(TAG, "AudioPlayer state is " + String.valueOf(mAudioPlayer.getState()));
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
                        mAudioRecorder.record();
                        mRecordButton.setImageResource(R.drawable.ic_button_stop);
                    } else if (mAudioRecorder.getState() == AudioRecord.RECORDSTATE_RECORDING) {
                        mAudioRecorder.stop();
                        mRecordButton.setImageResource(R.drawable.ic_button_record);
                    }
                }
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAudioRecorder.getState() != AudioRecord.RECORDSTATE_RECORDING) {
                    if (mAudioPlayer.getState() == AudioTrack.PLAYSTATE_STOPPED) {
                        mAudioPlayer.play();
                        mPlayButton.setImageResource(R.drawable.ic_button_stop);
                    } else if (mAudioPlayer.getState() == AudioTrack.PLAYSTATE_PLAYING) {
                        mAudioPlayer.stop();
                    }
                }
            }
        });

        mAudioPlayer.setOnPlayEndListener(new Runnable() {
            @Override
            public void run() {
                mPlayButton.setImageResource(R.drawable.ic_button_play);
            }
        });

        mReverseButton.setEnabled(false);
        mEditText.setEnabled(false);
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
