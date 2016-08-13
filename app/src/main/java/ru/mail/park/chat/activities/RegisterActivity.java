package ru.mail.park.chat.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import ru.mail.park.chat.R;
import ru.mail.park.chat.activities.tasks.RegisterTask;
import ru.mail.park.chat.api.HttpFileUpload;
import ru.mail.park.chat.auth_signup.IRegisterCallbacks;
import ru.mail.park.chat.models.OwnerProfile;

public class RegisterActivity extends AppCompatActivity implements IRegisterCallbacks {
    private AutoCompleteTextView mLoginView;
    private AutoCompleteTextView mFirstNameView;
    private AutoCompleteTextView mLastNameView;
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private EditText mConfirmPassvordView;
    private ProgressBar mProgressView;
    private Button emailSignUpButton;

    //step two
    private CircleImageView regImagePreview;
    private ImageButton regUserCameraShot;
    private ImageButton regUserUploadPicture;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int GET_FROM_GALLERY = 3;
    private String selectedFilePath;
    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mPasswordView = (EditText) findViewById(R.id.register_password);
        mConfirmPassvordView = (EditText) findViewById(R.id.register_confirm_password);
        mEmailView = (AutoCompleteTextView) findViewById(R.id.register_email);
        mLoginView = (AutoCompleteTextView) findViewById(R.id.register_login);
        mFirstNameView = (AutoCompleteTextView) findViewById(R.id.register_first_name);
        mLastNameView = (AutoCompleteTextView) findViewById(R.id.register_last_name);

        mProgressView = (ProgressBar) findViewById(R.id.register_progress);
        emailSignUpButton = (Button) findViewById(R.id.email_sign_up_button);

        regImagePreview = (CircleImageView) findViewById(R.id.reg_user_picture_in_editor);
        regUserCameraShot = (ImageButton) findViewById(R.id.reg_user_camera_shot);
        regUserUploadPicture = (ImageButton) findViewById(R.id.reg_user_upload_picture);

        emailSignUpButton.setOnClickListener(signUpListener);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && toolbar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        regUserCameraShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                File photo = null;
                try {
                    photo = RegisterActivity.this.createTemporaryFile("picture", ".jpg");
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

        regUserUploadPicture.setOnClickListener(new View.OnClickListener() {
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
            String filePath;

            Log.d("[TP-diploma]", "RegisterActivity onActivityResult");

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Log.d("[TP-diploma]", "sending file started");
                try {
                    selectedFilePath = mImageUri.getPath();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if(requestCode == GET_FROM_GALLERY) {
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
                } catch(Exception e) {
                    Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                }
            }

            if(selectedFilePath != null)
                regImagePreview.setImageBitmap(BitmapFactory.decodeFile(selectedFilePath));
        }
    }

    private final View.OnClickListener signUpListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            register();
        }
    };

    private void register() {
        RegisterTask task = new RegisterTask(this, this);
        String login = mLoginView.getText().toString();
        String firstName = mFirstNameView.getText().toString();
        String lastName = mLastNameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String confirmPassword = mConfirmPassvordView.getText().toString();
        String email = mEmailView.getText().toString();
        boolean imageFound = true;

        File file;
        try {
            file = new File(selectedFilePath);

            if(!file.exists())
                imageFound = false;
        } catch(NullPointerException e) {
            imageFound = false;
        }

        if (password.equals(confirmPassword))
            task.execute(login, firstName, lastName, password, email, imageFound ? selectedFilePath : null);
        else
            mPasswordView.setError("Password and confirmation don't match.");
    }

    @Override
    public void onRegistrationStart() {
        mProgressView.setVisibility(View.VISIBLE);
        emailSignUpButton.setEnabled(false);
    }

    @Override
    public void onRegistrationSuccess(OwnerProfile contact) {
        contact.saveToPreferences(this);
        Intent intent = new Intent(this, ChatsActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRegistrationFail(Map<ErrorType, String> errors) {
        mLoginView.setError(null);
        mFirstNameView.setError(null);
        mLastNameView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);

        if (errors != null) {
            for (Map.Entry<ErrorType, String> error : errors.entrySet()) {
                switch (error.getKey()) {
                    case LOGIN:
                        mLoginView.setError(error.getValue());
                        break;
                    case FIRST_NAME:
                        mFirstNameView.setError(error.getValue());
                        break;
                    case LAST_NAME:
                        mLastNameView.setError(error.getValue());
                        break;
                    case EMAIL:
                        mEmailView.setError(error.getValue());
                        break;
                    case PASSWORD:
                        mPasswordView.setError(error.getValue());
                        break;
                }
            }
        } else {
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRegistrationFinish() {
        emailSignUpButton.setEnabled(true);
        mProgressView.setVisibility(View.GONE);
    }
}
