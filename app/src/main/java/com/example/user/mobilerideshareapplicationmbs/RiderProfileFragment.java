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

import static android.app.Activity.RESULT_OK;


public class RiderProfileFragment extends Fragment implements View.OnClickListener{


    private ImageView riderProfileImage;
    private Uri resultUri;

    private DatabaseReference databaseRider;

    private String user_id;
    private String profileImageUrl;

    private String riderName;
    private String riderPhone;
    private String riderGender;
    private String riderBirthday;
    private String riderAge;

    String nameList[] = {"Male","Female"};

    private EditText riderNameField;
    private EditText riderNumberField;
    private Spinner riderGenderField;
    private EditText riderBirthdayField;
    private EditText riderAgeField;

    private Button updateRiderInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_rider_profile, container, false);

        riderGenderField = (Spinner) view.findViewById(R.id.rider_gender);
        ArrayAdapter adapter = new ArrayAdapter(getActivity(), R.layout.spinner_gender, nameList);
        riderGenderField.setAdapter(adapter);


        riderProfileImage = view.findViewById(R.id.rider_profile_image);
        riderNameField = view.findViewById(R.id.rider_name);
        riderBirthdayField = view.findViewById(R.id.rider_birthday);
        riderAgeField = view.findViewById(R.id.rider_age);
        riderNumberField= view.findViewById(R.id.rider_number);
        updateRiderInfo = view.findViewById(R.id.update_rider_info);

        riderProfileImage.setOnClickListener(this);
        updateRiderInfo.setOnClickListener(this);
        riderBirthdayField.setOnClickListener(this );


        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRider = FirebaseDatabase.getInstance().getReference("Rider").child(user_id);

        getUserInfo();



        return view;
    }


    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.rider_profile_image:
                CropImage.activity()
                        .setAspectRatio(1,1)
                        .setCropShape(CropImageView.CropShape.OVAL)
                        .start(getContext(), this);
                break;

            case R.id.update_rider_info:
                storeUserInformation();
                break;

            case R.id.rider_birthday:
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

            riderBirthdayField.setText(String.valueOf(dayOfMonth) + "-" + String.valueOf(monthOfYear+1) + "-" + String.valueOf(year));

            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - year;

            if (today.get(Calendar.DAY_OF_YEAR) < year){
                age--;
            }

            Integer ageInt = new Integer(age);
            String ageS = ageInt.toString();

            riderAgeField.setText(ageS + " years old");

        }
    };






    public void getUserInfo(){

        databaseRider.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                        if (map.get("User Name") != null) {
                            riderName = map.get("User Name").toString();
                            riderNameField.setText(riderName);
                        }

                        if (map.get("Contact Number") != null) {
                            riderPhone = map.get("Contact Number").toString();
                            riderNumberField.setText(riderPhone);
                        }

                        if (map.get("Gender") != null) {
                            riderGender = map.get("Gender").toString();
                            if (riderGender.equals("Male")) {
                                riderGenderField.setSelection(0);
                            }
                            else{
                                riderGenderField.setSelection(1);
                            }
                        }

                        if (map.get("Date") != null) {
                            riderBirthday = map.get("Date").toString();
                            riderBirthdayField.setText(riderBirthday);
                        }

                        if (map.get("Age") != null) {
                            riderAge = map.get("Age").toString();
                            riderAgeField.setText(riderAge);
                        }

                        if (map.get("Profile Images Url") != null) {
                            profileImageUrl = map.get("Profile Images Url").toString();
                            Glide.with(getActivity().getApplication()).load(profileImageUrl).into(riderProfileImage);
                        }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    public void storeUserInformation(){

        riderName = riderNameField.getText().toString();
        riderPhone = riderNumberField.getText().toString();
        riderGender = riderGenderField.getSelectedItem().toString();
        riderBirthday = riderBirthdayField.getText().toString();
        riderAge = riderAgeField.getText().toString();

        if (!TextUtils.isEmpty(riderName) && !TextUtils.isEmpty(riderPhone) &&!TextUtils.isEmpty(riderGender) &&!TextUtils.isEmpty(riderBirthday) && !TextUtils.isEmpty(riderAge) ) {

            Map userInfo = new HashMap();
            userInfo.put("User Name", riderName);
            userInfo.put("Contact Number", riderPhone);
            userInfo.put("Gender", riderGender);
            userInfo.put("Date", riderBirthday);
            userInfo.put("Age", riderAge);

            databaseRider.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener() {
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
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();

                        Map newImage = new HashMap();
                        newImage.put("Profile Images Url", downloadUrl.toString());
                        databaseRider.updateChildren(newImage);

                        Toast.makeText(getActivity(), "Succesfully upload your picture", Toast.LENGTH_LONG).show();

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
                riderProfileImage.setImageURI(resultUri);
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
