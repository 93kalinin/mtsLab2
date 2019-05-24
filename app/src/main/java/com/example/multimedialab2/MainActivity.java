package com.example.multimedialab2;

import android.Manifest;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class MainActivity extends Activity {

    final String TAG = "myLogs";

    private final int BUFFER_SIZE = 960000;
    private final int SAMPLE_RATE = 8000;
    private final int SEED = 128;
    private final int ONE_MINUTE = 60 * 1000;
    private final String ENCRYPTED_FILE_NAME = "encrypted.pcm";
    private final String DECRYPTED_FILE_NAME = "decrypted.pcm";
    private EditText keyInput;
    private String FILE_PATH;
    private AudioRecord audioRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,},
                12345);
        keyInput = findViewById(R.id.key_input);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/";
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG,"AudioRecord init failed");
            return;
        }
    }

    public void recordAndSave(View v) {
        audioRecord.startRecording();
        new Handler().postDelayed(() -> {
            audioRecord.stop();
            byte[] myBuffer = new byte[BUFFER_SIZE];
            byte[] pseudorandomNoise = new byte[BUFFER_SIZE];
            byte[] result = new byte[BUFFER_SIZE];
            Random pseudorandom = new Random(Integer.parseInt(keyInput.getText().toString()));

            audioRecord.read(myBuffer, 0, BUFFER_SIZE);
            pseudorandom.nextBytes(pseudorandomNoise);
            for (int i = 0; i < BUFFER_SIZE; ++i)
                result[i] = (byte) (myBuffer[i] ^ pseudorandomNoise[i]);

            try {
                BufferedOutputStream outputStream = new BufferedOutputStream(
                        new FileOutputStream(FILE_PATH + ENCRYPTED_FILE_NAME));
                outputStream.write(result, 0, result.length);
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error saving recording ", e);
            }
        }, ONE_MINUTE);
    }

    public void decryptAndSave(View v) {
        byte[] bytesFromFile = new byte[BUFFER_SIZE];
        byte[] pseudorandomNoise = new byte[BUFFER_SIZE];
        byte[] result = new byte[BUFFER_SIZE];
        Random pseudorandom = new Random(Integer.parseInt(keyInput.getText().toString()));

        try {
            BufferedInputStream inputStream = new BufferedInputStream(
                    new FileInputStream(FILE_PATH + ENCRYPTED_FILE_NAME));
            BufferedOutputStream outputStream = new BufferedOutputStream(
                    new FileOutputStream(FILE_PATH + DECRYPTED_FILE_NAME));

            inputStream.read(bytesFromFile, 0, BUFFER_SIZE);
            pseudorandom.nextBytes(pseudorandomNoise);
            for (int i = 0; i < BUFFER_SIZE; ++i)
                result[i] = (byte) (bytesFromFile[i] ^ pseudorandomNoise[i]);
            outputStream.write(result, 0, result.length);
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error saving recording ", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioRecord.release();
    }
}
