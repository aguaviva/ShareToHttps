package com.example.myapplication;

import android.app.ProgressDialog;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

public class HttpsTransfer {
    private static final String DEBUG = "DEBUG";

    /**
     * represents a tuple of success flag, and message as an R id
     * @author Michael Totschnig
     *
     */
    public static class Result {

        public int success;
        public String message;

        public Result(int success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    static public String Send(Uri target, String username, String password, String fileName, InputStream is, Date lastModified, ProgressListener progressListener) throws ProtocolException, MalformedURLException, IOException
    {
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary_key = "*****"+Long.toString(System.currentTimeMillis())+"*****";

        URL url = new URL(target.toString());
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "image/jpeg");

        String auth = username + ":" + password;
        final String basicAuth = "Basic " + Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", basicAuth);

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(1 * 60 * 1000);
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary_key);

        String header = "";
        header += twoHyphens + boundary_key + lineEnd;
        header += "Content-Disposition: form-data; name=\"lastModDate\"; ";
        header += lineEnd + lineEnd;
        header += lastModified + lineEnd;

        header += twoHyphens + boundary_key + lineEnd;
        header += "Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"" + fileName + "\"";
        header += lineEnd + lineEnd;

        String footer = lineEnd + twoHyphens + boundary_key + twoHyphens;

        int fileSize = is.available();

        conn.setFixedLengthStreamingMode(header.length() + fileSize + footer.length());

        try {
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());

            out.writeBytes(header);

            int maxBufferSize = 50 * 1024;
            int bufferSize = Math.min(fileSize, maxBufferSize);
            byte[] buffer = new byte[bufferSize];
            long startTime = System.currentTimeMillis();
            int bytesRead = is.read(buffer, 0, bufferSize);
            int totalRead = 0;
            int lastProgress = 0;
            while (bytesRead > 0) {
                totalRead += bytesRead;
                out.write(buffer, 0, bufferSize);
                int bytesAvailable = is.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = is.read(buffer, 0, bufferSize);

                int progress = (int) (100 * totalRead / fileSize);


                if (progress != lastProgress) {
                    lastProgress = progress;

                    // throttle progress if too fast
                    long currTime = System.currentTimeMillis();
                    if ((currTime-startTime)>16 || progress>99) {
                        startTime = currTime;
                        progressListener.httpsPublishProgress(progress);
                    }
                }
            }

            out.writeBytes(footer);
            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String response_str="";

        int response = conn.getResponseCode();
        if (response == 200) {
            InputStream input = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                //Log.d(DEBUG, line);
                response_str += line;
            }
            input.close();
        }

        conn.disconnect();

        return response_str;
    }

    public interface ProgressListener {
        public void httpsPublishProgress(int percentage);
    }
}



