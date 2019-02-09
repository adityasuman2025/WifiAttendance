package com.example.professorattendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsFragment extends Fragment
{
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    ListView settingsLV;
    TextView text;
    TextView studentDetails;

    String data[] = {"Manage Courses", "Log Out"};
    ArrayAdapter<String> adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_settings, null);

        settingsLV = view.findViewById(R.id.settingsLV);
        text = view.findViewById(R.id.text);
        studentDetails = view.findViewById(R.id.studentDetails);

        text.setText("Settings");

        return view;
    }
}
