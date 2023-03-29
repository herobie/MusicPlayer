package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.musicplayer.players.player.CurrentPlayListDialog;
import com.example.musicplayer.players.player.PlayerActivity;

import java.io.IOException;
import java.util.Collections;

public class PlayMusicService extends Service implements MediaPlayer.OnBufferingUpdateListener , MediaPlayer.OnCompletionListener {
    private int progress;
    public static final int NEW_SONG = 1;
    public static final int MUSIC_PAUSE = 2;
    public static final int MUSIC_RESUME = 3;
    public static final int NEXT_SONG = 4;
    public static final int PREVIOUS_SONG = 5;
    public static final int CHANGE_PROGRESS = 6;
    public static final int UPDATE_PROGRESS = 7;
    public static final int CHANGE_SONG = 8;
    public static final int RANDOM_PLAYING = 9;
    public static final int MUSIC_PREPARE = 10;

    private Handler handler = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message message){
            Intent intent = new Intent("updateStatus");
            sendBroadcast(intent);
            switch (message.what){
                case NEW_SONG:
                    startPlaying();
                    Intent in = new Intent(getApplicationContext() , PlayerActivity.class);
                    in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(in);
                    break;
                case MUSIC_PAUSE:
                    pause();
                    break;
                case MUSIC_RESUME:
                    resume();
                    break;
                case NEXT_SONG:
                    nextSong();
                    break;
                case PREVIOUS_SONG:
                    previousSong();
                    break;
                case CHANGE_PROGRESS:
                    onChangeProgress();
                    break;
                case CHANGE_SONG:
                    startPlaying();
                    break;
                case RANDOM_PLAYING:
                    formingRandomPlayList();
                    break;
                case MUSIC_PREPARE:
                    prepare();
                    Log.d("PlayMusicService" , "跳出prepare()方法");
                    break;
            }
        }
    };

    public PlayMusicService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    //第一次创建时调用onCreate
    @Override
    public void onCreate() {
        super.onCreate();
        Constant.mediaPlayer.setOnBufferingUpdateListener(this);
    }

    //每次启动时调用onStartCommand
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        progress = intent.getIntExtra("progress" , 0);
        Message message = new Message();
        int action = intent.getIntExtra("action" , 0);
        message.what = action;
        handler.sendMessage(message);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void initListener(){
        Constant.mediaPlayer.setOnBufferingUpdateListener(this);
        Constant.mediaPlayer.setOnCompletionListener(this);
    }

    public void prepare(){
        Log.d("PlayMusicService" , "开始准备音乐");
        releasePlayer();
        Constant.mediaPlayer = new MediaPlayer();
        initListener();
        try {
            String url = Constant.currentPlayList.get(Constant.currentPosition).musicUrl;
            Log.d("PlayMusicService" , "当前音乐url: " + url);
            Constant.mediaPlayer.setDataSource(Constant.currentPlayList.get(Constant.currentPosition).musicUrl);
            Log.d("PlayMusicService" , "音乐资源已经获取成功");
            Constant.mediaPlayer.prepare();
            Log.d("PlayMusicService" , "音乐准备完毕");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("PlayMusicService" , "prepare()方法执行完毕");
    }

    public void startPlaying(){
        Constant.mediaPlayer.release();
        Constant.mediaPlayer = new MediaPlayer();
        initListener();
        try {
            Constant.mediaPlayer.setDataSource(Constant.currentPlayList.get(Constant.currentPosition).musicUrl);
            Log.d("PlayMusicService" , "音乐开始准备");
            Constant.mediaPlayer.prepare();
            Log.d("PlayMusicService" , "音乐准备完毕");
            resume();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause(){
        if (Constant.mediaPlayer.isPlaying()){
            Constant.mediaPlayer.pause();
            Log.d("PlayMusicService" , "音乐暂停");
            Intent in = new Intent("updateStatus");
            sendBroadcast(in);
        }
    }

    public void resume(){
        if (!Constant.mediaPlayer.isPlaying()){
            Constant.mediaPlayer.start();
            Log.d("PlayMusicService" , "音乐继续");
            updateProgress();
            Intent in = new Intent("updateStatus");
            sendBroadcast(in);
        }
    }

    //更新进度条
    public void updateProgress(){
        Intent in = new Intent("changeSong");
        in.putExtra("mode" , "updateProgress");
        in.putExtra("currentProgress" , Constant.mediaPlayer.getCurrentPosition());
        sendBroadcast(in);
        PlayerActivity.updateHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS , 1000);
    }

    public void nextSong(){
        Constant.mediaPlayer.pause();
        releasePlayer();
        Constant.mediaPlayer = new MediaPlayer();
        initListener();
        //切换至下一首，将指针向下移动
        Constant.currentPosition ++;
        //如果超出集合长度了，则回到第一首歌的位置
        if (Constant.currentPosition > Constant.currentPlayList.size() - 1){
            Constant.currentPosition = 0;
        }
        //通知更换标题，封面等
        Intent in = new Intent("changeSong");
        sendBroadcast(in);
        Intent intent = new Intent("refreshDialog");
        intent.putExtra("which" , "status");
        sendBroadcast(intent);
        try {
            Constant.mediaPlayer.setDataSource(Constant.currentPlayList.get(Constant.currentPosition).musicUrl);
            Constant.mediaPlayer.prepare();
            Constant.mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void previousSong(){
        Constant.mediaPlayer.pause();
        releasePlayer();
        Constant.mediaPlayer = new MediaPlayer();
        initListener();
        Constant.currentPosition --;
        //如果超出集合长度了，则回到最后一首歌的位置
        if (Constant.currentPosition < 0){
            Constant.currentPosition = Constant.currentPlayList.size() - 1;
        }
        //通知更换标题，封面等
        Intent in = new Intent("changeSong");
        sendBroadcast(in);
        Intent intent = new Intent("refreshDialog");
        intent.putExtra("which" , "status");
        sendBroadcast(intent);
        try {
            Constant.mediaPlayer.setDataSource(Constant.currentPlayList.get(Constant.currentPosition).musicUrl);
            Constant.mediaPlayer.prepare();
            Constant.mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onChangeProgress(){
        Constant.mediaPlayer.seekTo(progress);
    }

    public void releasePlayer(){
        if (Constant.mediaPlayer != null){
            if(Constant.mediaPlayer.isPlaying()){
                Constant.mediaPlayer.stop();
                Constant.mediaPlayer.reset();
            }
            Constant.mediaPlayer.release();
            Constant.mediaPlayer = null;
            Log.d("PlayMusicService" , "音乐资源释放完毕");
        }
    }

    //生成随机播放列表
    @SuppressLint("NotifyDataSetChanged")
    public void formingRandomPlayList(){
        //先把当前播放歌曲的id拿出来
        String musicUrl = Constant.currentPlayList.get(Constant.currentPosition).musicUrl;
        Collections.shuffle(Constant.currentPlayList);
        //将拿出来的id与集合中的id一一比对，确定当前歌曲在打乱后的集合中的位置
        for (int i = 0 ; i < Constant.currentPlayList.size() ; i++){
            if (musicUrl == Constant.currentPlayList.get(i).musicUrl){
                Constant.currentPosition = i;
                break;
            }
        }
        //如果是在dialog中切换的则更新适配器
        if (PlayerActivity.currentPlayListDialog != null && PlayerActivity.currentPlayListDialog.isShowing()){
            CurrentPlayListDialog.currentPlayListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {


    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Intent intent = new Intent("updateStatus");
        //判断循环播放类型
        switch (Constant.currentPlayMode){
            case 0:
            case 2:
                nextSong();
                sendBroadcast(intent);
                break;
            case 1:
                Constant.mediaPlayer.seekTo(0);
                resume();
                sendBroadcast(intent);
                break;
        }
    }

}