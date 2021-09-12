package com.example.user.mobilerideshareapplicationmbs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.jar.Attributes;


public class Login_Page extends Fragment {

    private DatabaseReference databaseDriver;
    private DatabaseReference databaseRider;

    private Button loginButton;
    private EditText emailAddress;
    private EditText userPassword;
    private TextView textView;
    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login__page, container, false);

        Intent intent = getActivity().getIntent();
        String message = intent.getStringExtra("Extra Message");
        textView = view.findViewById(R.id.txt_display);
        textView.setText(message);

        progressDialog = new ProgressDialog(getActivity());

        firebaseAuth =FirebaseAuth.getInstance();

        databaseDriver = FirebaseDatabase.getInstance().getReference("Driver");
        databaseRider = FirebaseDatabase.getInstance().getReference("Rider");


        loginButton = view.findViewById(R.id.login_button);
        emailAddress = view.findViewById(R.id.txt_email);
        userPassword = view.findViewById(R.id.txt_password);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            openUserDashboard();

            }
        });

        return view;
    }


    private void openUserDashboard() {

        String userType =  textView.getText().toString().trim();

        if(userType.equals("Driver"))
        {
            loginDriver();
        }

        if (userType.equals("Rider"))
        {
            loginRider();
        }
    }

    private void loginRider() {

        final String email = emailAddress.getText().toString().trim();
        final String password = userPassword.getText().toString().trim();

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password))
        {

            progressDialog.setMessage("Login user.....");
            progressDialog.show();

            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    
                    if (!task.isSuccessful())
                    {
                        Toast.makeText(getActivity(), "Login Error", Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();

                    }
                    if(task.isSuccessful()){

                        checkRiderExist();
                    }
                }

            });
        }
        else
        {
            Toast.makeText(getActivity(), "Fill empty field", Toast.LENGTH_LONG).show();
        }
    }

    private void checkRiderExist() {
        final String user_ID = firebaseAuth.getCurrentUser().getUid();

        databaseRider.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(user_ID)){

                    progressDialog.dismiss();
                    Intent intent = new Intent(getActivity(), RiderDashboard.class);
                    startActivity(intent);
                    getActivity().finish();

                }
                else
                {
                    Toast.makeText(getActivity(), "You need to signup for creating an account", Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }



    private void loginDriver() {
        final String email = emailAddress.getText().toString().trim();
        final String password = userPassword.getText().toString().trim();

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password))
        {

            progressDialog.setMessage("Login user.....");
            progressDialog.show();

            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    if (!task.isSuccessful())
                    {
                        Toast.makeText(getActivity(), "Login Error", Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }
                    if (task.isSuccessful()){
                        checkDriverExist();
                    }
                }

            });
        }
        else
        {
            Toast.makeText(getActivity(), "Fill empty field", Toast.LENGTH_LONG).show();
        }
    }

    private void checkDriverExist() {

        final String user_ID = firebaseAuth.getCurrentUser().getUid();

        databaseDriver.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(user_ID)){

                    progressDialog.dismiss();
                    Intent intent = new Intent(getActivity(), DriverDashboard.class);
                    startActivity(intent);
                    getActivity().finish();

                }
                else
                {
                    Toast.makeText(getActivity(), "You need to signup for creating account", Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }
}
