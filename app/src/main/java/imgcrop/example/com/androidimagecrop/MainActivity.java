package imgcrop.example.com.androidimagecrop;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private ImageButton imageButton;
    private Button uploadButton;
    private  static  final  int Image_request=1;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReferenceFromUrl("gs://androidimagecrop.appspot.com");
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);
        //getting the reference of the views
        imageButton = (ImageButton) findViewById(R.id.imageview);
        uploadButton = (Button) findViewById(R.id.uploadbutton);
        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Uploading image\n thoda sbar rakh MC!!");
        onImageViewClick(); // for selecting an Image from gallery.
        onUploadButtonClick(); // for uploading the image to Firebase Storage.
    }

    protected  void onImageViewClick(){
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent  .setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent,Image_request );
            }
        });

    }
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Image_request && resultCode == RESULT_OK){
                Uri selectedImageUri = data.getData();
            CropImage.activity(selectedImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
                if (null != selectedImageUri) {
                    // Get the path from the Uri
                    String path = getPathFromURI(selectedImageUri);
                    imageButton.setImageURI(selectedImageUri);

                }

        }
        if (requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result= CropImage.getActivityResult(data);
            if(resultCode==RESULT_OK)
            {
               Uri resulturi=result.getUri();
                imageButton.setImageURI(resulturi);
                StorageReference filepath=  storageRef.child(resulturi.getLastPathSegment());
            }
            else
            {
                if(resultCode==CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
                {
                    Exception error=result.getError();
                }
            }
        }
    }
    private String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    protected void onUploadButtonClick(){

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                 storageRef = FirebaseStorage.getInstance().getReference().child("Profiles/"+System.currentTimeMillis());

                imageButton.setDrawingCacheEnabled(true);
                imageButton.buildDrawingCache();
                Bitmap bitmap = imageButton.getDrawingCache();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                UploadTask uploadTask = storageRef.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(MainActivity.this, "TASK FAILED", Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Image Uploaded SucessFully", Toast.LENGTH_SHORT).show();
                        Uri downloadUrl =taskSnapshot.getDownloadUrl();
                        String DOWNLOAD_URL = downloadUrl.getPath();

                    }
                });
            }
        });

    }
}