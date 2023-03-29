package com.example.musicplayer.players.player;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.R;

public class CurrentPlayListDialog extends Dialog implements View.OnClickListener{
    private TextView current_play_number , current_describe;
    private ImageButton current_play_mode , current_clear;
    private RecyclerView current_rv;
    public static CurrentPlayListAdapter currentPlayListAdapter;
    private CurrentPlayListDialogReceiver currentPlayListDialogReceiver;
    private IntentFilter currentPlayListFilter;
    public CurrentPlayListDialog(@NonNull Context context) {
        super(context , R.style.DialogBaseStyle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_current_playlist);
        initView();
        initReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getContext().unregisterReceiver(currentPlayListDialogReceiver);
    }

    @SuppressLint("SetTextI18n")
    public void initView(){
        current_play_number = findViewById(R.id.current_play_number);
        current_describe = findViewById(R.id.current_describe);
        current_clear = findViewById(R.id.current_clear);
        current_clear.setOnClickListener(this);
        current_play_mode = findViewById(R.id.current_play_mode);
        current_play_mode.setOnClickListener(this);
        current_rv = findViewById(R.id.current_rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        current_rv.setLayoutManager(layoutManager);
        currentPlayListAdapter = new CurrentPlayListAdapter(getContext());
        current_rv.setAdapter(currentPlayListAdapter);

        current_play_number.setText("当前播放" + "(" + Constant.currentPlayList.size() + ")");
        displayPlayMode();
    }

    public void initReceiver(){
        currentPlayListDialogReceiver = new CurrentPlayListDialogReceiver();
        currentPlayListFilter = new IntentFilter();
        currentPlayListFilter.addAction("refreshDialog");
        getContext().registerReceiver(currentPlayListDialogReceiver , currentPlayListFilter);
    }


    public void displayPlayMode(){
        switch (Constant.currentPlayMode){
            case 0:
                current_play_mode.setImageResource(R.drawable.ic_baseline_repeat_24_grey);
                current_describe.setText("列表循环");
                break;
            case 1:
                current_play_mode.setImageResource(R.drawable.ic_baseline_repeat_one_24_grey);
                current_describe.setText("单曲循环");
                break;
            case 2:
                current_play_mode.setImageResource(R.drawable.ic_baseline_compare_arrows_24_grey);
                current_describe.setText("随机播放");
                break;
        }
    }

    @SuppressLint({"NotifyDataSetChanged", "NonConstantResourceId"})
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.current_play_mode:
                Constant.currentPlayMode ++;
                //如果大于2则重置为0（列表循环）
                if (Constant.currentPlayMode > 2){
                    Constant.currentPlayMode = 0;
                }else if (Constant.currentPlayMode == 2){
                    Intent in = new Intent(getContext() , PlayMusicService.class);
                    in.putExtra("action" , PlayMusicService.RANDOM_PLAYING);
                    getContext().startService(in);
                }
                displayPlayMode();
                break;
            case R.id.current_clear:
                //点击后终止当前播放的音乐，清空播放列表，结束播放器页面
                dismiss();
                Intent finishIntent = new Intent("playerActivity");
                finishIntent.putExtra("activityAction" , "clearList");
                getContext().sendBroadcast(finishIntent);
                //刷新底部播放栏界面
                Intent intent = new Intent("updateStatus");
                getContext().sendBroadcast(intent);
                break;
        }
    }

    class CurrentPlayListDialogReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String which = intent.getStringExtra("which");
            if (which.equals("describe")){
                current_play_number.setText("当前播放" + "(" + Constant.currentPlayList.size() + ")");
            }else if (which.equals("status")){
                currentPlayListAdapter.notifyDataSetChanged();
            }
        }
    }
}
