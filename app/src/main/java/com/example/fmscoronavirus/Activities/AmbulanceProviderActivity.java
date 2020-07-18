package com.example.fmscoronavirus.Activities;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.fmscoronavirus.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AmbulanceProviderActivity extends AppCompatActivity implements View.OnClickListener {

    ConstraintLayout rootLayout;
    Button signup,login;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance_provider);

        signup=findViewById(R.id.signupProvider);
        login=findViewById(R.id.loginProvider);
        rootLayout=findViewById(R.id.rootLayout);

        signup.setOnClickListener(this);
        login.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() !=null) {
            goToAmbProviderDetailsActivity();
        }


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.signupProvider :
                showRegisterDialog();
                break;
            case R.id.loginProvider :
                showLoginDialog();
                break;
        }
    }

    private void showLoginDialog() {
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("SIGN IN");
        dialog.setMessage("please use email to sign in");
        final View view = getLayoutInflater().inflate(R.layout.layout_login, null);

        final EditText editemail=view.findViewById(R.id.editemail);
        final EditText editpassword=view.findViewById(R.id.editpassword);

        dialog.setView(view);
        dialog.setPositiveButton("SIGN IN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                //check fields if its empty
                if (TextUtils.isEmpty(editemail.getText().toString())) {
                    Snackbar.make(rootLayout, "please enter email address", Snackbar.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(editpassword.getText().toString())) {
                    Snackbar.make(rootLayout, "please enter password", Snackbar.LENGTH_SHORT).show();
                }
                else {

                    final ProgressDialog mProgress = new ProgressDialog(AmbulanceProviderActivity.this);
                    mProgress.setMessage("Please Wait...");
                    mProgress.show();

                    //login in firebase
                    mAuth.signInWithEmailAndPassword(editemail.getText().toString(), editpassword.getText().toString())
                            .addOnCompleteListener(AmbulanceProviderActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                mProgress.dismiss();
                                Snackbar.make(rootLayout, "login error", Snackbar.LENGTH_SHORT).show();
                            } else {
                                mProgress.dismiss();
                                goToAmbProviderDetailsActivity();
                            }
                        }
                    });
                }
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.show();
    }


    private void showRegisterDialog() {
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("REGISTER");
        dialog.setMessage("please use email to register");
        final View view = getLayoutInflater().inflate(R.layout.layout_register, null);

        final EditText editemail=view.findViewById(R.id.editemail);
        final EditText editpassword=view.findViewById(R.id.editpassword);
        final EditText editname=view.findViewById(R.id.editname);
        final EditText editphone=view.findViewById(R.id.editphone);

        dialog.setView(view);
        dialog.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                //check fields if its empty
                if(TextUtils.isEmpty(editemail.getText().toString())){
                    Snackbar.make(rootLayout,"please enter email address",Snackbar.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(editpassword.getText().toString())){
                    Snackbar.make(rootLayout,"please enter password",Snackbar.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(editname.getText().toString())){
                    Snackbar.make(rootLayout,"please enter name",Snackbar.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(editphone.getText().toString())){
                    Snackbar.make(rootLayout,"please enter phone",Snackbar.LENGTH_SHORT).show();
                }
                else {

                    final ProgressDialog mProgress = new ProgressDialog(AmbulanceProviderActivity.this);
                    mProgress.setMessage("Please Wait...");
                    mProgress.show();
                    mProgress.dismiss();

                    //register in firebase
                    mAuth.createUserWithEmailAndPassword(editemail.getText().toString(), editpassword.getText().toString())
                            .addOnCompleteListener(AmbulanceProviderActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (!task.isSuccessful()) {
                                        Snackbar.make(rootLayout, "sign up error", Snackbar.LENGTH_SHORT).show();
                                    } else {

                                        //save user in database
                                        String user_id = mAuth.getCurrentUser().getUid();
                                        DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference("users").child("providers").child(user_id);
                                        current_user_db.setValue(true);

                                        Map userInfo =new HashMap();
                                        userInfo.put("email",editemail.getText().toString());
                                        userInfo.put("name",editname.getText().toString());
                                        userInfo.put("password",editpassword.getText().toString());
                                        userInfo.put("phone",editphone.getText().toString());
                                        current_user_db.updateChildren(userInfo);

                                        mProgress.dismiss();
                                        goToAmbProviderDetailsActivity();
                                        Snackbar.make(rootLayout, "Register Successfully", Snackbar.LENGTH_SHORT).show();

                                    }
                                }
                            });

                }
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialog.show();
    }


    private void goToAmbProviderDetailsActivity(){
        Intent i = new Intent(AmbulanceProviderActivity.this, AmbProviderDetailsActivity.class);
        startActivity(i);
        finish();
    }


}
