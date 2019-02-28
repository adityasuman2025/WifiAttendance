package com.example.professorattendance;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class DeleteAttendance extends AppCompatActivity
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
    ListView presentStudentLV;

    String stud_attend_id_data[];
    String roll_data[];

    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_attendance);

        text = findViewById(R.id.text);

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

                    roll_data = new String[ja.length()];
                    stud_attend_id_data = new String[ja.length()];

                    for (int i =0; i<ja.length(); i++)
                    {
                        jo = ja.getJSONObject(i);

                        String roll_no = jo.getString("roll_no");
                        String id = jo.getString("id");

                        roll_data[i] = roll_no;
                        stud_attend_id_data[i] = id;
                    }

                    //listing courses in listview
                    adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, roll_data);
                    presentStudentLV.setAdapter(adapter);
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //on clicking on any list item under delete course
            presentStudentLV.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
                {
                    //getting the attend_id of selected item
                    final String stud_attend_to_delete = stud_attend_id_data[i];

                    //asking for password before adding that student attendance
                    LayoutInflater li = LayoutInflater.from(DeleteAttendance.this);
                    View promptsView = li.inflate(R.layout.askpassword, null);

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(DeleteAttendance.this);
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

                                                        type = "delete_stud_attend";
                                                        String delete_stud_attendResult = (new DatabaseActions().execute(type, stud_attend_to_delete).get());

                                                        if(delete_stud_attendResult.equals("1"))
                                                        {
                                                            //reloading this activity
                                                            finish();
                                                            startActivity(getIntent());
                                                        }
                                                        else
                                                        {
                                                            text.setText("Something went wrong while deleting the student's attendance");
                                                        }

                                                    }
                                                    else
                                                    {
                                                        String message = "The password you have entered is incorrect." + " \n \n" + "Please try again!";
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(DeleteAttendance.this);
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
                                                AlertDialog.Builder builder = new AlertDialog.Builder(DeleteAttendance.this);
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
            });
        }
        else
        {
            text.setText("Internet connection is not available");
        }
    }
}
