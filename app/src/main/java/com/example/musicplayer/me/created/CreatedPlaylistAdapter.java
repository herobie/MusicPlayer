package com.example.musicplayer.me.created;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Bean.playlist.Playlist;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.example.musicplayer.me.SavedPlaylistActionDialog;
import com.example.musicplayer.playlist.PlaylistActivity;

import java.util.List;

public class CreatedPlaylistAdapter extends RecyclerView.Adapter<CreatedPlaylistAdapter.ViewHolder> {
    private Context context;
    private List<Playlist> myPlaylist;
    public CreatedPlaylistAdapter(Context context , List<Playlist> myPlaylist){
        this.context = context;
        this.myPlaylist = myPlaylist;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        private ImageView result_cover;
        private TextView result_playlist_name , result_playlist_creator;
        private CardView result_playlist_view;
        private ImageButton result_playlist_action;
        public ViewHolder(@NonNull View view) {
            super(view);
            result_playlist_creator = view.findViewById(R.id.result_playlist_creator);
            result_playlist_name = view.findViewById(R.id.result_playlist_name);
            result_cover = view.findViewById(R.id.result_cover);
            result_playlist_view = view.findViewById(R.id.result_playlist_view);
            result_playlist_action = view.findViewById(R.id.result_playlist_action);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result_playlist, parent , false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.result_playlist_action.setVisibility(View.VISIBLE);
        holder.result_playlist_name.setText(myPlaylist.get(position).getName());
        holder.result_playlist_creator.setText(myPlaylist.get(position).getCreator().getNickname());
        //检查歌单里是否有至少一首歌，如果有则将封面更新为最新的那个
        if (new CRUD("music" , context).isAtLeastOneMusicExistInPlaylist(myPlaylist.get(position).getId())){
            String picUrl = new CRUD("music" , context).getTheFirstSongCover(myPlaylist.get(position).getId());
            ContentValues values = new ContentValues();
            values.put("coverImgUrl" , picUrl);
            new CRUD("playlist" , context).updatePicUrl(myPlaylist.get(position).getId() , values);
            //更新歌单列表中的封面
            myPlaylist.get(position).setCoverImgUrl(picUrl);
            values.clear();
        }
        //歌单封面
        Glide.with(context).load(Constant.myPlaylist.get(position).getCoverImgUrl())
                .placeholder(R.drawable.ic_baseline_photo_24)
                .error(R.drawable.ic_baseline_photo_24)
                .into(holder.result_cover);
        holder.result_playlist_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(context , PlaylistActivity.class);
                in.putExtra("playlistType" , "save");
                in.putExtra("name" , myPlaylist.get(position).getName());
                in.putExtra("coverImgUrl" , myPlaylist.get(position).getCoverImgUrl());
                //获取歌单内歌曲
                new CRUD("music" , context).retrieveMusicInPlaylist(Constant.myPlaylist.get(position).getId() , Constant.myPlaylist.get(position).getOrderBy());
                Constant.playlistId = myPlaylist.get(position).getId();
                context.startActivity(in);
            }
        });

        holder.result_playlist_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SavedPlaylistActionDialog savedPlaylistActionDialog = new SavedPlaylistActionDialog(context , myPlaylist.get(position).getName() , myPlaylist.get(position).getCoverImgUrl() , myPlaylist.get(position).getId() , "save");
                Constant.playlistId = Constant.myPlaylist.get(position).getId();
                savedPlaylistActionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Intent in = new Intent("refreshAdapter");
                        context.sendBroadcast(in);
                    }
                });
                savedPlaylistActionDialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return myPlaylist.size();
    }


}
