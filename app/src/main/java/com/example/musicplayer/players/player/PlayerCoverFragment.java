package com.example.musicplayer.players.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.players.GaussianBlurUtil;

public class PlayerCoverFragment extends Fragment {
    private ConstraintLayout cover_background;
    private ImageView album_cover;
    private ChangeSongReceiver changeSongReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private IntentFilter changeSongFilter;
    public static final int LOADING_COVER = 1;

    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case LOADING_COVER:
                    break;
            }
        }
    };
    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_player_cover, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initView(view);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(changeSongReceiver);
    }

    public void initView(View view){
        album_cover = view.findViewById(R.id.album_cover);
        cover_background = view.findViewById(R.id.cover_background);
        //加载封面
        initCover();
        initReceiver();
    }

    public void initReceiver(){
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        changeSongReceiver = new ChangeSongReceiver();
        changeSongFilter = new IntentFilter();
        changeSongFilter.addAction("changeSong");
        getContext().registerReceiver(changeSongReceiver , changeSongFilter);
    }

    public void initCover(){
        //设置背景
        Glide.with(this)
                .asBitmap()
                .load(Constant.currentPlayList.get(Constant.currentPosition).picUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        setCoverBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
        //设置封面
        Glide.with(this)
                .load(Constant.currentPlayList.get(Constant.currentPosition).picUrl)
                .placeholder(R.drawable.ic_baseline_photo_24)
                .error(R.drawable.ic_baseline_photo_24)
                .into(album_cover);
    }

    public void setCoverBackground(Bitmap resource){
        cover_background.setBackground(GaussianBlurUtil.BoxBlurFilter(resource));
    }

    //更新封面
    class ChangeSongReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            initCover();
        }
    }
}
