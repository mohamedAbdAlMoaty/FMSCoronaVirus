package com.example.fmscoronavirus.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.fmscoronavirus.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnPatient,btnAmbulanceProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPatient=findViewById(R.id.btnPatient);
        btnAmbulanceProvider=findViewById(R.id.btnAmbulanceProvider);

        btnPatient.setOnClickListener(this);
        btnAmbulanceProvider.setOnClickListener(this);





    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnAmbulanceProvider :
                Intent in=new Intent(MainActivity.this, AmbulanceProviderActivity.class);
                startActivity(in);
                finish();
                break;
            case R.id.btnPatient :
                Intent intent=new Intent(MainActivity.this,PatientActivity.class);
                startActivity(intent);
                finish();
                break;

        }
    }
}
