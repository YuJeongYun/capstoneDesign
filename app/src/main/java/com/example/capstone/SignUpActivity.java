package com.example.capstone;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "MemberInitActivity";

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ImageView profileImageView;
    private String photo_url, profilePath, address;
    private RelativeLayout loaderLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        findViewById(R.id.infoChangeButton).setOnClickListener(onClickListener);
        findViewById(R.id.locationAuthButton).setOnClickListener(onClickListener);
        loaderLayout = findViewById(R.id.loaderLayout);
        profileImageView = findViewById(R.id.profileImageView);
        findViewById(R.id.galleryButton).setOnClickListener(onClickListener);
    }

    //?????? ????????????????????? ??? ????????????
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        switch (requestCode) {
            case 0: //?????????
                if (resultCode == Activity.RESULT_OK) {
                    profilePath = resultIntent.getStringExtra("profilePath");
                    photo_url = profilePath;
                    Glide.with(this).load(profilePath).centerCrop().override(500).into(profileImageView);
                }
            case 1: //??????
                if (resultCode == RESULT_OK) {
                    address = resultIntent.getStringExtra("address");
                }
        }
    }

    //onClickListener
    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.infoChangeButton:
                    signUp(); //????????????
                    break;

                case R.id.locationAuthButton:
                    myStartActivity(LocationAuthActivity.class, 1); //???????????? ???????????? ??????
                    break;

                case R.id.galleryButton:
                    myStartActivity(GalleryActivity.class, "image"); //???????????? ??????
                    break;
            }
        }
    };

    //????????????
    private void signUp() {
        String email = ((EditText) findViewById(R.id.emailEditText)).getText().toString();
        String password = ((EditText) findViewById(R.id.passwordEditText)).getText().toString();
        String passwordCheck = ((EditText) findViewById(R.id.passwordCheckEditText)).getText().toString();

        Pattern p = Patterns.EMAIL_ADDRESS; // ******* ?????? *********
        Matcher m = p.matcher(email);

        if (email.length() > 0 && password.length() > 0 && passwordCheck.length() > 0) {
            if (password.equals(passwordCheck)) {
                final RelativeLayout loaderLayout = findViewById(R.id.loaderLayout);
                loaderLayout.setVisibility(View.VISIBLE);

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                loaderLayout.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    user = mAuth.getCurrentUser();
                                    startToast("??????????????? ?????????????????????.");
                                    storageUpload();

                                } else {
                                    if(!m.matches()) {
                                        startToast("????????? ???????????? ???????????????");
                                    } else {
                                        startToast("?????? ???????????? ??????????????????.");
                                    }

                                }
                            }
                        });
            } else {
                startToast("??????????????? ???????????? ????????????.");
            }
        } else {
            startToast("????????? ?????? ??????????????? ??????????????????.");
        }
    }

    //????????? ????????? ?????? ??????, Storage??? ?????? ??????
    private void storageUpload() {
        String uid = user.getUid();
        String name = ((EditText) findViewById(R.id.nameEditText)).getText().toString();

        String split_address[] = address.split(" ");
        for (int i = 0; i < split_address.length; i++) {
            System.out.println(split_address[i]);
        }
        String address_gu = split_address[2];
        String address_dong = split_address[3];

        if (name.length() > 0 && address_gu.length() > 0 && address_dong.length() > 0) {
            loaderLayout.setVisibility(View.VISIBLE);
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            user = FirebaseAuth.getInstance().getCurrentUser();
            final StorageReference mountainImagesRef = storageRef.child("users/" + user.getUid() + "/profileImage.jpg");

            if (profilePath == null) {
                MemberInfo memberInfo = new MemberInfo(uid, name, address_gu, address_dong, photo_url);
                storeUpload(memberInfo);
            } else {
                try {
                    InputStream stream = new FileInputStream(new File(profilePath));
                    UploadTask uploadTask = mountainImagesRef.putStream(stream);
                    uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return mountainImagesRef.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();

                                MemberInfo memberInfo = new MemberInfo(uid, name, address_gu, address_dong, photo_url);
                                storeUpload(memberInfo);
                            } else {
                                startToast("??????????????? ???????????? ?????????????????????.");
                            }
                        }
                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else {
            startToast("??????????????? ??????????????????.");
        }
    }

    //DB??? ??????????????? ?????????
    private void storeUpload(MemberInfo memberInfo) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (user != null) {
            db.collection("users").document(user.getUid()).set(memberInfo)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            startToast("???????????? ????????? ?????????????????????.");
                            loaderLayout.setVisibility(View.GONE);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            startToast("???????????? ????????? ?????????????????????.");
                            loaderLayout.setVisibility(View.GONE);
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
        }
    }

    //????????? ?????????
    private void startToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    //?????? ??????????????? ??????
    private void myStartActivity(Class c, int requestCode) {
        Intent intent = new Intent(this, c);
        startActivityForResult(intent, requestCode);
    }
    private void myStartActivity(Class c, String media) {
        Intent intent = new Intent(this, c);
        intent.putExtra("media", media);
        startActivityForResult(intent, 0);
    }
}