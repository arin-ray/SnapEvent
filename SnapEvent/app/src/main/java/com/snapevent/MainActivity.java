package com.snapevent;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity {
    protected Button button;
    private final String SERVERURL = "";
    private final static String INPUT_IMG_FILENAME = "/temp.jpg"; //name for storing image captured by camera view
    InputStream inputStream;
    String mCurrentPhotoPath;
    //EditText editText;
    String responseBody = "";

    static final int REQUEST_TAKE_PHOTO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove the title bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        EditText editText = (EditText)findViewById(R.id.edit_text);
       // editText.setText("Wating for OCR...", TextView.BufferType.EDITABLE);
        setContentView(R.layout.activity_main);
        // Added code
        startCameraActivity();
    }

    protected void startCameraActivity()
    {
        // Creating a new intent with and that will save to a path based on DATE/TIME
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE );

        // Ensure that there's a camera activity to handle the intent
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the file
                Log.e("MainActivity ", "file create error");
            }
            // Continue  if the file was created
            if (photoFile != null) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(cameraIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i("UPLOAD", String.format("onActivityResult: requestCode=%d resultCode=%d", requestCode,resultCode));

        if (requestCode == 1 && resultCode == RESULT_OK) {
          // Bundle extras = data.getExtras();
            Log.i("UPLOAD", "Going to get the pic");

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, options);
            Log.i("UPLOAD", "HERE");

            try {
                Log.i("UPLOAD ", "on sendPhoto");

                sendPhoto(bitmap);
                //editText.setText(responseBody,TextView.BufferType.EDITABLE);
			} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        Log.i("UPLOAD ", "ABT TO CREATE A FILE");

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "SnapEventJPEG_" + timeStamp + "_";
        String storageDir = Environment.getExternalStorageDirectory() + "/SnapEvent";
        File dir = new File(storageDir);
        if (!dir.exists())
            dir.mkdir();

        File image = new File(storageDir + "/" + imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.i("UPLOAD ", "photo path = " + mCurrentPhotoPath);
        return image;
    }

    private void sendPhoto(Bitmap bitmap) throws Exception {
        new UploadTask().execute(bitmap);
    }

    private class UploadTask extends AsyncTask<Bitmap, Void, Void> {
        protected Void doInBackground(Bitmap... bitmaps) {
            if (bitmaps[0] == null)
                return null;
            setProgress(0);

            Bitmap bitmap = bitmaps[0];
            // Need to rotate for some dumb reason....
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            rotated.compress(Bitmap.CompressFormat.JPEG, 100, stream); // convert Bitmap to ByteArrayOutputStream
            InputStream in = new ByteArrayInputStream(stream.toByteArray()); // convert ByteArrayOutputStream to ByteArrayInputStream

            DefaultHttpClient httpclient = new DefaultHttpClient();
            Log.i("UPLOAD ", "ATTEMPTING TO POST MA NIGGA");

            try {
                HttpPost httppost = new HttpPost(
                        "http://ec2-54-69-176-107.us-west-2.compute.amazonaws.com/test.php"); // server

                MultipartEntity reqEntity = new MultipartEntity();
                reqEntity.addPart("uploadedfile",
                        System.currentTimeMillis() + ".jpg", in);
                httppost.setEntity(reqEntity);

                Log.i("UPLOAD ", "request " + httppost.getRequestLine());
                HttpResponse response = null;
                try {
                    //Log.i("UPLOAD ","POSTING :" +EntityUtils.toString(httppost.getEntity()));
                    response = httpclient.execute(httppost);
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    if (response != null)
                        Log.i("UPLOAD ", "response " + response.getStatusLine().toString());
                    try {
                        responseBody = EntityUtils.toString(response.getEntity());
                        Log.i("UPLOAD ", "OUTPUT: " + responseBody);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } finally {

                }
            } finally {
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (stream != null) {
                try {
                 //   editText.setText(responseBody,TextView.BufferType.EDITABLE);

                    stream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            Toast.makeText(MainActivity.this, R.string.uploaded, Toast.LENGTH_LONG).show();
            //editText.setText(responseBody,TextView.BufferType.EDITABLE);
           // Html.fromHtml(responseBody);
            ((EditText) findViewById(R.id.edit_text)).setText(Html.fromHtml(responseBody), TextView.BufferType.EDITABLE);
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.i("UPLOAD ", "onResume: " + this);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        Log.i("UPLOAD ", "onSaveInstanceState");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
