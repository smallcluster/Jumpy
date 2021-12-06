package com.smallcluster.jumpy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SkinActivity extends AppCompatActivity {

    private static final int MY_CAMERA_REQUEST_CODE = 100;

    ImageView avatarImageView;
    Button photo;
    Bitmap avatar = null;

    // photo intent launcher
    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Bundle bundle = data.getExtras();

                        Bitmap finalPhoto = (Bitmap) bundle.get("data");

                        int w = finalPhoto.getWidth();
                        int h = finalPhoto.getHeight();
                        int min = Math.min(w,h);

                        Bitmap crop = null;
                        if(w == min){
                            crop = Bitmap.createBitmap(finalPhoto, 0, h/2-w/2, w,w);
                        } else {
                            crop = Bitmap.createBitmap(finalPhoto, w/2-h/2, 0, h,h);
                        }

                        avatar = Bitmap.createScaledBitmap(crop, 200, 200, true);



                        avatarImageView.setImageBitmap(avatar);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skin);
        avatarImageView = findViewById(R.id.imageView);
        photo = findViewById(R.id.button2);
        photo.setOnClickListener(view -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
            } else {
                prendrePhoto();
            }
        });
    }

    public void prendrePhoto(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Il y a une application pour prendre une photo
        if (intent.resolveActivity(getPackageManager()) != null) {
            someActivityResultLauncher.launch(intent);
        } else {
            Toast.makeText(SkinActivity.this, "Aucunne application pour effectuer la photo.", Toast.LENGTH_LONG).show();
        }
    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "permission camera acceptée", Toast.LENGTH_LONG).show();
                prendrePhoto();
            } else {
                Toast.makeText(this, "Permission camera refusée", Toast.LENGTH_LONG).show();
            }

        }}//end onRequestPermissionsResult

    public void jouer(View view){
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("avatar", avatar);
        startActivity(intent);
    }
}
