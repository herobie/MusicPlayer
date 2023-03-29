package com.example.musicplayer.profile;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.cookieStore.CookieJarImpl;
import com.example.musicplayer.cookieStore.PersistentCookieStore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{
    private TextInputEditText login_email , login_password;
    private TextView login_warning;
    private Button login_login;
    private Toolbar login_toolbar;
    private final int NETWORK_ERROR = 1;
    private final int LOGIN_FAILED = 2;
    private final int LOGIN_SUCCESS = 3;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case NETWORK_ERROR:
                    login_warning.setText("网络错误！请检查网络");
                    Toast.makeText(LoginActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    break;
                case LOGIN_FAILED:
                    login_warning.setText("账号或密码错误");
                    break;
                case LOGIN_SUCCESS:
                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    Constant.isLogin = true;
                    Constant.activeAccount = Constant.profileBean.getProfile().getNickname();
                    finish();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
    }

    public void initView(){
        login_login = findViewById(R.id.login_login);
        login_login.setOnClickListener(this);
        login_warning = findViewById(R.id.login_warning);
        login_email = findViewById(R.id.login_email);
        login_password = findViewById(R.id.login_password);
        login_toolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(login_toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.others_toolbar , menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.login_login:
                //判断是否输入了邮箱和地址
                boolean isInput = true;
                String email = login_email.getText().toString();
                String password = login_password.getText().toString();
                if (password.isEmpty()){
                    login_warning.setText("请输入密码！");
                    isInput = false;
                }
                if (email.isEmpty()){
                    login_warning.setText("请输入邮箱地址！");
                    isInput = false;
                }
                //只有两个都输入了才能发送信息，否则弹出警告
                if (isInput){
                    String url = "http://wyyapi.itaemobile.top/login?email=" + email + "&password=" + password;
                    sendLoginOkHttpRequest(getApplicationContext() , url, new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Message message = new Message();
                            message.what = NETWORK_ERROR;
                            handler.sendMessage(message);
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            parseInfoJSON(response.body().string());
                        }
                    });
                }
                break;
        }
    }

    public static void sendLoginOkHttpRequest(Context context , String target_url , Callback callback){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    OkHttpClient client = new OkHttpClient.Builder()
                            .cookieJar(new CookieJarImpl(new PersistentCookieStore(context)))
                            .build();
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
        Message message = new Message();
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            int code = jsonObject.getInt("code");
            if (code == 200){
                String data = jsonObject.toString();
                Gson gson = new Gson();
                Constant.profileBean = gson.fromJson(data , ProfileBean.class);
                message.what = LOGIN_SUCCESS;
            }else {
                message.what = LOGIN_FAILED;
            }
            handler.sendMessage(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
