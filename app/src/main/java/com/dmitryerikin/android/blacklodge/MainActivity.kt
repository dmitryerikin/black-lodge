package com.dmitryerikin.android.blacklodge

import androidx.appcompat.app.AppCompatActivity
import com.dmitryerikin.android.blacklodge.AudioConfig
import com.dmitryerikin.android.blacklodge.AudioRecorder
import com.dmitryerikin.android.blacklodge.AudioReverser
import android.widget.ImageButton
import android.widget.EditText
import android.os.Bundle
import com.dmitryerikin.android.blacklodge.R
import com.dmitryerikin.android.blacklodge.MainActivity
import com.dmitryerikin.android.blacklodge.Exceptions.AudioRecordException
import android.media.AudioRecord
import android.view.View.OnLongClickListener
import android.widget.Toast
import android.content.DialogInterface
import android.media.AudioTrack
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {

    private var mAbsoluteAppDirectoryPath: String? = null
    private var mAppDirectory: File? = null
    private var mOriginalAudioFile: File? = null
    private var mReversedAudioFile: File? = null
    private var mAudioRecorderConfig: AudioConfig? = null
    private var mAudioPlayerConfig: AudioConfig? = null
    private var mAudioRecorder: AudioRecorder? = null
    private var mAudioPlayer: AudioPlayer? = null
    private var mAudioReverser: AudioReverser? = null
    private var mRecordButton: ImageButton? = null
    private var mPlayButton: ImageButton? = null
    private var mReverseButton: ImageButton? = null
    private var mEditText: EditText? = null
    private var mReversed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initApp()
    }

    private fun initApp() {
        initFiles()
        initAudioTools()
        initViews()
        setupAudioToolsListeners()
    }

    private fun initFiles() {
        mAbsoluteAppDirectoryPath =
            Environment.getExternalStorageDirectory().toString() + "/" + APP_DIRECTORY_NAME
        mAppDirectory = File(mAbsoluteAppDirectoryPath)
        if (!mAppDirectory!!.exists()) {
            mAppDirectory!!.mkdir()
        }
        mOriginalAudioFile = File(mAppDirectory, ORIGINAL_AUDIO_FILENAME)
        try {
            mOriginalAudioFile!!.createNewFile()
        } catch (ioe: IOException) {
            Log.e(
                TAG,
                "initFiles: IOException while creating " + ORIGINAL_AUDIO_FILENAME + " file",
                ioe
            )
        }
        mReversedAudioFile = File(mAppDirectory, REVERSED_AUDIO_FILENAME)
        try {
            mOriginalAudioFile!!.createNewFile()
        } catch (ioe: IOException) {
            Log.e(
                TAG,
                "initFiles: IOException while creating " + REVERSED_AUDIO_FILENAME + " file",
                ioe
            )
        }
    }

    private fun initAudioTools() {
        mAudioRecorderConfig = AudioRecorder.getDefaultAudioConfig()
        mAudioPlayerConfig = AudioPlayer.getDefaultAudioConfig()
        try {
            mAudioRecorder = AudioRecorder(mAudioRecorderConfig, mOriginalAudioFile)
        } catch (are: AudioRecordException) {
            showAppClosingDialog("Initialization error, app will be closed")
        } catch (fnfe: FileNotFoundException) {
            fnfe.printStackTrace()
        }
        Log.d(TAG, "AudioRecorder state is " + mAudioRecorder!!.state.toString())
        try {
            mAudioPlayer = AudioPlayer(
                mAudioPlayerConfig,
                mAppDirectory!!.path + "/" + ORIGINAL_AUDIO_FILENAME
            )
        } catch (are: AudioRecordException) {
            showAppClosingDialog("Initialization error, app will be closed")
        } catch (fnfe: FileNotFoundException) {
            fnfe.printStackTrace()
        }
        Log.d(TAG, "AudioPlayer state is " + mAudioPlayer!!.state.toString())
        mAudioReverser = AudioReverser(
            mOriginalAudioFile, mReversedAudioFile,
            mAudioRecorderConfig
        )
    }

    private fun initViews() {
        mRecordButton = findViewById(R.id.record_button)
        mPlayButton = findViewById(R.id.play_button)
        mReverseButton = findViewById(R.id.reverse_button)
        mEditText = findViewById(R.id.edit_text)
        mRecordButton.setOnClickListener(View.OnClickListener { v: View? ->
            if (mAudioPlayer!!.state != AudioTrack.PLAYSTATE_PLAYING) {
                if (mAudioRecorder!!.state == AudioRecord.RECORDSTATE_STOPPED) {
                    mRecordButton.setImageResource(R.drawable.ic_button_stop)
                    mPlayButton.setEnabled(!mPlayButton.isEnabled())
                    mReverseButton.setEnabled(!mReverseButton.isEnabled())
                    shortToast("Record has been started")
                    mAudioRecorder!!.record()
                } else if (mAudioRecorder!!.state == AudioRecord.RECORDSTATE_RECORDING) {
                    mRecordButton.setImageResource(R.drawable.ic_button_record)
                    mPlayButton.setEnabled(!mPlayButton.isEnabled())
                    mReverseButton.setEnabled(!mReverseButton.isEnabled())
                    shortToast("Record has been stopped")
                    mAudioRecorder!!.stop()
                }
            }
        })
        mPlayButton.setOnClickListener(View.OnClickListener { v: View? ->
            if (mAudioRecorder!!.state != AudioRecord.RECORDSTATE_RECORDING) {
                if (mAudioPlayer!!.state == AudioTrack.PLAYSTATE_STOPPED) {
                    if (mReversed) mAudioPlayer!!.setFile(mReversedAudioFile) else mAudioPlayer!!.setFile(
                        mOriginalAudioFile
                    )
                    shortToast("Playing has been started")
                    mAudioPlayer!!.play()
                    mPlayButton.setImageResource(R.drawable.ic_button_stop)
                    mRecordButton.setEnabled(!mRecordButton.isEnabled())
                    mReverseButton.setEnabled(!mReverseButton.isEnabled())
                } else if (mAudioPlayer!!.state == AudioTrack.PLAYSTATE_PLAYING) {
                    mAudioPlayer!!.stop()
                }
            }
        })
        mReverseButton.setOnClickListener(View.OnClickListener { v: View? ->
            mAudioReverser!!.reverse()
            mReversed = !mReversed
            mReverseButton.setEnabled(!mReverseButton.isEnabled())
            mRecordButton.setEnabled(!mRecordButton.isEnabled())
            mPlayButton.setEnabled(!mPlayButton.isEnabled())
            shortToast("Reversing has been started")
        })
        mEditText.setOnLongClickListener(OnLongClickListener { v: View? ->
            mEditText.setText(StringBuilder(mEditText.getText().toString()).reverse())
            mEditText.setHint(StringBuilder(mEditText.getHint().toString()).reverse())
            true
        })
    }

    private fun setupAudioToolsListeners() {
        mAudioPlayer!!.setOnPlayEndListener {
            runOnUiThread {
                mPlayButton!!.setImageResource(R.drawable.ic_button_play)
                mRecordButton!!.isEnabled = !mRecordButton!!.isEnabled
                mReverseButton!!.isEnabled = !mReverseButton!!.isEnabled
                shortToast("Playing has been stopped")
            }
        }
        mAudioReverser!!.addOnCompletionListener {
            runOnUiThread {
                mReverseButton!!.isEnabled = !mReverseButton!!.isEnabled
                mRecordButton!!.isEnabled = !mRecordButton!!.isEnabled
                mPlayButton!!.isEnabled = !mPlayButton!!.isEnabled
                shortToast("Reversing has been ended")
            }
        }
    }

    private fun shortToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun showAppClosingDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
            .setNeutralButton("Ok") { dialog: DialogInterface?, which: Int -> finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mAudioRecorder!!.destroy()
        mAudioPlayer!!.destroy()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val APP_DIRECTORY_NAME = "BlackLodge"
        private const val ORIGINAL_AUDIO_FILENAME = "original_audio"
        private const val REVERSED_AUDIO_FILENAME = "reversed_audio"
    }
}