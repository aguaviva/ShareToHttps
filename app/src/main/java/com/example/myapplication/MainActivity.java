package com.example.myapplication;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Base64;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    EditText uri;
    EditText username;
    EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uri = (EditText)findViewById(R.id.uri);
        username = (EditText)findViewById(R.id.username);
        password = (EditText)findViewById(R.id.password);

        load_credentials();

        final Button button = findViewById(R.id.save);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                button.setEnabled(false);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        URL url = null;
                        try {
                            url = new URL(String.valueOf(uri.getText()));
                            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                            conn.setConnectTimeout(5000);
                            String auth = String.valueOf(username.getText()) + ":" + String.valueOf(password.getText());
                            final String basicAuth = "Basic " + Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
                            conn.setRequestProperty("Authorization", basicAuth);
                            int response_code = conn.getResponseCode();
                            if (response_code == HttpsURLConnection.HTTP_OK) {
                                save_credentials();
                            } else if (response_code == 401) {
                                show_alert("Bad username/password");
                            } else {
                                show_alert("Server returned code: " + response_code);
                            }
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            show_alert("Malformed URL");
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                            show_alert("Can't connect");
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                button.setEnabled(true);
                            }
                        });
                    }
                });
                thread.start();
            }
        });
    }

    void show_alert(String error)
    {
        runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(error + "\n\n Do you still want to save the form data?")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                save_credentials();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.create().show();
            }
        });
    }

    protected void save_credentials()
    {
        SharedPreferences sp = getSharedPreferences("Credentials", MODE_PRIVATE);
        SharedPreferences.Editor Ed = sp.edit();
        Ed.putString("uri", String.valueOf(uri.getText()));
        Ed.putString("username", String.valueOf(username.getText()));
        Ed.putString("password", String.valueOf(password.getText()));
        Ed.commit();

        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Saved!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void load_credentials()
    {
        SharedPreferences sp1 = this.getSharedPreferences("Credentials", MODE_PRIVATE);
        uri.setText(sp1.getString("uri", "https://"));
        username.setText(sp1.getString("username", ""));
        password.setText(sp1.getString("password", ""));
    }
}