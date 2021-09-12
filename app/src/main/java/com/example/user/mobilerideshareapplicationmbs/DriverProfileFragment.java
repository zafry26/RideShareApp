package com.example.user.mobilerideshareapplicationmbs;


import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;


public class DriverProfileFragment extends Fragment implements View.OnClickListener {

    String nameList[] = {"Male","Female"};


    private ImageView driverProfileImage;
    private Uri resultUri;

    private DatabaseReference databaseDriver;

    private String user_id;
    private String profileImageUrl;

    private String driverName;
    private String driverPhone;
    private String driverGender;
    private String driverBirthday;
    private String driverAge;

    private EditText driverNameField;
    private EditText driverNumberField;
    private Spinner driverGenderField;
    private TextView driverBirthdayField;
    private TextView driverAgeField;

    private Button updateDriverInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_driver_profile, container, false);

        driverGenderField = (Spinner) view.findViewById(R.id.driver_gender);
        ArrayAdapter adapter = new ArrayAdapter(getActivity(), R.layout.spinner_gender, nameList);
        driverGenderField.setAdapter(adapter);


        driverProfileImage = view.findViewById(R.id.driver_profile_image);
        driverNameField = view.findViewById(R.id.driver_name);
        driverBirthdayField = view.findViewById(R.id.driver_birthday);
        driverAgeField = view.findViewById(R.id.driver_age);
        driverNumberField= view.findViewById(R.id.driver_number);
        updateDriverInfo = view.findViewById(R.id.update_driver_info);

        driverProfileImage.setOnClickListener(this);
        updateDriverInfo.setOnClickListener(this);
        driverBirthdayField.setOnClickListener(this );


        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseDriver = FirebaseDatabase.getInstance().getReference("Driver").child(user_id);

        getUserInfo();



        return view;
    }


    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.driver_profile_image:
                CropImage.activity()
                        .setAspectRatio(1,1)
                        .setCropShape(CropImageView.CropShape.OVAL)
                        .start(getContext(), this);
                break;

            case R.id.update_driver_info:
                storeUserInformation();
                break;

            case R.id.driver_birthday:
                showDatePicker();
                break;
        }
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

            driverBirthdayField.setText(String.valueOf(dayOfMonth) + "-" + String.valueOf(monthOfYear + 1) + "-" + String.valueOf(year));

            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - year;

            if (today.get(Calendar.DAY_OF_YEAR) < year) {
                age--;
            }

            Integer ageInt = new Integer(age);
            String ageS = ageInt.toString();

            driverAgeField.setText(ageS + " years old");
        }
    };



    public void getUserInfo(){

        databaseDriver.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("User Name") != null) {
                        driverName = map.get("User Name").toString();
                        driverNameField.setText(driverName);
                    }

                    if (map.get("Contact Number") != null) {
                        driverPhone = map.get("Contact Number").toString();
                        driverNumberField.setText(driverPhone);
                    }

                    if (map.get("Gender") != null) {
                        driverGender = map.get("Gender").toString();
                        if (driverGender.equals("Male")) {
                            driverGenderField.setSelection(0);
                        }
                        else{
                            driverGenderField.setSelection(1);
                        }
                    }

                    if (map.get("Date") != null) {
                        driverBirthday = map.get("Date").toString();
                        driverBirthdayField.setText(driverBirthday);
                    }

                    if (map.get("Age") != null) {
                        driverAge = map.get("Age").toString();
                        driverAgeField.setText(driverAge);
                    }
                    if (map.get("Profile Images Url") != null) {
                        profileImageUrl = map.get("Profile Images Url").toString();
                        Glide.with(getActivity().getApplication()).load(profileImageUrl).into(driverProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    public void storeUserInformation(){

        driverName = driverNameField.getText().toString();
        driverPhone = driverNumberField.getText().toString();
        driverGender = driverGenderField.getSelectedItem().toString();
        driverBirthday = driverBirthdayField.getText().toString();
        driverAge = driverAgeField.getText().toString();

        if (!TextUtils.isEmpty(driverName) && !TextUtils.isEmpty(driverPhone)&&!TextUtils.isEmpty(driverGender) &&!TextUtils.isEmpty(driverBirthday) && !TextUtils.isEmpty(driverAge)) {

            Map userInfo = new HashMap();
            userInfo.put("User Name", driverName);
            userInfo.put("Contact Number", driverPhone);
            userInfo.put("Gender", driverGender);
            userInfo.put("Date", driverBirthday);
            userInfo.put("Age", driverAge);

            databaseDriver.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()){
                        Toast.makeText(getActivity(), "Successfully Updated", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getActivity(), "Failed to Update", Toast.LENGTH_LONG).show();
                    }
                }
            });


            if (resultUri != null) {

                StorageReference filepath = FirebaseStorage.getInstance().getReference().child("Profile Images").child(user_id);

                Bitmap bitmap = null;

                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getActivity().getApplication().getContentResolver(), resultUri);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream boss = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, boss);
                byte[] data = boss.toByteArray();
                UploadTask uploadTask = filepath.putBytes(data);


                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(getActivity(), "Failed to upload the picture", Toast.LENGTH_LONG).show();
                    }
                });


                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> downloadUrl = taskSnapshot.getStorage().getDownloadUrl();

                        if (taskSnapshot.getMetadata().getReference() != null) {
                            Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Map newImage = new HashMap();
                                    newImage.put("Profile Images Url", downloadUrl.toString());
                                    databaseDriver.updateChildren(newImage);

                                    Toast.makeText(getActivity(), "Succesfully upload your picture", Toast.LENGTH_LONG).show();
                                }
                            });
                        }



                    }
                });
            }
        }

        else{

            Toast.makeText(getActivity(), "Fill empty field", Toast.LENGTH_LONG).show();
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                resultUri = result.getUri();
                driverProfileImage.setImageURI(resultUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(getActivity(), ""+error, Toast.LENGTH_LONG).show();

            }
        }
    }



    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}

