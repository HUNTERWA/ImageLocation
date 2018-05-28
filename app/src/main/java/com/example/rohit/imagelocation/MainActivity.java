package com.example.rohit.imagelocation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
{
    private static final int PICK_IMAGE=100;
    String url="http://192.168.0.110:9000/upload";
    String mimeType,x;  //x is uri path

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Button button=findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent=new Intent(Intent.ACTION_PICK, Uri.parse("content://media/internal/images/media"));
                startActivityForResult(intent,PICK_IMAGE);
            }
        });*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},100);
                return;
            }
        }
        enable_button();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 100 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
            enable_button();
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},100);
            }
        }
    }

    private void enable_button()
    {
        Button button=findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent=new Intent(Intent.ACTION_PICK, Uri.parse("content://media/internal/images/media"));
                startActivityForResult(intent,PICK_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode==RESULT_OK && requestCode==PICK_IMAGE) {
            Uri uri = data.getData();
            x = getPath(uri);
            TextView textView = findViewById(R.id.textView);
            textView.setText(x);
            mimeType = getMimeType(x);
            Log.d("mimeTypeIs", mimeType);


            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    OkHttpClient okHttpClient = new OkHttpClient();
                    RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), new File(x));

                    RequestBody request = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("type", mimeType)
                            .addFormDataPart("file", x.substring(x.lastIndexOf("/") + 1), requestBody)
                            .build();

                    Request req = new Request.Builder()
                            .url(url)
                            .post(request)
                            .build();

                    try {
                        Response response = okHttpClient.newCall(req).execute();
                        Log.d("responseFromServer", "Is" + response);

                        if (!response.isSuccessful()) {
                            throw new IOException("Error : " + response);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
            thread.start();
        }
    }

    private String getPath(Uri uri)
    {
        if (uri==null)
        {
            return null;
        }
        String[] projection={MediaStore.Images.Media.DATA};
        Cursor cursor=managedQuery(uri,projection,null,null,null);
        if (cursor!=null)
        {
            int column_index=cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return  cursor.getString(column_index);
        }

        return uri.getPath();
    }

    public static String getMimeType(String x) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(x);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}
