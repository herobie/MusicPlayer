package com.example.musicplayer.musicActions;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MusicActionsActivity extends AppCompatActivity implements View.OnClickListener{
    private RecyclerView actions_rv;
    private BottomNavigationView actions_navigation;
    private Button actions_select_all;
    private ImageButton actions_back;
    private TextView actions_title;
    private MusicActionsAdapter musicActionsAdapter;
    private Button actions_add_next  , actions_add_collection , actions_delete;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_actions);
        initView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        new CRUD("lastPlay" , this).saveCurrentPlaylist();
    }

    public void initView(){
        //获取是从哪个地方启动的,本地歌曲和网络歌曲的操作有些不同,见localFragment点击方法
        Intent intent = getIntent();
        String musicActionMode = intent.getStringExtra("musicActionMode");
        actions_select_all = findViewById(R.id.actions_select_all);
        actions_select_all.setOnClickListener(this);
        actions_back = findViewById(R.id.actions_back);
        actions_back.setOnClickListener(this);
        actions_title = findViewById(R.id.actions_title);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        actions_rv = findViewById(R.id.actions_rv);
        actions_rv.setLayoutManager(linearLayoutManager);
        actions_delete = findViewById(R.id.actions_delete);
        actions_delete.setOnClickListener(this);
        actions_add_collection = findViewById(R.id.actions_add_collection);
        actions_add_collection.setOnClickListener(this);
        actions_add_next = findViewById(R.id.actions_add_next);
        actions_add_next.setOnClickListener(this);
        //根据本地或是网络、收藏（这俩都是保存的song类)来判断初始化模式
        if (musicActionMode.equals("local")){
            musicActionsAdapter = new MusicActionsAdapter(musicActionMode , this);
        }else if (musicActionMode.equals("web")){
            musicActionsAdapter = new MusicActionsAdapter(musicActionMode , Constant.musicList , this);
        }else if (musicActionMode.equals("save")){
            musicActionsAdapter = new MusicActionsAdapter(musicActionMode , Constant.musicList , this);
        }
        actions_rv.setAdapter(musicActionsAdapter);
    }

    public void initNavigation(boolean isDeleteAvailable){
        actions_navigation.getMenu().add(0 , 100 , 0 , "下一首播放");
        actions_navigation.getMenu().findItem(100).setIcon(R.drawable.ic_baseline_ondemand_video_24);
        actions_navigation.getMenu().add(0 , 101 , 0 , "添加到播放列表");
        actions_navigation.getMenu().findItem(101).setIcon(R.drawable.ic_baseline_queue_music_24);
        actions_navigation.getMenu().add(0 , 102 , 0 , "收藏到歌单");
        actions_navigation.getMenu().findItem(102).setIcon(R.drawable.ic_baseline_create_new_folder_24);
        //判断该页面能否使用删除功能(只有收藏的歌单可以)
        if (isDeleteAvailable){
            actions_navigation.getMenu().add(0 , 103 , 0 , "删除");
            actions_navigation.getMenu().findItem(103).setIcon(R.drawable.ic_baseline_delete_24);
        }
        actions_navigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent in = new Intent("musicAction");
                switch (item.getItemId()){
                    case 100:
                        in.putExtra("action" , "playNext");
                        break;
                    case 101:
                        in.putExtra("action" , "addPlaylist");
                }
                sendBroadcast(in);
                return true;
            }
        });
    }

    @Override
    public void onClick(View v) {
        Intent in = new Intent("musicAction");
        switch (v.getId()){
            case R.id.actions_back:
                in.putExtra("action" , "quit");
                sendBroadcast(in);
                finish();
                break;
            case R.id.actions_add_next:
                in.putExtra("action" , "addPlaylist");
                sendBroadcast(in);
                break;
        }

    }


}
