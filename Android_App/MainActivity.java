package com.example.custommodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



import com.example.custommodel.ml.FinalModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private static final int CAMERA_PERMISSION_CODE=100;
    private static final int STORAGE_PERMISSION_CODE=101;
    private static final int SELECT_PICTURE = 200;
    private static final int REQUEST_VIDEO_CAPTURE = 1;
    // Text??? ???????????? ???????????? ?????? ?????? TTS
    TextToSpeech TTS;
    // Firebase Firestore Cloud??? ??????
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ImageView imgView;
    private Button select, predict, camera;
    private TextView tv;
    private Bitmap img;
    private float[] results = new float[10];
    private String answer = "";
    float max = 0;
    int max_index = 0;
    String filePath;
    File imageFile;
    Uri photoURI;
    String BASE_URL = "http://192.168.123.103:5000/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TTS = new TextToSpeech(this, this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);

        imgView = (ImageView) findViewById(R.id.imageView);
        select = (Button) findViewById(R.id.btn_select);
        predict = (Button) findViewById(R.id.btn_predict);
        tv = (TextView) findViewById(R.id.tv_result);
        camera = (Button) findViewById(R.id.btn_camera);


        // select ?????? ????????? ??????.
        select.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                imageChooser();
            }
        });
        // camera ?????? ????????? ??????.
        camera.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });
        // predict ?????? ????????? ??????.
        predict.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // image??? Bitmap???????????? ??????.
                BitmapDrawable drawable = (BitmapDrawable) imgView.getDrawable();
                img = drawable.getBitmap();
                img = Bitmap.createScaledBitmap(img, 150, 150, true);
                try {
                    // ?????????????????? tflite ????????? ????????? ???????????? ?????? ????????? ?????? ??????
                    FinalModel model = FinalModel.newInstance(getApplicationContext());

                    // ????????? ?????? ????????? ?????? ?????? ?????? ??????
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150, 150, 3}, DataType.FLOAT32);


                    //converBitmapToByteBuffer??? ????????? bitmap????????? ???????????? ???????????? byteBuffer??? ????????????.
                    ByteBuffer byteBuffer = convertBitmapToByteBuffer(img);
                    inputFeature0.loadBuffer(byteBuffer);

                    // ????????? ??????????????? ????????? ??????
                    FinalModel.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    // ????????? ????????? ???????????? ???????????? close???
                    model.close();
                    // ????????? ?????? ?????? ?????? ???????????? ????????? max??? ????????? ???????????? ?????? ??????, max_index??? ????????? ????????? ???????????? ??????
                    max = 0;
                    max_index = 0;
                    for(int i=0; i<10; i++){

                        results[i] = outputFeature0.getFloatArray()[i];
                        if (max < results[i]){
                            max = results[i];
                            max_index = i;
                        }
                    }

                    // ????????? ???????????? ???????????? ????????? ?????? ??????
                    switch(max_index){
                        case 0:
                            answer = "cider";
                            searchDrink(answer);
                            break;
                        case 1:
                            answer = "coke";
                            searchDrink(answer);
                            break;
                        case 2:
                            answer = "fanta";
                            searchDrink(answer);
                            break;
                        case 3:
                            answer = "milkis";
                            searchDrink(answer);
                            break;
                        case 4:
                            answer = "monster";
                            searchDrink(answer);
                            break;
                        case 5:
                            answer = "mtdew";
                            searchDrink(answer);
                            break;
                        case 6:
                            answer = "pepsi";
                            searchDrink(answer);
                            break;
                        case 7:
                            answer = "soda";
                            searchDrink(answer);
                            break;
                        case 8:
                            answer = "sprite";
                            searchDrink(answer);
                            break;
                        case 9:
                            answer = "toreta";
                            searchDrink(answer);
                            break;

                    }

                } catch (IOException e) {
                    Log.d("error", e.toString());
                }
                if(photoURI!=null){
                    uploadFile(photoURI);
                }



            }
        });


    }// Oncreate???.


    // ????????? ??????????????? ?????? ????????? ????????? db??? ???????????? ?????????
    // ????????? db ???????????? ??????????????? name(????????? ??????), flavor(???)??? 2??? ???????????? ??????
    private void searchDrink(String answer){
        // firebase db??? DocumentReference ???????????? ???????????? ???????????? answer??? ???????????? ????????? ?????????
        DocumentReference drinksRef = db.collection("drinks").document(answer);
        drinksRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot doc = task.getResult();
                    if(doc.exists() && max > 0.8){
                        System.out.println("??????"+ Float.toString(max));
                        Log.d("Document", doc.getData().toString());
                        Log.d("Document", doc.get("name").toString());
                        String num = String.format("%.1f", max*100);
                        tv.setText("????????? ?????? : " + doc.get("name").toString() + "\n" +
                                        "????????? ?????? : " + doc.get("type").toString() + "\n" +
                                "????????? ??? : " + doc.get("flavor").toString() + "\n"
                                //+ "?????? : "+ num + "%"
                        );
                        speak();
                    }else{
                        tv.setText("?????? ??????????????????");
                        speak();
                        Log.d("Document", "No data");
                    }
                }
            }
        });
    }
    // camera?????? ????????? ???????????? ??????
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
            try{
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("error", "image file not created");
            }
            if(photoFile != null){
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.custommodel.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_PERMISSION_CODE);

            }
        }
        //startActivityForResult(takePictureIntent, CAMERA_PERMISSION_CODE);
    }
    // video ???????????? ??????
    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }
    // ?????? ????????? image file??? ??????.(????????? ?????? ???????????? ?????????)
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        filePath = image.getAbsolutePath();
        return image;
    }


    // tflite?????? ????????? ?????? ????????? bitmap??? bytebuffer??? ???????????? ????????? ??????
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bp) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(Float.BYTES*150*150*3);
        imgData.order(ByteOrder.nativeOrder());
        //????????? ???????????? ????????? ???????????? bitmap?????? ?????? filter: true??? ?????? ????????? ?????? ????????????
        Bitmap bitmap = Bitmap.createScaledBitmap(bp,150,150,true);
        int [] intValues = new int[150*150];
        //bitmap?????? ?????? ?????? ????????? ???????????? intValues??? ?????????.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Convert the image to floating point.
        int pixel = 0;

        for (int i = 0; i < 150; ++i) {
            for (int j = 0; j < 150; ++j) {
                final int val = intValues[pixel++];
                //0~255??? ?????? 8????????? ??? 0xFF??? 11111111??? ????????????.
                //val??? ????????? ???????????? ?????? R,G,B ????????? ???????????? 8??? ????????? ??? 24?????? ????????? ???????????? ????????????.
                //??????????????? 16??? shift?????? ?????? ???????????? 8??? ????????? ??????????????? ?????? 0xFF??? and????????? 8????????? ????????? ????????? ?????????.
                imgData.putFloat(((val>> 16) & 0xFF) / 255.f);
                imgData.putFloat(((val>> 8) & 0xFF) / 255.f);
                imgData.putFloat((val & 0xFF) / 255.f);
            }
        }
        return imgData;
    }
    // ?????? ?????? ??????
    public void checkPermission(String permission, int requestCode){
        if(ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {permission}, requestCode);
        }
        else{
            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }
    // select ?????? ????????? ???????????? ??????
    void imageChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Picture"),SELECT_PICTURE);
    }
    // ?????? ????????? ???????????? toast message ??????
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                          @NonNull String[] permissions,
                                          @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE){

            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(MainActivity.this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode == STORAGE_PERMISSION_CODE){

            if(grantResults.length > 0 &&  grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(MainActivity.this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // ?????? ????????? ??????, ????????? ??????????????? intent??? ??????????????? ???????????? ??????
    // ??????????????? ??????????????? ????????? ???????????? ????????? ???????????? ??????
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                imgView.setImageURI(data.getData());

                Uri uri = data.getData();

                if (null != uri){
                    photoURI = uri;
                    imgView.setImageURI(uri);
                }

            }
            if (requestCode == CAMERA_PERMISSION_CODE) {
                imgView.setImageURI(photoURI);
            }
        }
    }
    // TTS ????????? initialization?????? ?????? ??????
    @Override
    public void onInit(int i) {
        if(i == TextToSpeech.SUCCESS){
            int result = TTS.setLanguage(Locale.KOREAN);
            TTS.setSpeechRate(1);
            TTS.setPitch(1);
            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Log.d("TTS", "Language not supported");
            }
            else {
                predict.setEnabled(true);
                speak();
            }
        }
        else{
            Log.d("TTS", "Initialization failed");
        }
    }
    // Text??? ???????????? ???????????? ??????
    private void speak(){
        String message = tv.getText().toString();
        Log.d("TTS", message);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            TTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }



    private void uploadFile(Uri fileUri){
        FileUtils fileutils = new FileUtils(MainActivity.this);
        File originalFile = new File(fileutils.getPath(fileUri));
        RequestBody filePart = RequestBody.create(originalFile, MediaType.parse(getContentResolver().getType(fileUri)));
        MultipartBody.Part file = MultipartBody.Part.createFormData("file", originalFile.getName(), filePart);

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        UserClient client = retrofit.create(UserClient.class);
        Call<drinks> call = client.uploadPhoto(file);
        call.enqueue(new Callback<drinks>() {
            @Override
            public void onResponse(Call<drinks> call, Response<drinks> response) {
                Toast.makeText(MainActivity.this, "Image uploaded", Toast.LENGTH_SHORT).show();
                if(!response.isSuccessful()){
                    Log.d("prediction", "response failed");
                    return;
                }

                String drink_name= response.body().getName();
                String drink_type = response.body().getType();
                String drink_flavor = response.body().getFlavor();
                String drink_cautions = response.body().getCautions();
                System.out.println("??????!!!!");
                System.out.println(drink_name);
                System.out.println(drink_type);
                System.out.println(drink_flavor);
                System.out.println(drink_cautions);
            }

            @Override
            public void onFailure(Call<drinks> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });

    }

}// Activity ???
