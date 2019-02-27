package com.example.professorattendance;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class CoursesFragment extends Fragment
{
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    TextView text;

    String data[];
    String course_ids[];
    String old_saved_courses[];
    String old_saved_courseIDs[];

    ListView courseListView;
    ArrayAdapter<String> adapter;

    String user_id;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_courses, null);

        text = view.findViewById(R.id.text);
        courseListView = view.findViewById(R.id.courseListView);

    //getting the info of the logged user
        sharedPreferences = this.getActivity().getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        String user_id_cookie = sharedPreferences.getString("user_id", "DNE");
        editor = sharedPreferences.edit();

        user_id = new Encryption().decrypt(user_id_cookie);

    //checking if phone if connected to net or not
        ConnectivityManager connMgr = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if(networkInfo != null && networkInfo.isConnected()) //phone is connected
        {
        //getting the course list of the user from database
            String type = "get_prof_courses";
            try
            {
                String get_user_courses_result = (new DatabaseActions().execute(type, user_id).get());

                if(!get_user_courses_result.equals("0") && !get_user_courses_result.equals("-1") && !get_user_courses_result.equals("Something went wrong"))
                {
                //parse JSON data
                    JSONArray ja = new JSONArray(get_user_courses_result);
                    JSONObject jo = null;

                    data = new String[ja.length()];
                    course_ids = new String[ja.length()];

                    String temp_courses = "";
                    String temp_course_ids = "";
                    for (int i =0; i<ja.length(); i++)
                    {
                        jo = ja.getJSONObject(i);

                        String course_code = jo.getString("course_code");
                        String course_id = jo.getString("id");

                        //String temp = course_code + " # " + course_id;
                        temp_courses += (course_code + ",");
                        temp_course_ids += (course_id + ",");

                        data[i] = course_code;
                        course_ids[i] = course_id;
                    }

                //listing courses in listview
                    adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, data);
                    courseListView.setAdapter(adapter);

                //creating cookie of the registered courses of that student
                    editor.putString("studentCourses", temp_courses);
                    editor.putString("studentCoursesIDs", temp_course_ids);
                    editor.apply();
                }
            } catch (ExecutionException e)
            {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e)
            {
                text.setText("Something went wrong while listing the student courses form database");
                e.printStackTrace();
            }
        }
        else //phone is not connected to internet
        {
            String studentCourses_cookie = sharedPreferences.getString("studentCourses", null);
            String studentCoursesIDs_cookie = sharedPreferences.getString("studentCoursesIDs", null);
            old_saved_courses = studentCourses_cookie.split(",");
            old_saved_courseIDs = studentCoursesIDs_cookie.split(",");

        //listing courses in listview
            adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, old_saved_courses);
            courseListView.setAdapter(adapter);
        }

    //on clicking on any list item
        courseListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                String course_id_cookie;
                String course_code_cookie;

                if(networkInfo != null && networkInfo.isConnected()) //phone is connected
                {
                    course_code_cookie = data[i];
                    course_id_cookie = course_ids[i];
                }
                else
                {
                    course_code_cookie = old_saved_courses[i];
                    course_id_cookie = old_saved_courseIDs[i];
                }

                editor.putString("course_id", course_id_cookie);
                editor.putString("course_code", course_code_cookie);
                editor.apply();

            //asking for password before going to that course scanner
                LayoutInflater li = LayoutInflater.from(getActivity());
                View promptsView = li.inflate(R.layout.askpassword, null);

                final EditText userInput = promptsView
                        .findViewById(R.id.askPasswordText);

                // set dialog message
                new AlertDialog.Builder(getActivity())
                        .setView(promptsView)
                        .setCancelable(false)
                        .setNegativeButton("Go",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id)
                                    {
                                        String verify_password = (userInput.getText()).toString();

                                        ConnectivityManager connMgr = (ConnectivityManager) getActivity()
                                                .getSystemService(Context.CONNECTIVITY_SERVICE);
                                        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                                        if(networkInfo != null && networkInfo.isConnected()) //phone is connected
                                        {
                                            try
                                            {
                                                String type = "verify_password";
                                                String verify_password_results = new DatabaseActions().execute(type, user_id, verify_password).get();

                                                /** CHECK FOR USER'S INPUT **/
                                                if (verify_password_results.equals("1"))
                                                {
                                                    //redirecting to the qr code generator page
                                                    Intent QRCodeGeneratorIntent = new Intent(getActivity().getApplicationContext(), ScanQR.class);
                                                    startActivity(QRCodeGeneratorIntent);
                                                }
                                                else
                                                {
                                                    String message = "The password you have entered is incorrect." + " \n \n" + "Please try again!";
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                        ).create().show();
            }
        });

        return view;
    }
}
