package com.example.musicplayer.profile;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Constant;
import com.example.musicplayer.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileFragment extends Fragment implements View.OnClickListener{
    private ImageView profile_background;
    private CircleImageView profile_profile;
    private TextView profile_listening , profile_createDays , profile_ip , profile_createTime, profile_warning , profile_nickname , profile_signature , profile_follow;
    private LinearLayout profile_cover , profile_column;
    private final int NETWORK_ERROR = 1;
    private final int UPDATE_UI = 2;
    private int level , listenSongs , createDays;
    private long createTime;
    private RefreshUIReceiver refreshUIReceiver;
    private IntentFilter refreshUIFilter;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case NETWORK_ERROR:
                    Toast.makeText(getContext(), "获取个人信息失败，请检查网络！", Toast.LENGTH_SHORT).show();
                    break;
                case UPDATE_UI:
                    updateUI();
                    break;
            }
        }
    };
    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_profile, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initView(view);
        initReceiver();
        Log.d("ProfileFragment" , "执行onViewCreated");
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();
        Log.d("ProfileFragment" , "执行onResume");
        //登录后加载个人信息页面
        if (Constant.isLogin){
            int id = Constant.profileBean.getAccount().getId();
            String url = "http://wyyapi.itaemobile.top/user/detail?uid=" + String.valueOf(id);
            sendOkHttpRequest(url, new Callback() {
                Message message = new Message();
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    message.what = NETWORK_ERROR;
                    handler.sendMessage(message);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    parseInfoJSON(response.body().string());
                    message.what = UPDATE_UI;
                    handler.sendMessage(message);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(refreshUIReceiver);
    }

    public void initView(View view){
        profile_profile = view.findViewById(R.id.profile_profile);
        profile_cover = view.findViewById(R.id.profile_cover);
        profile_cover.setOnClickListener(this);
        profile_background = view.findViewById(R.id.profile_background);
        profile_background.setOnClickListener(this);
        if (!Constant.isLogin){
            profile_cover.setVisibility(View.VISIBLE);
        }
        profile_listening = view.findViewById(R.id.profile_listening);
        profile_ip = view.findViewById(R.id.profile_ip);
        profile_column = view.findViewById(R.id.profile_column);
        profile_createTime = view.findViewById(R.id.profile_createTime);
        profile_warning = view.findViewById(R.id.profile_warning);
        profile_nickname = view.findViewById(R.id.profile_nickname);
        profile_signature = view.findViewById(R.id.profile_signature);
        profile_follow = view.findViewById(R.id.profile_follow);
        profile_createDays = view.findViewById(R.id.profile_createDays);
    }

    public void initReceiver(){
        refreshUIReceiver = new RefreshUIReceiver();
        refreshUIFilter = new IntentFilter();
        refreshUIFilter.addAction("refreshProfileUI");
        getContext().registerReceiver(refreshUIReceiver , refreshUIFilter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.profile_cover:
                Intent in = new Intent(getContext() , LoginActivity.class);
                startActivity(in);
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    public void updateUI(){
        profile_cover.setVisibility(View.GONE);
        Glide.with(this).load(Constant.profileBean.getProfile().getAvatarUrl())
                .placeholder(R.drawable.ic_baseline_person_24)
                .error(R.drawable.ic_baseline_person_24)
                .into(profile_profile);
        Glide.with(this)
                .load(Constant.profileBean.getProfile().getBackgroundUrl())
                .placeholder(R.drawable.ic_baseline_photo_24)
                .error(R.drawable.ic_baseline_photo_24)
                .into(profile_background);
        profile_nickname.setText(Constant.profileBean.getProfile().getNickname());
        String signature = Constant.profileBean.getProfile().getSignature();
        //如果没有签名就隐藏签名栏,好看
        if (!signature.isEmpty()){
            profile_signature.setVisibility(View.VISIBLE);
            profile_signature.setText(signature);
        }else {
            profile_signature.setVisibility(View.GONE);
        }
        profile_follow.setText(Constant.profileBean.getProfile().getFolloweds() + "关注" + "   |   " + Constant.profileBean.getProfile().getFollows() + "粉丝" + "   |   " + "lv." + level);
        profile_createTime.setText("创建时间：" + transBirthday(createTime));
        profile_listening.setText("听过的歌曲数：" + String.valueOf(listenSongs));
        profile_createDays.setText("创建天数：" + createDays);
    }

    public void sendOkHttpRequest(String target_url , Callback callback){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(target_url)
                            .build();
                    client.newCall(request).enqueue(callback);
                }catch (Exception e){
                    Log.e("sendOkHttpRequest" , e.getMessage());
                }
            }
        }).start();
    }

    public void parseInfoJSON(String responseData){
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            level = jsonObject.getInt("level");
            listenSongs = jsonObject.getInt("listenSongs");
            createTime = jsonObject.getInt("createTime");
            createDays = jsonObject.getInt("createDays");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String transBirthday(long createTime){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = sdf.format(createTime);
        return date;
    }

    class RefreshUIReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            //退出登录时直接盖住原来的UI
            profile_cover.setVisibility(View.VISIBLE);
        }
    }
}
