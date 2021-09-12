package com.example.user.mobilerideshareapplicationmbs;


import android.app.Activity;
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
import android.widget.Button;
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
import com.google.firebase.auth.AdditionalUserInfo;
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
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;


public class DriverCarFragment extends Fragment implements View.OnClickListener {

    private ImageView carImage;
    private Uri resultUri;

    private DatabaseReference databaseCarInfo;

    private String user_id;
    private String carImageUrl;

    private String carManufacturer;
    private String carType;
    private String carYear;
    private String carPlatNumber;
    private String car_color;

    private EditText carManufacturerField;
    private EditText carTypeField;
    private EditText carYearField;
    private EditText carPlatNumberField;
    private EditText carColorField;

    private Button updateCarInfo;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_driver_car, container, false);

        carImage = view.findViewById(R.id.car_images);
        carManufacturerField = view.findViewById(R.id.car_manufacturer);
        carTypeField = view.findViewById(R.id.car_type);
        carYearField = view.findViewById(R.id.car_year);
        carPlatNumberField= view.findViewById(R.id.car_plat_number);
        carColorField= view.findViewById(R.id.car_color);
        updateCarInfo = view.findViewById(R.id.update_car_info);

        carImage.setOnClickListener(this);
        updateCarInfo.setOnClickListener(this);


        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseCarInfo = FirebaseDatabase.getInstance().getReference("Car Information").child(user_id);

        getUserInfo();

        return view;
    }

    private void getUserInfo() {

        databaseCarInfo.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("Car Manufacturer") != null) {
                        carManufacturer = map.get("Car Manufacturer").toString();
                        carManufacturerField.setText(carManufacturer);
                    }

                    if (map.get("Car Type") != null) {
                        carType = map.get("Car Type").toString();
                        carTypeField.setText(carType);
                    }

                    if (map.get("Car Year") != null) {
                        carYear = map.get("Car Year").toString();
                        carYearField.setText(carYear);
                    }

                    if (map.get("Car Plat Number") != null) {
                        carPlatNumber = map.get("Car Plat Number").toString();
                        carPlatNumberField.setText(carPlatNumber);
                    }
                    if (map.get("Car Color") != null) {
                        car_color = map.get("Car Color").toString();
                        carColorField.setText(car_color);
                    }
                    if (map.get("Car Images Url") != null) {
                        carImageUrl = map.get("Car Images Url").toString();
                        Glide.with(getActivity().getApplication()).load(carImageUrl).into(carImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.car_images:
                CropImage.activity()
                        .setAspectRatio(1,1)
                        .setCropShape(CropImageView.CropShape.OVAL)
                        .start(getContext(), this);
                break;

            case R.id.update_car_info:
                storeCarInformation();
                break;
        }

    }

    private void storeCarInformation() {

        carManufacturer = carManufacturerField.getText().toString();
        carType = carTypeField.getText().toString();
        carYear = carYearField.getText().toString();
        carPlatNumber = carPlatNumberField.getText().toString();
        car_color = carColorField.getText().toString();

        if (!TextUtils.isEmpty(carManufacturer) && !TextUtils.isEmpty(carType) && !TextUtils.isEmpty(carYear) && !TextUtils.isEmpty(carPlatNumber) && !TextUtils.isEmpty(car_color)) {

            Map carInfo = new HashMap();
            carInfo.put("Car Manufacturer", carManufacturer);
            carInfo.put("Car Type", carType);
            carInfo.put("Car Year", carYear);
            carInfo.put("Car Plat Number", carPlatNumber);
            carInfo.put("Car Color", car_color);

            databaseCarInfo.updateChildren(carInfo).addOnCompleteListener(new OnCompleteListener() {
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

                StorageReference filepath = FirebaseStorage.getInstance().getReference().child("Car Images").child(user_id);

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
                        newImage.put("Car Images Url", downloadUrl.toString());
                        databaseCarInfo.updateChildren(newImage);

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
                carImage.setImageURI(resultUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(getActivity(), ""+error, Toast.LENGTH_LONG).show();

            }
        }
    }
}
