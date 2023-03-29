package com.example.musicplayer.search.result;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.search.result.playlist.PlaylistResultAdapter;
import com.example.musicplayer.search.result.single.ResultAdapter;
import com.google.android.material.tabs.TabLayout;

public class ResultFragment extends Fragment {
    private TabLayout result_type;
    private LinearLayout single_view , list_view;
    private RecyclerView result_single , result_list;
    private ResultAdapter resultAdapter;
    private PlaylistResultAdapter playlistResultAdapter;
    private LocalBroadcastManager localBroadcastManager;
    private ResultRVFragment resultRVFragment;
    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_search_result, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initView(view);
        initTabItems(view);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void initView(View view){
        single_view = view.findViewById(R.id.single_view);
        resultRVFragment = new ResultRVFragment("single");
        replaceFragment(resultRVFragment);
    }

    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.single_view , fragment);
        transaction.commit();
    }

    public void initTabItems(View view){
        result_type = view.findViewById(R.id.result_type);
        result_type.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Intent in = new Intent("adapterActions");
                if (tab.getPosition() == 0){
                    in.putExtra("adapterAction" , "single");
                }else if (tab.getPosition() == 1){
                    in.putExtra("adapterAction" , "playlist");
                }
                getContext().sendBroadcast(in);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        result_type.addTab(result_type.newTab().setText("单曲"));
        result_type.addTab(result_type.newTab().setText("歌单"));
    }

}
