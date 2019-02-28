package com.example.professorattendance;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class AddManualAttendance extends AppCompatActivity
{
    //defining variables
    SharedPreferences sharedPreferences;

    String user_id_cookie;
    String course_id_cookie;
    TextView courseCode;

    TextView text;
    TextView todaysDate;

    String formattedDate;

    String type;
    Spinner studentListSpinnner;
    ListView presentStudentLV;

    String stud_id_data[];
    String roll_data[];
    String roll_data1[];

    ArrayAdapter<String> adapter;
    ArrayAdapter<String> adapter1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_manual_attendance);

        text = findViewById(R.id.text);
        studentListSpinnner = findViewById(R.id.studentListSpinnner);
        presentStudentLV = findViewById(R.id.presentStudentLV);
        courseCode = findViewById(R.id.courseCode);
        todaysDate = findViewById(R.id.todaysDate);

    //getting the info of the logged user
        sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);

        user_id_cookie = sharedPreferences.getString("user_id", "DNE");
        final String user_id = new Encryption().decrypt(user_id_cookie);

        course_id_cookie = sharedPreferences.getString("course_id", "DNE");
        String course_code_cookie = sharedPreferences.getString("course_code", "");

        courseCode.setText(course_code_cookie);

    //showing today's date
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        formattedDate = dateFormat.format(date);
        todaysDate.setText(formattedDate);

        //checking if phone if connected to net or not
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)
        {
        //getting the list of all the students registered for that course
            try
            {
                type = "get_studs_of_a_course";
                String get_studs_of_a_courseResult = new DatabaseActions().execute(type, course_id_cookie).get();

                if(!get_studs_of_a_courseResult.equals("0") && !get_studs_of_a_courseResult.equals("-1") && !get_studs_of_a_courseResult.equals("Something went wrong"))
                {
                    //parse JSON data
                    JSONArray ja = new JSONArray(get_studs_of_a_courseResult);
                    JSONObject jo = null;

                    roll_data = new String[ja.length() + 1];
                    stud_id_data = new String[ja.length() + 1];

                    roll_data[0] = "";
                    stud_id_data[0] = "";

                    for(int i =0; i<ja.length(); i++)
                    {
                        jo = ja.getJSONObject(i);

                        String id = jo.getString("id");
                        String roll_no = jo.getString("roll_no");

                        roll_data[i + 1] = roll_no;
                        stud_id_data[i + 1] = id;
                    }

                    //showing the list of student registered in that course
                    adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, roll_data);
                    studentListSpinnner.setAdapter(adapter);

                    //on selecting any option in the drop down menu of the courses
                    studentListSpinnner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                    {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
                        {
                            final String selected_stud_id = (stud_id_data[position]).trim();

                            if(selected_stud_id !="" && selected_stud_id != "0")
                            {
                            //asking for password before adding that student attendance
                                LayoutInflater li = LayoutInflater.from(AddManualAttendance.this);
                                View promptsView = li.inflate(R.layout.askpassword, null);

                                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AddManualAttendance.this);
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
                                                                if (verify_password_results.equals("1")) //if password is correct
                                                                {
                                                                    //inserting this student attendance for this date for today in database
                                                                    type = "insert_student_attendance_for_course_and_date";

                                                                    String insert_student_attendance_for_course_and_dateResult = (new DatabaseActions().execute(type, selected_stud_id, course_id_cookie).get());
                                                                    if(insert_student_attendance_for_course_and_dateResult.equals("1"))
                                                                    {
                                                                        //reloading this activity
                                                                        finish();
                                                                        startActivity(getIntent());
                                                                    }
                                                                    else
                                                                    {
                                                                        text.setText("Something went wrong while taking student attendance");
                                                                    }
                                                                }
                                                                else
                                                                {
                                                                    String message = "The password you have entered is incorrect." + " \n \n" + "Please try again!";
                                                                    AlertDialog.Builder builder = new AlertDialog.Builder(AddManualAttendance.this);
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
                                                            AlertDialog.Builder builder = new AlertDialog.Builder(AddManualAttendance.this);
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
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView)
                        {
                            text.setText("hello");
                        }
                    });
                }
                else
                {
                    text.setText("Something went wrong while getting students list of this course");
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        //getting the list of all the students who are present today in that course
            DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
            String today = dateFormat1.format(date);

            type = "get_students_present_for_a_date_for_a_course";
            String get_students_present_for_a_date_for_a_courseResults = null;
            try
            {
                get_students_present_for_a_date_for_a_courseResults = (new DatabaseActions().execute(type, course_id_cookie, today).get());
                
                if(!get_students_present_for_a_date_for_a_courseResults.equals("0") && !get_students_present_for_a_date_for_a_courseResults.equals("-1") && !get_students_present_for_a_date_for_a_courseResults.equals("Something went wrong"))
                {
                    //parse JSON data
                    JSONArray ja = new JSONArray(get_students_present_for_a_date_for_a_courseResults);
                    JSONObject jo = null;

                    roll_data1 = new String[ja.length()];

                    for (int i =0; i<ja.length(); i++)
                    {
                        jo = ja.getJSONObject(i);

                        String roll_no = jo.getString("roll_no");

                        roll_data1[i] = roll_no;
                    }

                    //listing courses in listview
                    adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, roll_data1);
                    presentStudentLV    .setAdapter(adapter1);
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else
        {
            text.setText("Internet Connection is not available");
        }

    }
}
