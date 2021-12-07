package com.smallcluster.jumpy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
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
    Bitmap avatar;

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
                        Bitmap resized = Bitmap.createScaledBitmap(crop, 100, 100, true);

                        // TODO : utiliser un crop oval
                        /*
                        Bitmap cropCircle = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(cropCircle);
                        int color = 0xff424242;
                        Paint paint = new Paint();
                        Rect rect = new Rect(0, 0, resized.getWidth(), resized.getHeight());
                        float r = 50.0f;
                        paint.setAntiAlias(true);
                        canvas.drawARGB(0, 0, 0, 0);
                        paint.setColor(color);
                        canvas.drawCircle(r, r, r, paint);
                        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                        canvas.drawBitmap(resized, rect, rect, paint);
                        */

                        avatar = resized;
                        avatarImageView.setImageBitmap(crop);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skin);
        avatarImageView = findViewById(R.id.imageView);
        avatar = BitmapFactory.decodeResource(getResources(), R.drawable.face);
        avatarImageView.setImageBitmap(avatar);
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
