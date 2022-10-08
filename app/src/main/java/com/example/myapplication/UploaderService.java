package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UploaderService extends Service {

    private static final String LOG = "log";
    private NotificationManager mNM;
    private LocalBroadcastManager localBroadcastManager;

    public static final String LOG_MESSAGE = "LOG_MESSAGE";
    public static final String PROGRESS = "PROGRESS";
    public static final String DATA = "DATA";

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.uploader_service_started;

    public static class UriEntry
    {
        public InputStream is;
        public String fileName;
        public Date lastModified;

        public UriEntry(InputStream is, String fileName, Date lastModified) {
            this.is = is;
            this.fileName = fileName;
            this.lastModified = lastModified;
        }
    }

    private List<UriEntry> uriEntryList = null;

    public String parseImageUriTofileName(Uri uri) {
        String selectedImagePath = null;

        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri,projection, null, null, null);
        if (cursor != null) {
            // Here you will get a null pointer if cursor is null
            // This can be if you used OI file manager for picking the media
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            selectedImagePath = cursor.getString(column_index);
        }
        return selectedImagePath;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        ArrayList<Uri> imageUriList = intent.getParcelableArrayListExtra("URIS");

        SharedPreferences sp1=this.getSharedPreferences("Credentials", MODE_PRIVATE);
        Uri target = Uri.parse(sp1.getString("uri", ""));
        String username = sp1.getString("username", "");
        String password = sp1.getString("password", "");

        uriEntryList = new ArrayList<>();
        for (Uri imageUri : imageUriList) {
            if (imageUri != null) {
                // Get resource path
                String file_uri = parseImageUriTofileName(imageUri);
                File source = new File(file_uri);
                try {
                    InputStream is = new FileInputStream(source);
                    uriEntryList.add(new UriEntry(is, source.getName(), new Date(source.lastModified())));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    log(e.getClass().getName() + "\n" + e.getMessage() + "\n");
                }
            }
        }

        HttpsTransfer.ProgressListener progress_listener = new HttpsTransfer.ProgressListener() {
            @Override
            public void httpsPublishProgress(int percentage) {
                Intent intent = new Intent(PROGRESS);
                intent.putExtra(DATA, percentage);
                localBroadcastManager.sendBroadcast(intent);
                //createNotificationChannel(startId, "Percentage: " + percentage);
            }
        };

        log("Files to upload: " + uriEntryList.size() + "\n");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                try {
                    for (UriEntry imageUri : uriEntryList) {

                        try {
                            log("uploading: " + imageUri.fileName + "\n");
                            HttpsTransfer.Result res = HttpsTransfer.Send(target, username, password, imageUri.fileName, imageUri.is, imageUri.lastModified, progress_listener);
                            log("res: " + res.success + " " + res.message + "\n");
                        } catch (ProtocolException e) {
                            log(e.getClass().getName() + "\n" + e.getMessage()+"\n");
                            e.printStackTrace();
                        } catch (MalformedURLException e) {
                            log(e.getClass().getName() + "\n" + e.getMessage()+"\n");
                            e.printStackTrace();
                        } catch (IOException e) {
                            log(e.getClass().getName() + " Internet Down?" + "\n");
                            e.printStackTrace();
                        }

                        try {
                            if (imageUri.is != null)
                                imageUri.is.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long elapsed_seconds = (System.currentTimeMillis() - startTime)/1000;

                long hours = elapsed_seconds / 3600;
                long minutes = (elapsed_seconds % 3600) / 60;
                long seconds = elapsed_seconds % 60;

                log(String.format("Elapsed: %02dh %02dm %02ds\n", hours, minutes, seconds));

                stopSelf(startId);
            }
        });
        thread.start();

        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        showNotification();

        log("service created\n");
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        log("service destroyed\n");
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.uploader_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Receiver.class), PendingIntent.FLAG_IMMUTABLE);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher_foreground)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.upoader_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    private void log(String msg)
    {
        Log.d(LOG, msg);
        Intent intent = new Intent(LOG_MESSAGE);
        intent.putExtra(DATA, msg);
        localBroadcastManager.sendBroadcast(intent);
    }

}
