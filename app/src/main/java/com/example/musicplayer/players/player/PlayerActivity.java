package com.example.musicplayer.players.player;

import static com.example.musicplayer.PlayMusicService.UPDATE_PROGRESS;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.transition.Slide;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.example.musicplayer.players.dialog.ChoosePlaylistDialog;

import java.text.SimpleDateFormat;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener , SeekBar.OnSeekBarChangeListener , MediaPlayer.OnBufferingUpdateListener {
    private ImageButton player_back , player_like , player_more_actions , player_play_mode , player_previous , player_play , player_next , player_playList;
    private TextView player_title , player_artist , player_end_time;
    public static TextView player_start_time;
    public static SeekBar player_duration;
    private ConstraintLayout player_view;
    private PlayerCoverFragment playerCoverFragment;
    private ChangeSongReceiver changeSongReceiver;
    private PlayerActivityReceiver playerActivityReceiver;
    private IntentFilter changeSongFilter , playerActivityFilter;
    private LocalBroadcastManager localBroadcastManager;
    private Context mContext = this;
    public static CurrentPlayListDialog currentPlayListDialog;
    public static Handler updateHandler = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message message){
            switch (message.what){
                case 7:
                    //避免突然空指针报错加个try/catch
                    try{
                        int progress = Constant.mediaPlayer.getCurrentPosition();
                        player_duration.setProgress(progress);
                        player_start_time.setText(toMinutes(progress));
                        if (Constant.mediaPlayer.isPlaying()){
                            PlayerActivity.updateHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS , 1000);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);//加一个启动动画
        setContentView(R.layout.activity_player);
        Intent in = new Intent(this , PlayMusicService.class);
        startService(in);
        getWindow().setEnterTransition(new Slide());
        getWindow().setExitTransition(new Slide());
        initView();
        initReceiver();
        replaceFragment(playerCoverFragment);
    }

    @Override
    protected void onPause() {
        super.onPause();
        new CRUD("lastPlay" , this).saveCurrentPlaylist();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(changeSongReceiver);
        this.unregisterReceiver(playerActivityReceiver);
    }

    public void initView(){
        player_back = findViewById(R.id.player_back);
        player_back.setOnClickListener(this);
        player_like = findViewById(R.id.player_like);
        //检查该歌曲是否存在于喜爱的歌单中，并根据存在状态设置不同ui
        if (new CRUD("music" , this).retrieveIfAlreadyInPlaylist(0 , Constant.currentPlayList.get(Constant.currentPosition).musicUrl , Constant.currentPlayList.get(Constant.currentPosition).getId())){
            player_like.setImageResource(R.drawable.ic_baseline_favorite_24_red);
        }else {
            player_like.setImageResource(R.drawable.ic_baseline_favorite_border_24);
        }
        player_like.setOnClickListener(this);
        player_more_actions = findViewById(R.id.player_more_actions);
        player_more_actions.setOnClickListener(this);
        player_play_mode = findViewById(R.id.player_play_mode);
        player_play_mode.setOnClickListener(this);
        player_previous = findViewById(R.id.player_previous);
        player_previous.setOnClickListener(this);
        player_play = findViewById(R.id.player_play);
        player_play.setOnClickListener(this);
        player_next = findViewById(R.id.player_next);
        player_next.setOnClickListener(this);
        player_playList = findViewById(R.id.player_playList);
        player_playList.setOnClickListener(this);

        player_title = findViewById(R.id.player_title);
        player_artist = findViewById(R.id.player_artist);
        player_start_time = findViewById(R.id.player_start_time);
        int progress = Constant.mediaPlayer.getCurrentPosition();
        player_start_time.setText(toMinutes(progress));
        player_end_time = findViewById(R.id.player_end_time);
        player_duration = findViewById(R.id.player_duration);
        player_duration.setOnSeekBarChangeListener(this);
        player_view = findViewById(R.id.player_view);

        playerCoverFragment = new PlayerCoverFragment();

        String title = Constant.currentPlayList.get(Constant.currentPosition).title;
        String artist = Constant.currentPlayList.get(Constant.currentPosition).artist;
        player_title.setText(title);
        player_artist.setText(artist);
        player_end_time.setText(toMinutes(Constant.currentPlayList.get(Constant.currentPosition).time));

        player_duration.setMax(Constant.currentPlayList.get(Constant.currentPosition).time);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            player_duration.setMin(0);
            player_duration.setProgress(Constant.mediaPlayer.getCurrentPosition());
        }
        if (Constant.mediaPlayer.isPlaying()){
            player_play.setImageResource(R.drawable.ic_baseline_pause_circle_outline_40_large);
        }else {
            player_play.setImageResource(R.drawable.ic_baseline_play_circle_outline_36_large);
        }
        changePlayMode(false);
        if (Constant.mediaPlayer.isPlaying()){
            Message message = new Message();
            message.what = 7;
            updateHandler.sendMessage(message);
        }
        if (!Constant.isInitLastPlayMusic){
            Intent in = new Intent(this, PlayMusicService.class);
            in.putExtra("action" , PlayMusicService.MUSIC_PREPARE);
            startService(in);
            Log.d("OnPlayingFragment" , "启动播放上次退出时的音乐");
            Constant.isInitLastPlayMusic = true;
        }
    }

    //将毫秒换算成几分几秒，返回换算出的结果
    public static String toMinutes(int millisecond){
//        int min = millisecond / 60000;
//        int sec = (millisecond / 1000) % 60;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        String time = sdf.format(millisecond);
        return time;
    }

    public void initReceiver(){
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        changeSongReceiver = new ChangeSongReceiver();
        playerActivityReceiver = new PlayerActivityReceiver();
        playerActivityFilter = new IntentFilter();
        changeSongFilter = new IntentFilter();
        playerActivityFilter.addAction("playerActivity");
        changeSongFilter.addAction("changeSong");
        this.registerReceiver(changeSongReceiver , changeSongFilter);
        this.registerReceiver(playerActivityReceiver , playerActivityFilter);
    }

    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.player_fragContainer , fragment);
        transaction.commit();
    }


    @Override
    public void onClick(View v) {
        Intent in = new Intent(this , PlayMusicService.class);
        switch (v.getId()){
            case R.id.player_back:
                finishAfterTransition();//结束时附带转场动画调这个
                break;
            case R.id.player_play:
                play(in);
                break;
            case R.id.player_playList:
                currentPlayListDialog = new CurrentPlayListDialog(this);
                //dialog退出时会刷新主页面的UI
                currentPlayListDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        changePlayMode(false);
                    }
                });
                //避免开启多个dialog
                if (!currentPlayListDialog.isShowing()){
                    currentPlayListDialog.show();
                }
                break;
            case R.id.player_next:
                in.putExtra("action" , PlayMusicService.NEXT_SONG);
                startService(in);
                break;
            case R.id.player_previous:
                in.putExtra("action" , PlayMusicService.PREVIOUS_SONG);
                startService(in);
                break;
            case R.id.player_play_mode:
                Constant.currentPlayMode ++;
                changePlayMode(true);
                break;
            case R.id.player_like:
                CRUD crud = new CRUD("music" , this);
                if (!crud.retrieveIfAlreadyInPlaylist(0 , Constant.currentPlayList.get(Constant.currentPosition).musicUrl , 0 )){
                    ContentValues values = new ContentValues();
                    values.put("title" , Constant.currentPlayList.get(Constant.currentPosition).title);
                    values.put("artist" , Constant.currentPlayList.get(Constant.currentPosition).artist);
                    values.put("id" , Constant.currentPlayList.get(Constant.currentPosition).getId());
                    values.put("picUrl" , Constant.currentPlayList.get(Constant.currentPosition).picUrl);
                    values.put("musicUrl" , Constant.currentPlayList.get(Constant.currentPosition).musicUrl);
                    values.put("playlist" , 0);
                    values.put("creator" , Constant.activeAccount);
                    crud.add(values);
                    player_like.setImageResource(R.drawable.ic_baseline_favorite_24_red);
                    Toast.makeText(this, "收藏成功", Toast.LENGTH_SHORT).show();
                    values.clear();
                }else {
                    crud.deleteMusicFromPlaylist(0 , Constant.currentPlayList.get(Constant.currentPosition).getId());
                    player_like.setImageResource(R.drawable.ic_baseline_favorite_border_24);
                    Toast.makeText(this, "取消收藏", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.player_more_actions:
                ChoosePlaylistDialog choosePlaylistDialog = new ChoosePlaylistDialog(this , 0 , "player");
                choosePlaylistDialog.show();
                break;
        }
    }

    //刷新UI，传入参数isRefreshList判断在刷新UI的同时进行更新列表操作
    public void changePlayMode(boolean isRefreshList){
        switch (Constant.currentPlayMode){
            //列表循环
            case 0:
                player_play_mode.setImageResource(R.drawable.ic_round_repeat_24);
                break;
            //单曲循环
            case 1:
                player_play_mode.setImageResource(R.drawable.ic_baseline_repeat_one_24_white);
                break;
            //随机播放
            case 2:
                player_play_mode.setImageResource(R.drawable.ic_baseline_compare_arrows_24_white);
                if (isRefreshList){
                    Intent in = new Intent(this , PlayMusicService.class);
                    in.putExtra("action" , PlayMusicService.RANDOM_PLAYING);
                    startService(in);
                }
                break;
            //列表循环
            case 3:
                //设定为列表循环
                Constant.currentPlayMode = 0;
                player_play_mode.setImageResource(R.drawable.ic_round_repeat_24);
                break;
        }
    }

    public void play(Intent intent){
        //根据mediaPlayer状态切换播放状态和按钮图像
        if (Constant.mediaPlayer.isPlaying()){
            intent.putExtra("action" , PlayMusicService.MUSIC_PAUSE);
            startService(intent);
            player_play.setImageResource(R.drawable.ic_baseline_play_circle_outline_36_large);
        }else{
            intent.putExtra("action" , PlayMusicService.MUSIC_RESUME);
            startService(intent);
            player_play.setImageResource(R.drawable.ic_baseline_pause_circle_outline_40_large);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser){
            Intent in = new Intent(this , PlayMusicService.class);
            in.putExtra("action" , PlayMusicService.CHANGE_PROGRESS);
            in.putExtra("progress" , progress);
            startService(in);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    class ChangeSongReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String mode = null;
            if (intent.getStringExtra("activityAction") == null){
                String title = Constant.currentPlayList.get(Constant.currentPosition).title;
                String artist = Constant.currentPlayList.get(Constant.currentPosition).artist;
                player_title.setText(title);
                player_artist.setText(artist);
                player_duration.setMax(Constant.currentPlayList.get(Constant.currentPosition).time);
                player_duration.setProgress(Constant.mediaPlayer.getCurrentPosition());
                player_start_time.setText("00:00");
                player_end_time.setText(toMinutes(Constant.currentPlayList.get(Constant.currentPosition).time));
                player_play.setImageResource(R.drawable.ic_baseline_pause_circle_outline_40_large);
                //检查该歌曲是否存在于喜爱的歌单中，并根据存在状态设置不同ui
                if (new CRUD("music" , mContext).retrieveIfAlreadyInPlaylist(0 , Constant.currentPlayList.get(Constant.currentPosition).musicUrl , Constant.currentPlayList.get(Constant.currentPosition).getId())){
                    player_like.setImageResource(R.drawable.ic_baseline_favorite_24_red);
                }else {
                    player_like.setImageResource(R.drawable.ic_baseline_favorite_border_24);
                }
            }else{
                if (new CRUD("music" , mContext).retrieveIfAlreadyInPlaylist(0 , Constant.currentPlayList.get(Constant.currentPosition).musicUrl , Constant.currentPlayList.get(Constant.currentPosition).getId())){
                    player_like.setImageResource(R.drawable.ic_baseline_favorite_24_red);
                }else {
                    player_like.setImageResource(R.drawable.ic_baseline_favorite_border_24);
                }
            }
        }
    }

    class PlayerActivityReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String mode = intent.getStringExtra("activityAction");
            switch (mode){
                case "clearList":
                    //终止当前播放的音乐，清空播放列表，结束播放器页面
                    Constant.mediaPlayer.stop();
                    Constant.mediaPlayer.release();
                    Constant.currentPosition = 0;
                    Constant.currentPlayList.clear();
                    finishAfterTransition();
                    break;
            }
        }
    }
}
