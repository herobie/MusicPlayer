package com.example.musicplayer.playlist;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Constant;
import com.example.musicplayer.R;

public class SearchMusicInPlaylistActivity extends AppCompatActivity implements View.OnClickListener , TextWatcher {
    private ImageButton playlist_search_back , playlist_search_delete;
    private EditText playlist_input;
    private RecyclerView playlist_search_rv;
    private PlaylistAdapter playlistAdapter;
    private long playlistId;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_search);
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void initView(){
        playlist_search_back = findViewById(R.id.playlist_search_back);
        playlist_search_back.setOnClickListener(this);
        playlist_search_delete = findViewById(R.id.playlist_search_delete);
        playlist_search_delete.setOnClickListener(this);
        playlist_input = findViewById(R.id.playlist_input);
        playlist_input.addTextChangedListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        playlist_search_rv = findViewById(R.id.playlist_search_rv);
        playlist_search_rv.setLayoutManager(layoutManager);
        playlistAdapter = new PlaylistAdapter(this , Constant.searchMusicInPlaylist , "searchPlaylist");
        playlist_search_rv.setAdapter(playlistAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playlist_search_back:
                finish();
                break;
            case R.id.playlist_search_delete:
                playlist_input.setText("");
                Constant.searchMusicInPlaylist.clear();
                break;

        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void afterTextChanged(Editable s) {
        //搜索
        Constant.searchMusicInPlaylist.clear();
        for (int i = 0 ; i < Constant.musicList.size() ; i++){
            if (Constant.musicList.get(i).name.contains(s.toString())){
                Constant.searchMusicInPlaylist.add(Constant.musicList.get(i));
            }
        }
        playlistAdapter.notifyDataSetChanged();
        if (!s.toString().equals("")){
            playlist_search_delete.setVisibility(View.VISIBLE);
        }else {
            playlist_search_delete.setVisibility(View.GONE);
        }
    }
}
