package com.example.professorattendance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity
{

//defining variables
    Button login_btn;
    Button register_btn;
    EditText username_input;
    EditText password_input;
    TextView login_feed;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        login_btn = findViewById(R.id.login_btn);
        register_btn = findViewById(R.id.register_btn);
        username_input = findViewById(R.id.username_input);
        password_input = findViewById(R.id.password_input);
        login_feed = findViewById(R.id.login_feed);

    //checking if already loggedIn or not
        sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        String user_id_cookie = sharedPreferences.getString("user_id", "DNE");

        if(user_id_cookie.equals("DNE"))
        {
            //login_feed.setText("No one is logged in");
        }
        else //if someone is already logged in
        {
        //redirecting to the professor dashboard page
            Intent dashboardIntent = new Intent(MainActivity.this, Dashboard.class);
            startActivity(dashboardIntent);
            finish(); //used to delete the last activity history which we want to delete
        }

    //on clicking on login button
        login_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String username = username_input.getText().toString();
                String password = password_input.getText().toString();
                String type = "prof_verify_login";

                //checking if phone if connected to net or not
                ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                        connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)
                {
                    //trying to login the user
                    try
                    {
                        String login_result = new DatabaseActions().execute(type, username, password).get();

                        if(login_result.equals("-1"))
                        {
                            login_feed.setText("Database issue found");
                        }
                        else if (login_result.equals("Something went wrong"))
                        {
                            login_feed.setText(login_result);
                        }
                        else if(Integer.parseInt(login_result) > 0)
                        {
                            //creating cookie of the logged in user
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("username", new Encryption().encrypt(username));
                            editor.putString("user_id", new Encryption().encrypt(login_result));
                            editor.apply();

                            //redirecting the list course page
                            Intent dashboardIntent = new Intent(MainActivity.this, Dashboard.class);
                            startActivity(dashboardIntent);
                            finish(); //used to delete the last activity history which we want to delete
                        }
                        else
                        {
                            login_feed.setText("Your login credentials may be incorrect or this may be not your registered phone.");
                        }
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    login_feed.setText("Internet connection is not available");
                }
            }
        });

    //on clicking on register button
        register_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent registerIntent = new Intent(MainActivity.this, Register.class);
                startActivity(registerIntent);
                finish(); //used to delete the last activity history which we want to delete
            }
        });
    }
}
