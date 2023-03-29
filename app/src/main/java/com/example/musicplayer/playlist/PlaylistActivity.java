package com.example.musicplayer.playlist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Bean.SongBean;
import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.example.musicplayer.musicActions.MusicActionsActivity;
import com.example.musicplayer.players.OnPlayingFragment;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlaylistActivity extends AppCompatActivity implements View.OnClickListener{
    private ImageButton playlist_play_all , playlist_actions;
    private TextView playlist_describe;
    private ImageView playlist_bar_image;
    private RecyclerView playlist_rv;
    private Toolbar playlist_bar;
    private CollapsingToolbarLayout playlist_ctb;
    private PlaylistAdapter playlistAdapter;
    private String playlistType;
    private long playlistId;
    private OnPlayingFragment onPlayingFragment;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        Log.d("PlaylistActivity" , "PlaylistActivity启动！");
        initView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        new CRUD("lastPlay" , this).saveCurrentPlaylist();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void initView(){
        Intent in = getIntent();
        playlistType = in.getStringExtra("playlistType");
        playlist_bar_image = findViewById(R.id.playlist_bar_image);
        playlist_actions = findViewById(R.id.playlist_actions);
        playlist_actions.setOnClickListener(this);
        playlist_play_all = findViewById(R.id.playlist_play_all);
        playlist_play_all.setOnClickListener(this);
        playlist_describe = findViewById(R.id.playlist_describe);
        playlist_describe.setText("播放全部" + "(" + Constant.musicList.size() + ")");
        playlist_ctb = findViewById(R.id.playlist_ctb);
        playlist_ctb.setTitle(in.getStringExtra("name"));

        Glide.with(this).load(in.getStringExtra("coverImgUrl")).into(playlist_bar_image);

        playlist_bar = findViewById(R.id.playlist_bar);
        setSupportActionBar(playlist_bar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        playlist_rv = findViewById(R.id.playlist_rv);
        playlist_rv.setLayoutManager(layoutManager);
        playlistAdapter = new PlaylistAdapter(this , Constant.musicList , "playlist");
        playlist_rv.setAdapter(playlistAdapter);

        onPlayingFragment = new OnPlayingFragment();
        replaceFragment(onPlayingFragment);
    }

    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.playlist_toolbar , menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.playlist_search:
                Intent in = new Intent(this , SearchMusicInPlaylistActivity.class);
                startActivity(in);
                break;
            case android.R.id.home:
                finishAfterTransition();
                break;
        }
        return true;
    }

    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.playlist_frag_container , fragment);
        transaction.commit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playlist_actions:
                Intent in = new Intent(this , MusicActionsActivity.class);
                in.putExtra("musicActionMode" , playlistType);
                startActivity(in);
                break;
            case R.id.playlist_play_all:
                //先把至少有一个音乐准备完成设定为false
                boolean isAtLeastOneReady = false;
                //清空当前播放列表
                if (!Constant.currentPlayList.isEmpty()){
                    Constant.currentPlayList.clear();
                }
                int musicNum = Constant.musicList.size();
                int threadNum = setThreadNumber(musicNum);
                ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
                CountDownLatch latch = new CountDownLatch(musicNum);
                Log.d("PlaylistActivity" , "开始解析歌曲");
                for (int i = 0 ; i < musicNum ; i ++){
                    final int tmpPosition = i;
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            Callback callback = new Callback() {
                                @Override
                                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                                }

                                @Override
                                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                    parseSingleJson(response.body().string() , tmpPosition);
                                }
                            };
                            acquireMusicUrl(formingUrl(tmpPosition) , callback);
                        }
                    });
                }
                try {
                    //只要至少有一个线程解析出结果了就可以继续下面的步骤
                    latch.await(1, TimeUnit.SECONDS);
                    //判断有没有线程已经进行完成，如果还没有，则开始播放第一首音乐，如果有，则在播放列表中等待
                    if (!isAtLeastOneReady){
                        Log.d("PlaylistActivity" , "开始播放");
                        Intent startPlayingIntent = new Intent(PlaylistActivity.this , PlayMusicService.class);
                        startPlayingIntent.putExtra("action" , PlayMusicService.NEW_SONG);
                        startService(startPlayingIntent);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    //根据数据数量判断开启的线程数量
    public int setThreadNumber(int dataNumbers){
        int threadNumber = 1;
        if (dataNumbers > 150 && dataNumbers <= 320){
            threadNumber = 2;
        }else if (dataNumbers > 320 && dataNumbers <= 470){
            threadNumber = 3;
        }else if (dataNumbers > 470){
            threadNumber = 4;
        }
        return threadNumber;
    }

    public String formingUrl(int position){
        String musicID = "?id=" + String.valueOf(Constant.musicList.get(position).id);
        String url = "http://wyyapi.itaemobile.top/song/url/v1" + musicID + Constant.defaultBrLevel;
        return url;
    }

    public void acquireMusicUrl(String url , Callback callback){
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                client.newCall(request).enqueue(callback);
            }
        }).start();
    }

    public void parseSingleJson(String responseData , int position){
        //获取数据
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(responseData);
            //最外层data数组
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            //数组中的数据
            JSONObject jsonObject1 = jsonArray.getJSONObject(0);
            //获取url , 标题 ， 歌手等数据
            String url = jsonObject1.getString("url");
            String picUrl = Constant.musicList.get(position).al.picUrl;
            String title = Constant.musicList.get(position).name;
            String artist = displayMultipleArtist(position);
            int time = jsonObject1.getInt("time");
            int id = jsonObject1.getInt("id");
            SongBean songBean = new SongBean(title , artist , url , picUrl , time , id ,"web");
            try {
                //这个地方好像会报数组越界的错误
                Constant.currentPlayList.add(songBean);
            }catch (Exception e){
                Thread.sleep(10);
                Constant.currentPlayList.add(songBean);
            }
        } catch (JSONException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String displayMultipleArtist(int position){
        String artists = "";
        //如果有一名以上的歌手，则循环调出歌手名
        for (int i = 0 ; i < Constant.musicList.get(position).getAr().size() ; i++){
            artists += Constant.musicList.get(position).getAr().get(i).getName();
            artists += "   ";
        }
        return artists;
    }
}
