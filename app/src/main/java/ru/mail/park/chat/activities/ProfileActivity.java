package ru.mail.park.chat.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import ru.mail.park.chat.R;
import ru.mail.park.chat.activities.tasks.UpdateProfileTask;
import ru.mail.park.chat.api.HttpFileUpload;
import ru.mail.park.chat.api.MultipartProfileUpdater;
import ru.mail.park.chat.models.OwnerProfile;

public class ProfileActivity extends AppCompatActivity implements MultipartProfileUpdater.IUploadListener {

    private ImageView imgCameraShot;
    private ImageView imgUploadPicture;
    private TextView  userTitle;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int GET_FROM_GALLERY = 3;
    private static final String FILE_UPLOAD_URL = "http://p30480.lab1.stud.tech-mail.ru/files/upload";
    private String accessToken;
    private String selectedFilePath;

    private EditText userLogin;
    private EditText userEmail;
    private EditText firstName;
    private EditText lastName;
    private EditText userPhone;

    private Intent takePictureIntent;

    private Uri mImageUri;

    private ProfileActivity thisAct = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        thisAct = this;

        imgCameraShot = (ImageView) findViewById(R.id.user_camera_shot);
        imgUploadPicture = (ImageView) findViewById(R.id.user_upload_picture);
        userTitle = (TextView) findViewById(R.id.user_title);

        userLogin = (EditText) findViewById(R.id.user_login);
        userEmail = (EditText) findViewById(R.id.user_email);
        userPhone = (EditText) findViewById(R.id.user_phone);
        firstName = (EditText) findViewById(R.id.first_name);
        lastName  = (EditText) findViewById(R.id.last_name);

        OwnerProfile ownerProfile = new OwnerProfile(this);
        userTitle.setText(ownerProfile.getContactTitle());
        userLogin.setText(ownerProfile.getLogin());
        userEmail.setText(ownerProfile.getEmail());
        userPhone.setText(ownerProfile.getPhone());
        firstName.setText(ownerProfile.getFirstName());
        lastName.setText(ownerProfile.getLastName());

        accessToken = ownerProfile.getAuthToken();

        imgCameraShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                File photo = null;
                try {
                    // place where to store camera taken picture
                    Log.d("[TP-diploma]", "creating tmp file");
                    photo = thisAct.createTemporaryFile("picture", ".jpg");
                    photo.delete();
                } catch (Exception e) {
                    Log.d("[TP-diploma]", "Can't create file to take picture!");
                }
                mImageUri = Uri.fromFile(photo);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                //start camera intent

                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    Log.d("[TP-diploma]", "starting activity");
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        imgUploadPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent uploadPictureIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(uploadPictureIntent, GET_FROM_GALLERY);
            }
        });
    }

    private File createTemporaryFile(String part, String ext) throws Exception
    {
        File tempDir= Environment.getExternalStorageDirectory();
        tempDir=new File(tempDir.getAbsolutePath()+"/.temp/");
        if(!tempDir.exists())
        {
            tempDir.mkdirs();
        }
        Log.d("[TP-diploma]", "inside createTemporaryFile");
        return File.createTempFile(part, ext, tempDir);
    }

    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String filePath = "";//data.getStringExtra(GET_FROM_GALLERY);
            FileInputStream fstrm = null;
            HttpFileUpload hfu = null;

            Log.d("[TP-diploma]", "onActivityResult");

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if(resultCode == Activity.RESULT_OK) {
                    Log.d("[TP-diploma]", "sending file started");
                    try {
                        selectedFilePath = mImageUri.getPath();
                        Toast.makeText(ProfileActivity.this, "camera shot: "+selectedFilePath, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if(requestCode == GET_FROM_GALLERY) {
                if(resultCode == Activity.RESULT_OK) {
                    try {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        Cursor cursor = getContentResolver().query(
                                selectedImage, filePathColumn, null, null, null);
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        filePath = cursor.getString(columnIndex);
                        cursor.close();

                        selectedFilePath = filePath;
                        Toast.makeText(ProfileActivity.this, "from gallery: "+selectedFilePath, Toast.LENGTH_SHORT).show();
                    } catch(Exception e) {
                        Toast.makeText(this, "File not found", Toast.LENGTH_SHORT);
                    }
/*                    hfu = new HttpFileUpload(FILE_UPLOAD_URL, filePath.substring(filePath.lastIndexOf('/'), filePath.length()), accessToken);
                    hfu.Send_Now(fstrm, this);*/
                }
            }
        }
    }

    public void onUploadComplete(String name) {
        Toast.makeText(this, name, Toast.LENGTH_LONG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        OwnerProfile currentProfile = new OwnerProfile(this);
        OwnerProfile updatedProfile = getUpdatedProfile();

        if (!currentProfile.equals(updatedProfile)) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setMessage("Cancel changes?");
            alertBuilder.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ProfileActivity.super.onBackPressed();
                        }
                    });
            alertBuilder.setNegativeButton(getString(android.R.string.cancel), null);
            alertBuilder.show();
        } else {
            ProfileActivity.super.onBackPressed();
        }
    }

    private OwnerProfile getUpdatedProfile() {
        OwnerProfile profile = new OwnerProfile(this);
        profile.setEmail(userEmail.getText().toString());
        profile.setLogin(userLogin.getText().toString());
        profile.setPhone(userPhone.getText().toString());
        profile.setFirstName(firstName.getText().toString());
        profile.setLastName(lastName.getText().toString());
        profile.setImg(selectedFilePath);
        return profile;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                OwnerProfile profile = getUpdatedProfile();

                ProgressDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this);
                dialogBuilder.setTitle("Saving user data");
                dialogBuilder.setMessage("Sending data to server");
                dialogBuilder.setCancelable(false);
                new UpdateProfileTask(dialogBuilder.show(), this).execute(profile);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
