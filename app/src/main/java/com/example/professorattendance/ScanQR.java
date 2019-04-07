package com.example.professorattendance;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class ScanQR extends AppCompatActivity
{
//defining variables
    TextView courseCode;
    Button scan_btn;

    ImageView wifImg;
    TextView present_counter;
    TextView text_danger;
    TextView text;

    Button manual_attend_btn;
    Button delete_attend_btn;

    String user_id_cookie;
    String course_id_cookie;
    String type;
    String formattedDate;

    String no_of_students_present_today_cookie_name;
    String no_of_students_present_today_cookie;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    ServerSocket serverSocket;

    String messageFromClient;
    String feedForClient = "";
    String feedForHost = "";

    //to prevent going back from current window
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.askpassword, null);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = promptsView
                .findViewById(R.id.askPasswordText);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setNegativeButton("Go",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id)
                            {
                                String verify_password = (userInput.getText()).toString();

                                //getting the info of the logged user
                                sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                                String user_id_cookie = sharedPreferences.getString("user_id", "DNE");

                                String user_id = new Encryption().decrypt(user_id_cookie);

                                //checking if phone if connected to net or not
                                ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                                if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                                        connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)
                                {
                                    try
                                    {
                                        String type = "verify_password";
                                        String verify_password_results = new DatabaseActions().execute(type, user_id, verify_password).get();

                                        /** CHECK FOR USER'S INPUT **/
                                        if (verify_password_results.equals("1"))
                                        {
                                        //closing this page
                                            finish();
                                        }
                                        else
                                        {
                                            String message = "The password you have entered is incorrect." + " \n \n" + "Please try again!";
                                            AlertDialog.Builder builder = new AlertDialog.Builder(ScanQR.this);
                                            builder.setTitle("Error");
                                            builder.setMessage(message);
                                            builder.setPositiveButton("Ok", null);
                                            builder.create().show();
                                        }
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                else
                                {
                                    String message = "Internet Connection is not available.";
                                    AlertDialog.Builder builder = new AlertDialog.Builder(ScanQR.this);
                                    builder.setTitle("Error");
                                    builder.setMessage(message);
                                    builder.setPositiveButton("Ok", null);
                                    builder.create().show();
                                }
                            }
                        })
                .setPositiveButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.dismiss();
                            }
                        }
                );

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        courseCode = findViewById(R.id.courseCode);

        wifImg = findViewById(R.id.wifImg);
        present_counter = findViewById(R.id.present_counter);
        text_danger = findViewById(R.id.text_danger);
        text = findViewById(R.id.text);

        manual_attend_btn = findViewById(R.id.manual_attend_btn);
        delete_attend_btn = findViewById(R.id.delete_attend_btn);

    //to get the cookie values
        sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        user_id_cookie = sharedPreferences.getString("user_id", "DNE");
        String user_id = new Encryption().decrypt(user_id_cookie);

        String course_code_cookie = sharedPreferences.getString("course_code", "");
        course_id_cookie = sharedPreferences.getString("course_id", "DNE");

        courseCode.setText(course_code_cookie);

    //displaying the no of present students counter
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        formattedDate = dateFormat.format(date);

        no_of_students_present_today_cookie_name = formattedDate + "_date_" + course_id_cookie + "_course_present_counter";

        no_of_students_present_today_cookie = sharedPreferences.getString(no_of_students_present_today_cookie_name, "0");

        present_counter.setText("Present Students Counter: " + no_of_students_present_today_cookie);

    //getting the cookies of classes of courses for today
        final String today_is_class_of_courses = formattedDate + "__courses";
        final String todays_classes_course_ids_cookie = sharedPreferences.getString(today_is_class_of_courses, "DNE");

        if(todays_classes_course_ids_cookie.equals("DNE"))//no any class of any course has been added today
        {
        //asking prof if they want to mark today for class of this course
            new AlertDialog.Builder(ScanQR.this)
            .setTitle("Confirm Class")
            .setCancelable(false) //to prevent closing of alert dialog box on pressing back button
            .setMessage("Do you want to take attendance of today for this course")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    try
                    {
                        type = "add_today_as_class_of_course";
                        String add_today_as_class_of_courseResult = (new DatabaseActions().execute(type, course_id_cookie).get());

                        if(add_today_as_class_of_courseResult.equals("1"))
                        {
                        //creating cookie for today and adding this course in the todays_classes_course_ids_cookie
                            String todays_courses = course_id_cookie + "!";
                            editor.putString(today_is_class_of_courses, todays_courses);
                            editor.apply();

                        //closing this activity
                            finish();
                        }
                        else
                        {
                            text.setText("Something went wrong while adding today date as class of this course");
                        }
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    dialogInterface.dismiss();
                    finish();
                }
            }).create().show();
        }
        else//if class of some course has been added for today
        {
            if(!todays_classes_course_ids_cookie.contains(course_id_cookie + "!")) //if class of this course has not been added for today
            {
            //asking prof if they want to mark today for class of this course
                new AlertDialog.Builder(ScanQR.this)
                .setTitle("Confirm Class")
                .setCancelable(false) //to prevent closing of alertdialog box on pressing back button
                .setMessage("Do you want to take attendance of today for this course")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        try
                        {
                            type = "add_today_as_class_of_course";
                            String add_today_as_class_of_courseResult = (new DatabaseActions().execute(type, course_id_cookie).get());

                            if(add_today_as_class_of_courseResult.equals("1"))
                            {
                           //including this course in todays_classes_course_ids_cookie
                                String todays_courses = todays_classes_course_ids_cookie + course_id_cookie + "!";
                                editor.putString(today_is_class_of_courses, todays_courses);
                                editor.apply();

                            //closing this activity
                                finish();
                            }
                            else
                            {
                                text.setText("Something went wrong while adding today date as class of this course");
                            }
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        finish();
                        dialogInterface.dismiss();
                    }
                }).create().show();
            }
        }

    //checking if hotspot is on or not
        ApManager ap = new ApManager(this.getApplicationContext());

        Boolean hotspotStatus = ap.isApOn();
        if(hotspotStatus)//hotspot is ON
        {
            wifImg.setImageResource(R.drawable.wifi);

        //handling socket
            Thread socketServerThread = new Thread(new SocketServerThread());
            socketServerThread.start();
        }
        else//hotspot is OFF
        {
            text_danger.setText("Your mobile hotspot is OFF. Turn it ON and restart the App");
        }

    //on clicking on manual attendance btn
        manual_attend_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
            //redirecting to the manual attendance adding page
                Intent addManualAttendanceIntent = new Intent(ScanQR.this, AddManualAttendance.class);
                startActivity(addManualAttendanceIntent);
            }
        });

    //on clicking on delete attendance btn
        delete_attend_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //redirecting to the delete attendance page
                Intent deleteAttendanceIntent = new Intent(ScanQR.this, DeleteAttendance.class);
                startActivity(deleteAttendanceIntent);
            }
        });
    }

//inner class to send and receive message
    private class SocketServerThread extends Thread
    {
        final int SocketServerPORT = 3399;

        @Override
        public void run()
        {
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try
            {
                serverSocket = new ServerSocket(SocketServerPORT);

                ScanQR.this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(ScanQR.this, "Ready ", Toast.LENGTH_SHORT).show();
                    }
                });

                while (true)
                {
                //preparing socket for receiving and sending msg
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());

                //receiving data from client(student)
                    messageFromClient = dataInputStream.readUTF();

                    String scanned_result = new Encryption().decrypt(messageFromClient);

                    //checking if phone if connected to net or not
                    ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                    if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)
                    {
                        //device is connected to internet
                        try
                        {
                            JSONArray js = new JSONArray(scanned_result);

                            String code_student_id = js.getString(0);
                            String code_course_id = js.getString(1);
                            String code_timestamps = js.getString(2);

                            int size = js.length();
                            text.setText(Integer.toString(size));

                            if(!code_course_id.equals(course_id_cookie))//if scanned for wrong course
                            {
                                feedForClient = "You are trying to mark attendance for wrong Course";
                                feedForHost = "Student is trying to mark attendance for wrong Course";

                                //text_danger.setText("This QR Code is not associated with the selected course");
                            }
                            else
                            {
                                //getting the cookie of student present for this courses for today
                                final String today_students_present_for_this_course = formattedDate + "_date_" + code_course_id + "_course_present_students";
                                final String today_students_present_for_this_course_cookie = sharedPreferences.getString(today_students_present_for_this_course, "DNE");

                                if(today_students_present_for_this_course_cookie.equals("DNE")) //no any students are present for this course today
                                {
                                    //inserting this student attendance for this date for today in database
                                    type = "insert_student_attendance_for_course_and_date";

                                    String insert_student_attendance_for_course_and_dateResult = (new DatabaseActions().execute(type, code_student_id, code_course_id).get());
                                    if(insert_student_attendance_for_course_and_dateResult.equals("1"))
                                    {
                                        //creating cookie of students present today for this course and adding this student in that cookie
                                        String students_present = code_student_id + "!";
                                        editor.putString(today_students_present_for_this_course, students_present);
                                        editor.apply();

                                        //increasing the no of students counter
                                        String c = sharedPreferences.getString(no_of_students_present_today_cookie_name, "0");
                                        int new_c = Integer.parseInt(c) + 1;

                                        editor.putString(no_of_students_present_today_cookie_name, Integer.toString(new_c));
                                        editor.apply();

                                        present_counter.setText("Present Students Counter: " + new_c);

                                        feedForClient = "Your attendance successfully marked";
                                        feedForHost = "Attendance successfully marked";
                                    }
                                    else
                                    {
                                        feedForClient = "Something went wrong in database while marking your attendance";
                                        feedForHost = "Something went wrong in database while marking this student attendance";

                                        //text_danger.setText("Something went wrong while taking student attendance");
                                    }
                                }
                                else //some students are present for this course today
                                {
                                    if(!today_students_present_for_this_course_cookie.contains(code_student_id + "!")) //if this student attendance has not been taken for this course for today
                                    {
                                        //inserting this student attendance for this date for today in database
                                        type = "insert_student_attendance_for_course_and_date";

                                        String insert_student_attendance_for_course_and_dateResult = (new DatabaseActions().execute(type, code_student_id, code_course_id).get());
                                        if(insert_student_attendance_for_course_and_dateResult.equals("1"))
                                        {
                                            //including this student attendance in the cookie
                                            String students_present = today_students_present_for_this_course_cookie + code_student_id + "!";
                                            editor.putString(today_students_present_for_this_course, students_present);
                                            editor.apply();

                                            //increasing the no of students counter
                                            String c = sharedPreferences.getString(no_of_students_present_today_cookie_name, "0");
                                            int new_c = Integer.parseInt(c) + 1;

                                            editor.putString(no_of_students_present_today_cookie_name, Integer.toString(new_c));
                                            editor.apply();

                                            present_counter.setText("Present Students Counter: " + new_c);

                                            feedForClient = "Your attendance successfully marked";
                                            feedForHost = "Attendance successfully marked";
                                        }
                                        else
                                        {
                                            feedForClient = "Something went wrong in database while marking your attendance";
                                            feedForHost = "Something went wrong in database while marking this student attendance";

                                            //text_danger.setText("Something went wrong while taking student attendance");
                                        }
                                    }
                                    else
                                    {
                                        feedForClient = "Your today's Attendance has already been marked for this course";
                                        feedForHost = "Today's Attendance of this student has already been marked for this course";

                                        //text_danger.setText("Attendance of this student has already been taken today for this course");
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        feedForClient = "Internet connection is not available in the professor's phone";
                        feedForHost = "Internet connection is not available";

                        //text_danger.setText("Internet connection is not available");
                    }

                    ScanQR.this.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                        //showing processed msg to professor/host/server
                            if(feedForHost.equals("Attendance successfully marked"))
                            {
                                text.setText(feedForHost);
                            }
                            else
                            {
                                text_danger.setText(feedForHost);
                            }

                            final Handler someHandler = new Handler(getMainLooper());
                            someHandler.postDelayed(new Runnable() {
                                @Override
                                public void run()
                                {
                                    text_danger.setText("");
                                    text.setText("");
                                }
                            }, 3000);
                        }
                    });

                //sending msg/data to client(student)
                    dataOutputStream.writeUTF(feedForClient);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();

                final String errMsg = e.toString();
                ScanQR.this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        text_danger.setText(errMsg);
                    }
                });

            }
            finally
            {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (serverSocket != null)
        {
            try
            {
                serverSocket.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
