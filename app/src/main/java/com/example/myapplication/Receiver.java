package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

public class Receiver extends AppCompatActivity {

    WifiLevelReceiver receiver;
    TextView log;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        log = (TextView) findViewById(R.id.log);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        log.setText("Starting!\n");
        log.setMovementMethod(new ScrollingMovementMethod());

        // filters
        receiver = new WifiLevelReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(UploaderService.LOG_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(UploaderService.PROGRESS));

        Intent intent = getIntent();
        Uri data = intent.getData();
        String action = intent.getAction();

        ArrayList<Uri> imageUriList = null;
        if (Intent.ACTION_SEND.equals(action))
        {
            imageUriList = new ArrayList<>();
            Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            imageUriList.add(imageUri);
        }
        else if (Intent.ACTION_SEND_MULTIPLE.equals(action))
        {
            imageUriList = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }
        else
        {
            return;
        }

        Intent i = new Intent(this, UploaderService.class);
        i.putParcelableArrayListExtra("URIS", imageUriList);
        startService(i);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    class WifiLevelReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(UploaderService.LOG_MESSAGE))
            {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String str = intent.getStringExtra(UploaderService.DATA);
                        log.append(str);
                    }
                });
            }
            else if (intent.getAction().equals(UploaderService.PROGRESS))
            {
                runOnUiThread(new Runnable() {
                    public void run() {
                        int progress = intent.getIntExtra(UploaderService.DATA, 0);
                        progressBar.setProgress(progress);
                    }
                });
            }

        }
    }
}