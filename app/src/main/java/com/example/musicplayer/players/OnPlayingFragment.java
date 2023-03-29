package com.example.musicplayer.players;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.example.musicplayer.players.player.CurrentPlayListDialog;
import com.example.musicplayer.players.player.PlayerActivity;

import de.hdodenhof.circleimageview.CircleImageView;

public class OnPlayingFragment extends Fragment implements View.OnClickListener{
    private String picUrl ,  title , artist;
    int position;
    private ConstraintLayout onPlaying_view;
    private CircleImageView onPlaying_photo;
    private TextView onPlaying_title;
    private ImageButton onPlaying_playList , onPlaying_play;
    private OnPlayingFragmentReceiver onPlayingFragmentReceiver;
    private ClearListReceiver clearListReceiver;
    private IntentFilter onPlayingFragmentFilter , clearListFilter;
    public OnPlayingFragment(String title , String artist , String picUrl){
        this.title = title;
        this.artist = artist;
        this.picUrl = picUrl;
    }

    public OnPlayingFragment(){

    }

    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.onplaying_navigation, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initView(view);
        initReceiver();
        Log.d("OnPlayingFragment" , "底部播放界面加载完毕");
    }

    @Override
    public void onPause() {
        super.onPause();
        new CRUD("lastPlay" , getContext()).saveCurrentPlaylist();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(onPlayingFragmentReceiver);
        getContext().unregisterReceiver(clearListReceiver);
    }

    public void initView(View view){
        onPlaying_view = view.findViewById(R.id.onplaying_view);
        onPlaying_view.setOnClickListener(this);

        onPlaying_photo = view.findViewById(R.id.onplaying_photo);
        //这几个try/catch都是先检查currentPlaylist里有没有歌，如果没有歌则这个fragment的样式全部设为默认样式
        try {
            Glide.with(this)
                    .load(Constant.currentPlayList.get(Constant.currentPosition).picUrl)
                    .placeholder(R.drawable.ic_baseline_photo_24)
                    .error(R.drawable.ic_baseline_photo_24)
                    .into(onPlaying_photo);
        }catch (Exception e){
            onPlaying_photo.setImageResource(R.drawable.ic_baseline_music_note_24);
        }
        onPlaying_title = view.findViewById(R.id.onplaying_title);

        try {
            onPlaying_title.setText(Constant.currentPlayList.get(Constant.currentPosition).getTitle() + " — " + Constant.currentPlayList.get(Constant.currentPosition).getArtist());
        }catch (Exception e){
            onPlaying_title.setText(title + " — " + artist);
        }

        onPlaying_playList = view.findViewById(R.id.onplaying_playList);
        onPlaying_playList.setOnClickListener(this);

        onPlaying_play = view.findViewById(R.id.onplaying_play);
        onPlaying_play.setOnClickListener(this);

        try {
            if (Constant.mediaPlayer.isPlaying()){
                onPlaying_play.setImageResource(R.drawable.ic_outline_pause_circle_24);
            }else {
                onPlaying_play.setImageResource(R.drawable.ic_outline_play_circle_24);
            }
        }catch (Exception e){
            onPlaying_play.setImageResource(R.drawable.ic_outline_play_circle_24);
        }
    }

    public void initReceiver(){
        onPlayingFragmentReceiver = new OnPlayingFragmentReceiver();
        onPlayingFragmentFilter = new IntentFilter();
        onPlayingFragmentFilter.addAction("updateStatus");
        getContext().registerReceiver(onPlayingFragmentReceiver , onPlayingFragmentFilter);

        clearListReceiver = new ClearListReceiver();
        clearListFilter = new IntentFilter();
        clearListFilter.addAction("playerActivity");
        getContext().registerReceiver(clearListReceiver , clearListFilter);
    }

    @Override
    public void onClick(View v) {
        //判断播放列表中有无歌曲，必须有歌曲才能操作
        if (Constant.currentPlayList.size() > 0){
            switch (v.getId()){
                case R.id.onplaying_view:
                    Intent ToPlayerIntent = new Intent(getActivity() , PlayerActivity.class);
                    startActivity(ToPlayerIntent , ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
                    break;
                case R.id.onplaying_play:
                    //根据播放状态显示暂停或继续
                    Intent in = new Intent(getContext() , PlayMusicService.class);
                    if (!Constant.isInitLastPlayMusic){
                        in.putExtra("action" , PlayMusicService.MUSIC_PREPARE);
                        getContext().startService(in);
                        Log.d("OnPlayingFragment" , "启动播放上次退出时的音乐");
                        Constant.isInitLastPlayMusic = true;
                    }
                    if (Constant.mediaPlayer.isPlaying()){
                        in.putExtra("action" , PlayMusicService.MUSIC_PAUSE);
                    }else{
                        in.putExtra("action" , PlayMusicService.MUSIC_RESUME);
                    }
                    getContext().startService(in);
                    break;
                case R.id.onplaying_playList:
                    PlayerActivity.currentPlayListDialog = new CurrentPlayListDialog(getContext());
                    if (!PlayerActivity.currentPlayListDialog.isShowing()){
                        PlayerActivity.currentPlayListDialog.show();
                    }
                    break;
            }
        }
    }

    //更新底部音乐播放栏信息
    class OnPlayingFragmentReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                Glide.with(context)
                        .load(Constant.currentPlayList.get(Constant.currentPosition).picUrl)
                        .placeholder(R.drawable.ic_baseline_photo_24)
                        .error(R.drawable.ic_baseline_photo_24)
                        .into(onPlaying_photo);
                String title = Constant.currentPlayList.get(Constant.currentPosition).title;
                String artist = Constant.currentPlayList.get(Constant.currentPosition).artist;
                onPlaying_title.setText(title + " — " + artist);
                if (!Constant.mediaPlayer.isPlaying()){
                    onPlaying_play.setImageResource(R.drawable.ic_outline_play_circle_24);
                }else {
                    onPlaying_play.setImageResource(R.drawable.ic_outline_pause_circle_24);
                }
            }catch (Exception e){
                onPlaying_title.setText("暂无歌曲");
                onPlaying_play.setImageResource(R.drawable.ic_outline_play_circle_24);
                onPlaying_photo.setImageResource(R.drawable.ic_baseline_music_note_24);
            }
        }
    }

    class ClearListReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Constant.mediaPlayer.stop();
            Constant.currentPosition = 0;
            Constant.currentPlayList.clear();
            intent = new Intent("updateStatus");
            context.sendBroadcast(intent);
            PlayerActivity.currentPlayListDialog.dismiss();
        }
    }
}
