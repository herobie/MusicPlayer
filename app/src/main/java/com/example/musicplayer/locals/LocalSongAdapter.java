package com.example.musicplayer.locals;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Bean.SongBean;
import com.example.musicplayer.Constant;
import com.example.musicplayer.PlayMusicService;
import com.example.musicplayer.R;
import com.example.musicplayer.players.dialog.ItemsActionsDialog;

import java.util.List;

public class LocalSongAdapter extends RecyclerView.Adapter<LocalSongAdapter.ViewHolder>{
    private List<SongBean> songBeanList;
    private Context context;
    private ViewHolder holder;

    static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView local_number , local_title , local_artist;
        private ImageButton local_actions;
        private ConstraintLayout local_item_view;
        public ViewHolder(View view){
            super(view);
            local_number = view.findViewById(R.id.local_number);
            local_title = view.findViewById(R.id.local_title);
            local_artist = view.findViewById(R.id.local_artist);
            local_actions = view.findViewById(R.id.local_actions);
            local_item_view = view.findViewById(R.id.local_item_view);
        }
    }

    public LocalSongAdapter(List<SongBean> songBeanList, Context context){
        this.songBeanList = songBeanList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_item, parent , false);
        holder = new ViewHolder(view);
        return holder;
    }


    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.local_title.setText(songBeanList.get(position).getTitle());
        holder.local_artist.setText(songBeanList.get(position).getArtist());
        holder.local_number.setText(String.valueOf(position + 1));
        holder.local_item_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //先判断播放列表中有没有相同的，如果没有再加入播放列表
                boolean isSame = false;
                for (int i = 0 ; i < Constant.currentPlayList.size() ; i++){
                    if (Constant.localSongList.get(position).musicUrl.equals( Constant.currentPlayList.get(i).musicUrl)){
                        isSame = true;
                        break;
                    }
                }
                if (!isSame){
                    Constant.currentPlayList.add(Constant.currentPosition , Constant.localSongList.get(position));
                }
                Intent startPlayingIntent = new Intent( context , PlayMusicService.class);
                startPlayingIntent.putExtra("action" , PlayMusicService.NEW_SONG);
                context.startService(startPlayingIntent);
            }
        });

        holder.local_actions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemsActionsDialog itemsActionsDialog = new ItemsActionsDialog(v.getContext() , "local" , position);
                itemsActionsDialog.show();
                itemsActionsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                    }
                });
            }
        });
    }


    @Override
    public int getItemCount() {
        return songBeanList.size();
    }


}
