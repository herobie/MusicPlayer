package com.example.musicplayer.me;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.profile.ProfileBean;
import com.google.android.material.tabs.TabLayout;

import de.hdodenhof.circleimageview.CircleImageView;

public class MeFragment extends Fragment {
    private TabLayout me_tab;
    private MyPlaylistFragment myPlaylistFragment;
    private CircleImageView me_profile;
    private TextView me_username , me_info;
    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_me, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initTabItems(view);
        initView(view);
        replaceFragment(myPlaylistFragment);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.me_frag , fragment);
        transaction.commit();
    }

    public void initView(View view){
        me_info = view.findViewById(R.id.me_info);
        me_username = view.findViewById(R.id.me_username);
        me_profile = view.findViewById(R.id.me_profile);
        //登录后加载个人信息
        if (Constant.isLogin){
            me_username.setText(Constant.profileBean.getProfile().getNickname());
            int followeds = Constant.profileBean.getProfile().getFolloweds();
            int follows = Constant.profileBean.getProfile().getFollows();
            me_info.setText(followeds + "关注   |   " + follows + "粉丝");
            Glide.with(getContext()).load(Constant.profileBean.getProfile().getAvatarUrl())
                    .placeholder(R.drawable.ic_baseline_person_24)
                    .error(R.drawable.ic_baseline_person_24)
                    .into(me_profile);
        }
        myPlaylistFragment = new MyPlaylistFragment();
    }

    public void initTabItems(View view){
        me_tab = view.findViewById(R.id.me_tab);
        me_tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Intent in = new Intent("changePlaylistType");
                if (tab.getPosition() == 0){
                    in.putExtra("type" , "created");
                }else if (tab.getPosition() == 1){
                    in.putExtra("type" , "subscribe");
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
        me_tab.addTab(me_tab.newTab().setText("创建歌单"));
        me_tab.addTab(me_tab.newTab().setText("收藏歌单"));
    }
}
