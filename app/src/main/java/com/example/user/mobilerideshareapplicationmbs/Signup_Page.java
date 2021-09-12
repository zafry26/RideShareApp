package com.example.user.mobilerideshareapplicationmbs;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.net.FileNameMap;
import java.net.PasswordAuthentication;
import java.time.Year;
import java.util.Calendar;


public class Signup_Page extends Fragment  {
    Spinner spinner ;
    String nameList[] = {"Male","Female"};

    private DatabaseReference databaseDriver;
    private DatabaseReference databaseRider;

    private FirebaseAuth firebaseAuth;

    private TextView textview;
    private Button signupButton;
    private EditText emailAddress;
    private EditText userPassword;
    private EditText userName;
    private Spinner userGender;
    private EditText todayDate;
    private EditText userAge;
    private EditText userContact;

    private ProgressDialog progressDialog;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_signup__page, container, false);

        userGender = (Spinner) view.findViewById(R.id.spn_gender);
        ArrayAdapter adapter = new ArrayAdapter(getActivity(), R.layout.spinner_gender, nameList);
        userGender.setAdapter(adapter);


        Intent intent = getActivity().getIntent();
        String message = intent.getStringExtra("Extra Message");
        textview = view.findViewById(R.id.txt_display);
        textview.setText(message);


        progressDialog = new ProgressDialog(getActivity());

        firebaseAuth = FirebaseAuth.getInstance();

        databaseDriver = FirebaseDatabase.getInstance().getReference("Driver");
        databaseRider = FirebaseDatabase.getInstance().getReference("Rider");


        signupButton = view.findViewById(R.id.signup_button);
        emailAddress =  view.findViewById(R.id.txt_email);
        userPassword = view.findViewById(R.id.txt_password);
        userName = view.findViewById(R.id.txt_name);
        todayDate = view.findViewById(R.id.txt_date);
        userAge = view.findViewById(R.id.txt_age);
        userContact = view.findViewById(R.id.txt_contact);



        todayDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showDatePicker();
            }
        });


        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               openUserDashboard();
            }
        });

        return view;

    }

    private void showDatePicker() {
        DatePickerFragment date = new DatePickerFragment();

        Calendar calender = Calendar.getInstance();
        Bundle args = new Bundle();
        args.putInt("year", calender.get(Calendar.YEAR));
        args.putInt("month", calender.get(Calendar.MONTH));
        args.putInt("day", calender.get(Calendar.DAY_OF_MONTH));
        date.setArguments(args);

        date.setCallBack(ondate);
        date.show(getFragmentManager(),"Date Picker");

    }

    DatePickerDialog.OnDateSetListener ondate = new DatePickerDialog.OnDateSetListener() {


        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

            todayDate.setText(String.valueOf(dayOfMonth) + "-" + String.valueOf(monthOfYear+1) + "-" + String.valueOf(year));

            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - year;

            if (today.get(Calendar.DAY_OF_YEAR) < year){
                age--;
            }

            Integer ageInt = new Integer(age);
            String ageS = ageInt.toString();

            userAge.setText(ageS + " years old");

        }
    };

    private void openUserDashboard() {

        String userType = textview.getText().toString().trim();
        if(userType.equals("Driver"))
            {
                signupDriver();
            }

            if (userType.equals("Rider"))
            {
                signupRider();
            }
        }


    public void signupDriver(){

        final String userType = textview.getText().toString().trim();
        final String email = emailAddress.getText().toString().trim();
        final String password = userPassword.getText().toString().trim();
        final String name = userName.getText().toString().trim();
        final String gender = userGender.getSelectedItem().toString().trim();
        final String date = todayDate.getText().toString().trim();
        final String age = userAge.getText().toString().trim();
        final String contact = userContact.getText().toString().trim();


        if (userType.equals("Driver") && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(name) && !TextUtils.isEmpty(gender) && !TextUtils.isEmpty(date) && !TextUtils.isEmpty(age) && !TextUtils.isEmpty(contact))
            {

                progressDialog.setMessage("Registering user.....");
                progressDialog.show();

                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){

                            String user_ID = firebaseAuth.getCurrentUser().getUid();
                            DatabaseReference my_current_db = databaseDriver.child(user_ID);

                            my_current_db.child("User Type").setValue(userType);
                            my_current_db.child("Email Address").setValue(email);
                            my_current_db.child("Password").setValue(password);
                            my_current_db.child("User Name").setValue(name);
                            my_current_db.child("Gender").setValue(gender);
                            my_current_db.child("Date").setValue(date);
                            my_current_db.child("Age").setValue(age);
                            my_current_db.child("Contact Number").setValue(contact);

                            Toast.makeText(getActivity(), "Successfully Registered", Toast.LENGTH_LONG).show();
                            progressDialog.dismiss();
                            Intent intent = new Intent(getActivity(), DriverDashboard.class);
                            startActivity(intent);
                            getActivity().finish();

                        }
                        if(!task.isSuccessful()){
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), "Could not signup.Please try again later", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
            else
            {
                Toast.makeText(getActivity(), "Fill empty field", Toast.LENGTH_LONG).show();
            }
    }



    public void signupRider(){

        final String userType = textview.getText().toString().trim();
        final String email = emailAddress.getText().toString().trim();
        final String password = userPassword.getText().toString().trim();
        final String name = userName.getText().toString().trim();
        final String gender = userGender.getSelectedItem().toString().trim();
        final String date = todayDate.getText().toString().trim();
        final String age = userAge.getText().toString().trim();
        final String contact = userContact.getText().toString().trim();


            if (userType.equals("Rider") && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(name) && !TextUtils.isEmpty(gender) && !TextUtils.isEmpty(date) && !TextUtils.isEmpty(age) && !TextUtils.isEmpty(contact))
            {

                progressDialog.setMessage("Registering user.....");
                progressDialog.show();

                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){

                            String user_ID = firebaseAuth.getCurrentUser().getUid();
                            DatabaseReference my_current_db = databaseRider.child(user_ID);

                            my_current_db.child("User Type").setValue(userType);
                            my_current_db.child("Email Address").setValue(email);
                            my_current_db.child("Password").setValue(password);
                            my_current_db.child("User Name").setValue(name);
                            my_current_db.child("Gender").setValue(gender);
                            my_current_db.child("Date").setValue(date);
                            my_current_db.child("Age").setValue(age);
                            my_current_db.child("Contact Number").setValue(contact);

                            Toast.makeText(getActivity(), "Successfully Registered", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            Intent intent = new Intent(getActivity(), RiderDashboard.class);
                            startActivity(intent);
                            getActivity().finish();
                        }
                        if(!task.isSuccessful()){
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), "Could not signup.Please try again later", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
            else
            {
                Toast.makeText(getActivity(), "Fill empty field", Toast.LENGTH_LONG).show();
            }
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
