package com.example.jarvis_layout;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MusicPlayerActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView nextIv, playIv, lastIv, musicIcon;
    TextView singerTv, songTv;
    RecyclerView musicRv;

    // 数据源
    List<LocalMusicBean> metaDatas;
    private LocalMusicAdapter adapter;
    private int position;

    //记录当前正在播放的音乐的位置
    int currentPlayPosition = -1;
    //记录暂停音乐时进度条的位置
    int currentPausePositionInSong = 0;

    MediaPlayer mediaPlayer;

    private static final int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.music_player_activity_main);

        getWindow().getDecorView().post(() -> hideSystemBars());//隐藏系统栏
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button button = findViewById(R.id.cancel);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MusicPlayerActivity.this,MainActivity.class);
                stopMusic();
                startActivity(intent);
            }
        });


        initView();
        mediaPlayer = new MediaPlayer();
        metaDatas = new ArrayList<>();
        // 创建适配器
        adapter = new LocalMusicAdapter(this, metaDatas);
        musicRv.setAdapter(adapter);
        //设置布局管理器
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        musicRv.setLayoutManager(manager);
//        requestFilePermission();
        //加载本地数据源
        loadLocalMusicData();
        // 设置每一项的点击事件
        setEventListener();
    }

//    private void requestFilePermission(){
//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_PERMISSION_CODE);
//    }

    private void setEventListener() {
        // 设置每一项的点击事件
        adapter.setOnItemClickListener(new LocalMusicAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                currentPlayPosition = position;
                LocalMusicBean musicBean = metaDatas.get(position);

                playMusicInMusicBean(musicBean);

                // 播放完自动下一首
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (currentPlayPosition == metaDatas.size() - 1) {
                            currentPlayPosition = 0;
                        } else {
                            currentPlayPosition = currentPlayPosition + 1;
                        }
                        LocalMusicBean nextBean = metaDatas.get(currentPlayPosition);
                        playMusicInMusicBean(nextBean);
                    }
                });
            }
        });
    }

    private void playMusicInMusicBean(LocalMusicBean musicBean) {
//        根据传入对象播放音乐
        // 设置底部显示的歌手名称和歌曲名
        singerTv.setText(musicBean.getSinger());
        songTv.setText(musicBean.getSong());
        stopMusic();

        // 重置多媒体播放器
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(musicBean.getPath());
            playMusic();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void pauseMusic() {
        // 暂停音乐
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            currentPausePositionInSong = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            playIv.setImageResource(R.mipmap.icon_play);
        }
    }

    private void playMusic() {
        // 播放音乐
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            if (currentPausePositionInSong == 0) {
                try {
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                mediaPlayer.seekTo(currentPausePositionInSong);
                mediaPlayer.start();
            }

            playIv.setImageResource(R.mipmap.icon_pause);
        }
    }

    private void stopMusic() {
        // 停止音乐的函数
        if (mediaPlayer != null) {
            currentPausePositionInSong = 0;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
            playIv.setImageResource(R.mipmap.icon_play);
        }
    }

    private void loadLocalMusicData() {
        //加载本地音乐文件到集合当中
        // 1. 获取contentResolver对象
        ContentResolver resolver = getContentResolver();
        // 2. 获取本地音乐存储的uri地址
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // 3. 开始查询
        Cursor cursor = resolver.query(uri, null, null, null, null);
        // 4. 遍历cursor对象
        int id = 0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String song = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String singer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                id++;
                String sid = String.valueOf(id);
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
                String time = sdf.format(new Date(duration));
                if (time.equals("00:00")) {
                    id--;
                    continue;
                } else {
                    // 将一行的数据封装到对象当中
                    LocalMusicBean bean = new LocalMusicBean(sid, song, singer, time, path);
                    metaDatas.add(bean);
                }
            }
            // 数据源发生更新，提示适配器更新
            adapter.notifyDataSetChanged();
        }
        assert cursor != null;
        cursor.close();
    }

    private void initView() {
        nextIv = findViewById(R.id.local_music_iv_next);
        playIv = findViewById(R.id.local_music_iv_play);
        lastIv = findViewById(R.id.local_music_iv_last);
        singerTv = findViewById(R.id.local_music_tv_singer);
        songTv = findViewById(R.id.local_music_tv_song);
        musicRv = findViewById(R.id.local_music_rv);

        musicIcon = findViewById(R.id.local_music_iv_icon);

        nextIv.setOnClickListener(this);
        lastIv.setOnClickListener(this);
        playIv.setOnClickListener(this);

        musicIcon.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        int nowId = view.getId();
        if (nowId == R.id.local_music_iv_next) {
            if (currentPlayPosition == metaDatas.size() - 1) {
                currentPlayPosition = 0;
            } else {
                currentPlayPosition = currentPlayPosition + 1;
            }
            LocalMusicBean nextBean = metaDatas.get(currentPlayPosition);
            playMusicInMusicBean(nextBean);
        } else if (nowId == R.id.local_music_iv_play) {
            if (currentPlayPosition == -1) {
                // 并音乐在播放
                Toast.makeText(this, "请选择您想要播放的音乐", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mediaPlayer.isPlaying()) {
                // 此时处于播放状态，需要暂停音乐
                pauseMusic();
            } else {
                // 处于暂停播放状态，需要开始播放
                playMusic();
            }
        } else if (nowId == R.id.local_music_iv_last) {
            if (currentPlayPosition == 0) {
                currentPlayPosition = metaDatas.size() - 1;
            } else {
                currentPlayPosition = currentPlayPosition - 1;
            }
            LocalMusicBean lastBean = metaDatas.get(currentPlayPosition);
            playMusicInMusicBean(lastBean);
        }else if(nowId == R.id.local_music_iv_icon){
            Intent intent = new Intent(this, LyricView.class);
            startActivity(intent);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusic();
    }
}