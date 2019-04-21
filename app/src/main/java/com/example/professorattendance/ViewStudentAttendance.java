package com.example.professorattendance;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class ViewStudentAttendance extends AppCompatActivity
{
//defining variables
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    TextView courseCode;
    TextView text;

    Button exportBtn;

    String user_id_cookie;
    String course_id_cookie;

    String type;
    String names[];
    String rollNos[];
    String presentDays[];
    float no_of_classes;

    ListView studentAttendanceLV;

    private static final int REQUEST_PERMISSION_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_student_attendance);

        courseCode = findViewById(R.id.courseCode);
        text = findViewById(R.id.text);
        exportBtn = findViewById(R.id.exportBtn);
        studentAttendanceLV = findViewById(R.id.studentAttendanceLV);

        //getting values from cookie
        sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        user_id_cookie = sharedPreferences.getString("user_id", "DNE");
        final String course_code_cookie = sharedPreferences.getString("course_code", "");
        course_id_cookie = sharedPreferences.getString("course_id", "DNE");

        courseCode.setText(course_code_cookie);

    //checking if phone if connected to net or not
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)
        {
        //checking permission to write in external storage
            if(checkPermissionFromDevice())
            {
            //on clicking to export to csv button
                exportBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        type= "export_student_attendance_into_csv";
                        try
                        {
                            String export_student_attendance_into_csvResult = (new DatabaseActions().execute(type, course_id_cookie).get());

                            if(export_student_attendance_into_csvResult.equals("0"))
                            {
                                text.setText("Something went wrong while exporting csv");
                            }
                            else if(export_student_attendance_into_csvResult.equals("-1"))
                            {
                                text.setText("Database error");
                            }
                            else if(export_student_attendance_into_csvResult.equals("1")) //all is fine //file successfully exported in server
                            {
                                String downloadFromUrl = "http://mngo.in/qr_attendance/exports/" + course_id_cookie + ".csv";
                                String downloadedFileName = course_code_cookie + "_attendance_data.csv";

                            // Create request for android download manager
                                Uri uri= Uri.parse(downloadFromUrl);

                                DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                                DownloadManager.Request request = new DownloadManager.Request(uri);

                                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                                        DownloadManager.Request.NETWORK_MOBILE);

                            // set title and description
                                request.setTitle(downloadedFileName);
                                request.setDescription("Android Data download using DownloadManager.");

                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                            //set the local destination for download file to a path within the application's external files directory
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"downloadfileName");
                                request.setMimeType("*/*");
                                downloadManager.enqueue(request);
                            }
                            else
                            {
                                text.setText(export_student_attendance_into_csvResult);
                            }
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            else
            {
                requestPermission();

                exportBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(ViewStudentAttendance.this, "Please allow storage permission and try to export again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        //for showing the attendance details in the app
            try
            {
            //to get count of classes for a course
                type= "get_course_class_count";
                String get_course_class_count_result = (new DatabaseActions().execute(type, course_id_cookie).get());

                if(!get_course_class_count_result.equals("0") && !get_course_class_count_result.equals("-1") && !get_course_class_count_result.equals("Something went wrong"))
                {
                    no_of_classes = Float.parseFloat(get_course_class_count_result);
                }
                else
                {
                    no_of_classes = 1;
                }

            //to get attendance details for each students for a course
                type = "get_students_attendance_for_a_course";
                String get_students_attendance_for_a_courseResults = (new DatabaseActions().execute(type, course_id_cookie).get());

                if(!get_students_attendance_for_a_courseResults.equals("0") && !get_students_attendance_for_a_courseResults.equals("-1") && !get_students_attendance_for_a_courseResults.equals("Something went wrong"))
                {
                //parse JSON data
                    JSONArray ja = new JSONArray(get_students_attendance_for_a_courseResults);
                    JSONObject jo = null;

                    names = new String[ja.length()];
                    rollNos = new String[ja.length()];
                    presentDays = new String[ja.length()];

                    String temp_courses = "";
                    for (int i =0; i<ja.length(); i++)
                    {
                        jo = ja.getJSONObject(i);

                        String name = jo.getString("name");
                        String roll_no = jo.getString("roll_no");
                        String present_days_string = jo.getString("present_days");

                        float present_days = Float.parseFloat(present_days_string);
                        float percent = (present_days/no_of_classes)*100;

                        names[i] = name;
                        rollNos[i] = roll_no;
                        presentDays[i] = Double.toString( Math.round(percent * 10) / 10.0);
                    }

                //listing courses in listview
                    StudentAttendanceAdapter studentAttendanceAdapter = new StudentAttendanceAdapter();
                    studentAttendanceLV.setAdapter(studentAttendanceAdapter);

                //on clicking on list
                    studentAttendanceLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
                        {
                            Toast.makeText(ViewStudentAttendance.this, names[position], Toast.LENGTH_LONG).show();
                        }
                    });
                }
                else
                {
                    text.setText("Something went wrong in getting the student attendance details.");
                }
            }
            catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else
        {
            text.setText("Internet connection is not available");
        }
    }

//creating custom adapter to list each student attendance details
    class StudentAttendanceAdapter extends BaseAdapter
    {
        @Override
        public int getCount() {
            return names.length;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
        //rendering the layout
            view = getLayoutInflater().inflate(R.layout.student_attendance_adapter, null);

        //defining variables
            TextView student_roll = view.findViewById(R.id.student_roll);
            TextView student_name = view.findViewById(R.id.student_name);
            TextView stud_percent = view.findViewById(R.id.stud_percent);

        //setting the variables to a value
            student_roll.setText(names[i]);
            student_name.setText(rollNos[i]);
            stud_percent.setText(presentDays[i] + "%");

            return view;
        }
    }

//on requesting permission
    private void requestPermission()
    {
        ActivityCompat.requestPermissions(this, new String[]
        {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
        }, REQUEST_PERMISSION_CODE);
    }

//for asking for permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_PERMISSION_CODE:
            {
            //restarting app
                finish();

                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT);
                }
                else
                {
                    Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT);
                }
            }
            break;
        }
    }

//for checking permission
    private boolean checkPermissionFromDevice()
    {
        int write_external_storage_result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return (write_external_storage_result == PackageManager.PERMISSION_GRANTED);
    }
}
