package com.example.musicplayer.me;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.musicplayer.Bean.playlist.Playlist;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;
import java.util.Random;

public class NewPlaylistDialog extends Dialog implements View.OnClickListener {
    private TextInputEditText me_create_new_playlist;
    private TextView me_create_warning;
    private Button me_create_cancel , me_create_confirm;
    private ImageButton me_create_clear;
    private String launchType;
    private long playlistId;
    private int position;
    public NewPlaylistDialog(@NonNull Context context , String launchType) {
        super(context , R.style.DialogBaseStyle);
        this.launchType = launchType;
    }

    public NewPlaylistDialog(@NonNull Context context , String launchType , long playlistId , int position) {
        super(context , R.style.DialogBaseStyle);
        this.launchType = launchType;
        this.playlistId = playlistId;
        this.position = position;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_create_new_playlist);
        initView();
    }

    public void initView(){
        me_create_confirm = findViewById(R.id.me_create_confirm);
        me_create_confirm.setOnClickListener(this);
        me_create_cancel = findViewById(R.id.me_create_cancel);
        me_create_cancel.setOnClickListener(this);
        me_create_warning = findViewById(R.id.me_create_warning);
        me_create_new_playlist = findViewById(R.id.me_create_new_playlist);
        me_create_clear = findViewById(R.id.me_create_clear);
        me_create_clear.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.me_create_cancel:
                dismiss();
                break;
            case R.id.me_create_confirm:
                if (launchType.equals("createNewPlaylist")){
                    createNewPlaylist();
                }else {
                    changePlaylistName();
                }
                Intent in = new Intent("refreshAdapter");
                getContext().sendBroadcast(in);
                dismiss();
                break;
        }
    }

    public void createNewPlaylist(){
        String name = Objects.requireNonNull(me_create_new_playlist.getText()).toString();
        //在[1,5000)随机生成一个独特的歌单id
        long id = 0;
        //如果生成了一个重复的，则再生成一次，直到不重复为止
        do{
            Random r = new Random();
            id = r.nextInt(4999) + 1;
        }while (new CRUD("playlist" , getContext()).isPlaylistIdExist(id));
        ContentValues values = new ContentValues();
        values.put("name" , name);
        values.put("nickname" , Constant.activeAccount);
        values.put("id" , id);
        values.put("type" , "created");
        new CRUD("playlist" , getContext()).add(values);
        values.clear();
        Playlist playlist = new Playlist(name , Constant.activeAccount , null , id , null);
        Constant.myPlaylist.add(Constant.myPlaylist.size(), playlist);
        Toast.makeText(getContext(), "创建成功！", Toast.LENGTH_SHORT).show();
        Log.d("NewPlaylistDialog" , "歌单创建成功！");
    }

    public void changePlaylistName(){
        String name = Objects.requireNonNull(me_create_new_playlist.getText()).toString();
        ContentValues values = new ContentValues();
        values.put("name" , name);
        new CRUD("playlist" , getContext()).updatePlaylistName(values , Constant.playlistId);
        values.clear();
        Toast.makeText(getContext(), "操作成功！", Toast.LENGTH_SHORT).show();
        Log.d("NewPlaylistDialog" , "歌单改名成功！");
    }
}
