package com.example.myapplication;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText uri = (EditText)findViewById(R.id.uri);
        EditText username = (EditText)findViewById(R.id.username);
        EditText password = (EditText)findViewById(R.id.password);

        SharedPreferences sp1=this.getSharedPreferences("Credentials", MODE_PRIVATE);
        uri.setText(sp1.getString("uri", ""));
        username.setText(sp1.getString("username", ""));
        password.setText(sp1.getString("password", ""));

        final Button button = findViewById(R.id.save);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String url = String.valueOf(uri.getText());
                if (URLUtil.isValidUrl(url))
                {
                    SharedPreferences sp = getSharedPreferences("Credentials", MODE_PRIVATE);
                    SharedPreferences.Editor Ed = sp.edit();
                    Ed.putString("uri", url);
                    Ed.putString("username", String.valueOf(username.getText()));
                    Ed.putString("password", String.valueOf(password.getText()));
                    Ed.commit();
                    Toast.makeText(getApplicationContext(),"saved",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"URL not valid",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}