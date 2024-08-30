package com.example.jarvis_layout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.ImageButton;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.media.MediaPlayer;
import android.os.Environment;
import android.view.View;
import android.view.MotionEvent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.Manifest;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 200;
    private static final int WRITE_SETTINGS_REQUEST_CODE = 1001;
    private static final long SHORT_PRESS_THRESHOLD = 100;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mprec;
    private TextView timeTextView;
    private Handler handler = new Handler();
    private Runnable runnable;
    private Visualizer visualizer;
    private AudioVisualizerSphereView audioVisualizerSphereView;
    private String filename = null;
    private long pressStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getWindow().getDecorView().post(() -> hideSystemBars());//隐藏系统栏

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            // 如果没有权限，请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

//        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if(!Environment.isExternalStorageManager()){
//                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivity(intent);
//            }
//        }

        //处理录音部分
        ImageButton recordButton = findViewById(R.id.RecordButton);
        File tempDir = getTempDirectory();
        filename = tempDir.getAbsolutePath() + "/Recording";
        mprec = MediaPlayer.create(this, R.raw.record_over);//获取录音部分
        audioVisualizerSphereView = findViewById(R.id.audioVisualizerSphere1);
        mprec.setOnPreparedListener(mediaPlayer -> setupVisualizer());

        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 记录按下的时间
                        pressStartTime = System.currentTimeMillis();
                        // 开始录音
                        startRecording();
                        recordButton.setScaleX((float) 0.90);
                        recordButton.setScaleY((float) 0.90);
                        return false;

                    case MotionEvent.ACTION_UP:
                        // 计算按下的时间
                        long pressDuration = System.currentTimeMillis() - pressStartTime;

                        if (pressDuration < SHORT_PRESS_THRESHOLD) {
                            // 短按处理
                            showToast("Pressed Time Too Short:" + pressDuration + "ms");
                            // 如果短按，停止录音
                            stopRecording();
                            recordButton.setScaleX((float) 1.00);
                            recordButton.setScaleY((float) 1.00);
                            v.performClick();
                            return false;
                        } else {
                            // 长按处理
                            stopRecording();
                            recordButton.setScaleX((float) 1.00);
                            recordButton.setScaleY((float) 1.00);
                            convertToWav(filename + ".3gp", filename + ".wav");
                            showToast("Save Succesed,Duration:" + pressDuration + "ms");


                            mprec.start();


                            return false;
                        }

                    default:
                        return false;
                }
            }
        });

        //更新时钟
        timeTextView = findViewById(R.id.timeTextView);
        runnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);

        //跳转相册
        ImageButton imageButton = findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        //二维码部分
        ImageButton QRbutton = findViewById(R.id.wifi_setting_button);
        QRbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startQRScanner();
            }
        });

        //播放器按钮
        ImageButton musicbutton = findViewById(R.id.musicbutton);
        musicbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent musicintent = new Intent(MainActivity.this, MusicPlayerActivity.class);
                startActivity(musicintent);
            }
        });
    }

    //隐藏bar
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30 及以上：使用 WindowInsetsController
            getWindow().setDecorFitsSystemWindows(false);  // 使内容可以延伸到系统栏区域
            View decorView = getWindow().getDecorView();
            WindowInsetsController insetsController = decorView.getWindowInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // API 30 以下：使用 SYSTEM_UI_FLAG
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    //创建Temp文件夹
    private File getTempDirectory() {
        File tempDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tempDir = new File(getExternalFilesDir(null), "Temp");
        } else {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Temp";
            tempDir = new File(path);
        }

        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return tempDir;
    }

    //转换wav
    private void convertToWav(String inputFilePath, String outputFilePath) {
        String[] cmd = {"-i", inputFilePath, outputFilePath};
        FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
            if (returnCode == Config.RETURN_CODE_SUCCESS) {
                Log.e("FFmpeg", "Conversion to WAV successed");

            } else {
                Log.e("FFmpeg", "Conversion to WAV failed");
            }
        });
    }

    //开始录音
    private void startRecording() {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(filename + ".3gp");
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                Log.d("TAG", "Recording started");
            } catch (IOException e) {
                Log.e("TAG", "prepare() failed");
            } catch (RuntimeException e) {
                Log.e("TAG", "start() failed");
            }
        }
    }

    //结束录音
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException stopException) {
                Log.e("TAG", "stop() failed");
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                Log.d("TAG", "Recording stopped");
            }
        }
    }

    //展示提示信息
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //设置viusualizer
    private void setupVisualizer() {
        int audioSessionId = mprec.getAudioSessionId();
        if (audioSessionId != AudioManager.ERROR) {
            visualizer = new Visualizer(audioSessionId);
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    float amplitude = getAmplitudeFromWaveform(waveform);

                    audioVisualizerSphereView.setAmplitude(amplitude);
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {

                }
            }, Visualizer.getMaxCaptureRate(), true, false);

            visualizer.setEnabled(true);
        }
    }

    //获得音频Amplitude
    private float getAmplitudeFromWaveform(byte[] waveform) {
        float sum = 0;
        for (byte wave : waveform) {
            sum += Math.abs(wave);
        }
        float amplitude = sum / waveform.length;
        amplitude = amplitude / 128f;

        return amplitude;
    }

    //更新时间
    private void updateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        timeTextView.setText(currentTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable); // 停止更新
        mprec.reset();
    }

    //打开相册
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivity(intent);
    }

    //QR
    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("scan a QR code");
        integrator.setOrientationLocked(true);
        integrator.setCaptureActivity(CaptureActivity.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                String qrData = result.getContents();
                // 解析二维码数据
                String ssid = parseSsidFromQrData(qrData);
                String password = parsePasswordFromQrData(qrData);
                boolean isHidden = parseIsHiddenFromQrData(qrData);

                connectToWifi(ssid, password, isHidden);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String parseSsidFromQrData(String qrData) {
        // 解析逻辑
        String ssid = "";
        // 例子: 从二维码数据中提取SSID
        String[] parts = qrData.split(";");
        for (String part : parts) {
            if (part.startsWith("S:")) {
                ssid = part.substring(2);
            }
        }
        return ssid;
    }

    private String parsePasswordFromQrData(String qrData) {
        // 解析逻辑
        String password = "";
        // 例子: 从二维码数据中提取密码
        String[] parts = qrData.split(";");
        for (String part : parts) {
            if (part.startsWith("P:")) {
                password = part.substring(2);
            }
        }
        return password;
    }

    private boolean parseIsHiddenFromQrData(String qrData) {
        // 解析逻辑
        boolean isHidden = false;
        // 例子: 从二维码数据中提取是否隐藏
        String[] parts = qrData.split(";");
        for (String part : parts) {
            if (part.startsWith("H:")) {
                isHidden = part.substring(2).equals("true");
            }
        }
        return isHidden;
    }

    //链接wifi
    private void connectToWifi(String ssid, String password, boolean isHidden) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 使用 WifiNetworkSpecifier 连接 Wi-Fi
            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
            builder.setSsid(ssid);
            if (password != null && !password.isEmpty()) {
                builder.setWpa2Passphrase(password);
            }
            builder.setIsHiddenSsid(isHidden);

            WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(wifiNetworkSpecifier)
                    .build();

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    // 连接成功
                    connectivityManager.bindProcessToNetwork(network);
                    Toast.makeText(MainActivity.this, "Connected to " + ssid, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    // 连接失败
                    Toast.makeText(MainActivity.this, "Failed to connect to " + ssid, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // 对于低于 Android 10 的版本
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "\"" + ssid + "\"";
            if (password != null && !password.isEmpty()) {
                wifiConfig.preSharedKey = "\"" + password + "\"";
            } else {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }

            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            int networkId = wifiManager.addNetwork(wifiConfig);
            if (networkId != -1) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(networkId, true);
                wifiManager.reconnect();
                Toast.makeText(this, "Connecting to " + ssid + "...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to configure network.", Toast.LENGTH_SHORT).show();
            }
        }
    }


}