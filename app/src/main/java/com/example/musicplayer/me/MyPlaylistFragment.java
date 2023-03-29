package com.example.musicplayer.me;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.example.musicplayer.me.created.CreatedPlaylistAdapter;
import com.example.musicplayer.me.saved.SavedPlaylistAdapter;

public class MyPlaylistFragment extends Fragment implements View.OnClickListener{
    private TextView me_type;
    private ImageButton me_add , me_action;
    private RecyclerView me_rv;
    private CreatedPlaylistAdapter createdPlaylistAdapter;
    private SavedPlaylistAdapter savedPlaylistAdapter;
    private RefreshAdapterReceiver refreshAdapterReceiver;
    private PlaylistTypeChangeReceiver playlistTypeChangeReceiver;
    private IntentFilter playlistTypeChangeFilter , refreshAdapterFilter;
    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_me_playlist, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initView(view);
        initReceiver();
        initSQL();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        savedPlaylistAdapter.notifyDataSetChanged();
        createdPlaylistAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(playlistTypeChangeReceiver);
        getContext().unregisterReceiver(refreshAdapterReceiver);
    }

    public void initView(View view){
        me_type = view.findViewById(R.id.me_type);
        me_add = view.findViewById(R.id.me_add);
        me_add.setOnClickListener(this);
        me_action = view.findViewById(R.id.me_action);
        me_action.setOnClickListener(this);
        me_rv = view.findViewById(R.id.me_rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        me_rv.setLayoutManager(layoutManager);
    }

    public void initReceiver(){
        playlistTypeChangeReceiver = new PlaylistTypeChangeReceiver();
        playlistTypeChangeFilter = new IntentFilter();
        playlistTypeChangeFilter.addAction("changePlaylistType");
        getContext().registerReceiver(playlistTypeChangeReceiver , playlistTypeChangeFilter);

        refreshAdapterReceiver = new RefreshAdapterReceiver();
        refreshAdapterFilter = new IntentFilter();
        refreshAdapterFilter.addAction("refreshAdapter");
        getContext().registerReceiver(refreshAdapterReceiver , refreshAdapterFilter);
    }

    public void initSQL(){
        CRUD crud = new CRUD("playlist" , getContext());
        //先判断我最喜欢的音乐这一歌单有没有被创建，如果没有就创建一个
        if (!crud.isFavoritePlaylistExist()){
            ContentValues values = new ContentValues();
            values.put("name" , "我喜欢的音乐");
            values.put("nickname" , Constant.activeAccount);
            values.put("id" , 0);
            values.put("coverImgUrl" , "null");
            values.put("type" , "created");
            values.put("orderBy" , "DESC");
            crud.add(values);
            values.clear();
        }
        crud.retrievePlaylist("created");
        createdPlaylistAdapter = new CreatedPlaylistAdapter(getContext() , Constant.myPlaylist);
        savedPlaylistAdapter = new SavedPlaylistAdapter(getContext());
        me_rv.setAdapter(createdPlaylistAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.me_add:
                NewPlaylistDialog newPlaylistDialog = new NewPlaylistDialog(getContext() , "createNewPlaylist");
                newPlaylistDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        createdPlaylistAdapter.notifyDataSetChanged();
                    }
                });
                newPlaylistDialog.show();
                break;
        }
    }

    class RefreshAdapterReceiver extends BroadcastReceiver{

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onReceive(Context context, Intent intent) {
            createdPlaylistAdapter.notifyDataSetChanged();
            savedPlaylistAdapter.notifyDataSetChanged();
        }
    }

    class PlaylistTypeChangeReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra("type");
            CRUD crud = new CRUD("playlist" , getContext());
            if (type.equals("created")){
                me_type.setText("创建的歌单");
                me_add.setVisibility(View.VISIBLE);
                crud.retrievePlaylist("created");
                me_rv.setAdapter(createdPlaylistAdapter);
            }else {
                me_type.setText("收藏的歌单");
                me_add.setVisibility(View.INVISIBLE);
                crud.retrievePlaylist("save");
                me_rv.setAdapter(savedPlaylistAdapter);
            }
        }
    }
}
