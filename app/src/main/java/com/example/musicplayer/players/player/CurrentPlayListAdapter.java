package com.example.musicplayer.players.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.R;

public class CurrentPlayListAdapter extends RecyclerView.Adapter<CurrentPlayListAdapter.ViewHolder> {
    private Context context;
    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageButton current_remove , current_playing;
        private TextView current_title , current_artist;
        private ConstraintLayout current_view;
        public ViewHolder(@NonNull View view) {
            super(view);
            current_remove = view.findViewById(R.id.current_remove);
            current_playing = view.findViewById(R.id.current_playing);
            current_title = view.findViewById(R.id.current_title);
            current_artist = view.findViewById(R.id.current_artist);
            current_view = view.findViewById(R.id.current_view);
        }
    }

    public CurrentPlayListAdapter(Context context){
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_current_playlist, parent , false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.current_title.setText(Constant.currentPlayList.get(position).getTitle());
        holder.current_artist.setText(Constant.currentPlayList.get(position).getArtist());
        //显示正在播放的歌曲
        if (Constant.currentPosition == position){
            holder.current_playing.setVisibility(View.VISIBLE);
        }else{
            holder.current_playing.setVisibility(View.GONE);
        }
        holder.current_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position != Constant.currentPosition){
                    Constant.currentPosition =  position;
                    Intent in = new Intent("changeSong");
                    context.sendBroadcast(in);
                    Intent startPlayingIntent = new Intent(context , PlayMusicService.class);
                    startPlayingIntent.putExtra("action" , PlayMusicService.CHANGE_SONG);
                    context.startService(startPlayingIntent);
                    PlayerActivity.currentPlayListDialog.hide();
                }
            }
        });

        holder.current_remove.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v) {
                //如果现存播放列表数量小于1就直接执行删除全部的功能
                if (Constant.currentPlayList.size() <= 1){
                    Intent finishIntent = new Intent("playerActivity");
                    finishIntent.putExtra("activityAction" , "clearList");
                    context.sendBroadcast(finishIntent);
                }else {
                    //判断删的是不是正在播放的歌
                    boolean isRemoveCurrentPlaying = false;
                    if (Constant.currentPosition == position){
                        Constant.mediaPlayer.stop();
                        isRemoveCurrentPlaying = true;
                    }
                    Constant.currentPlayList.remove(position);
                    //如果删除的是正在播放的歌，那么就切换到下一首歌
                    if (isRemoveCurrentPlaying){
                        //如果正在播放的是最后一首了，则指针前移防止超出集合长度
                        if (Constant.currentPosition == Constant.currentPlayList.size()){
                            Constant.currentPosition --;
                        }
                        Intent in = new Intent(context , PlayMusicService.class);
                        in.putExtra("action" , PlayMusicService.CHANGE_SONG);
                        context.startService(in);
                    }else if (position < Constant.currentPosition){
                        Constant.currentPosition --;//如果被删除歌在当前播放歌曲的前面，则指针前移，保证指针和歌曲相同
                    }
                    notifyDataSetChanged();
                    Intent in = new Intent("refreshDialog");
                    in.putExtra("which" , "describe");
                    context.sendBroadcast(in);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return Constant.currentPlayList.size();
    }


}
