package com.example.musicplayer.search;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Constant;
import com.example.musicplayer.R;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
    private List<String> searchHistoryList;
    private Context context;
    public SearchAdapter(List<String> searchHistoryList , Context context){
        this.searchHistoryList = searchHistoryList;
        this.context = context;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView tv_history;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_history = itemView.findViewById(R.id.tv_history);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent , false);
        ViewHolder holder = new ViewHolder(view);
        Log.d("SearchAdapter" , "成功创建ViewHolder");
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tv_history.setText(searchHistoryList.get(position));
        Log.d("SearchAdapter" , "成功创建历史记录标签");
        holder.tv_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent("searchBarInsert");
                in.putExtra("itemName" , holder.tv_history.getText());
                context.sendBroadcast(in);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Constant.searchHistory.size();
    }


}
