package com.example.musicplayer.players.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.example.musicplayer.players.dialog.adapter.ChoosePlaylistAdapter;

public class ChoosePlaylistDialog extends Dialog {
    private RecyclerView choose_music_rv;
    private ChoosePlaylistAdapter choosePlaylistAdapter;
    private int position;
    private String launchFrom;
    public ChoosePlaylistDialog(@NonNull Context context , int position , String launchFrom) {
        super(context , R.style.DialogBaseStyle);
        //歌曲在搜索、本地、歌单列表等地方的位置
        this.position = position;
        this.launchFrom = launchFrom;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_choose_playlist);
        initView();
    }

    public void initView(){
        CRUD crud = new CRUD("playlist" , getContext());
        crud.retrievePlaylist("created");
        choose_music_rv = findViewById(R.id.choose_playlist_rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        choose_music_rv.setLayoutManager(layoutManager);
        choosePlaylistAdapter = new ChoosePlaylistAdapter(getContext() , position , launchFrom);
        choose_music_rv.setAdapter(choosePlaylistAdapter);
    }
}
