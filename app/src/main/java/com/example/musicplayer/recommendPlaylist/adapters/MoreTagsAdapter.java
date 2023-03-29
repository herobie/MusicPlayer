package com.example.musicplayer.recommendPlaylist.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Constant;
import com.example.musicplayer.R;

public class MoreTagsAdapter extends RecyclerView.Adapter<MoreTagsAdapter.ViewHolder> {
    private Context context;
    public MoreTagsAdapter(Context context){
        this.context = context;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView tv_history;
        public ViewHolder(@NonNull View view) {
            super(view);
            tv_history = view.findViewById(R.id.tv_history);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent , false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.tv_history.setText(Constant.tagList.get(position).getName());
        holder.tv_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent("showMoreTags");
                in.putExtra("tagName" , Constant.tagList.get(position).getName());
                context.sendBroadcast(in);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Constant.tagList.size();
    }


}
