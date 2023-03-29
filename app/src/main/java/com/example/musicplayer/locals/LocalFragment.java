package com.example.musicplayer.locals;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Bean.SongBean;
import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.musicActions.MusicActionsActivity;
import com.example.musicplayer.R;

public class LocalFragment extends Fragment implements View.OnClickListener{
    private RecyclerView rv_local_music;
    private LocalSongAdapter localSongAdapter;
    private ImageButton local_choose_music , local_play;

    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.local_music_fragment, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initView(view);
        //获取本地歌曲,注意先判断一下localSongList是否为空避免重复加载
        getSongs(getContext());
        initAdapter(view);
    }

    public void initView(View view){
        local_choose_music = view.findViewById(R.id.local_choose_music);
        local_choose_music.setOnClickListener(this);
        local_play = view.findViewById(R.id.local_play);
        local_play.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.local_choose_music:
                Intent toMusicActionsIntent = new Intent(getActivity() , MusicActionsActivity.class);
                toMusicActionsIntent.putExtra("musicActionMode" , "local");
                startActivity(toMusicActionsIntent);
                break;
            case R.id.local_play:
                if (Constant.mediaPlayer.isPlaying()){
                    Constant.mediaPlayer.stop();
                }
                Constant.currentPlayList.clear();
                Constant.currentPosition = 0;
                Constant.currentPlayList.addAll(Constant.localSongList);
                Intent startPlayingIntent = new Intent( getActivity() , PlayMusicService.class);
                startPlayingIntent.putExtra("action" , PlayMusicService.NEW_SONG);
                getActivity().startService(startPlayingIntent);
                break;
        }
    }

    public void initAdapter(View view){
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rv_local_music = view.findViewById(R.id.rv_local_music);
        rv_local_music.setLayoutManager(layoutManager);
        localSongAdapter = new LocalSongAdapter(Constant.localSongList, getContext());
        rv_local_music.setAdapter(localSongAdapter);
    }

    @SuppressLint("Range")
    public void getSongs(Context context){
        if (Constant.localSongList.size() == 0){
            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI , null ,
                    MediaStore.Audio.Media.IS_MUSIC , null ,
                    MediaStore.Audio.Media.DEFAULT_SORT_ORDER
            );

            if(cursor.moveToFirst()){
                int i = 0;
                while (cursor.moveToNext()){
                    //获取歌名，演唱者，路径，时长
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));//Uri地址
                    int duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    String picUri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID));
                    SongBean songBean = new SongBean(name , artist , id , picUri , duration , i ,"local");
                    Constant.localSongList.add(songBean);
                    i++;
                }
                cursor.close();
            }
        }
    }

}
