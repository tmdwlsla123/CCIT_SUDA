package com.cookandroid.ccit_suda;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cookandroid.ccit_suda.laravelechoandroid.EchoCallback;
import com.cookandroid.ccit_suda.laravelechoandroid.EchoOptions;
import com.cookandroid.ccit_suda.laravelechoandroid.connector.SocketIOConnector;
import com.cookandroid.ccit_suda.retrofit2.ApiInterface;
import com.cookandroid.ccit_suda.retrofit2.CallbackWithRetry;
import com.cookandroid.ccit_suda.retrofit2.HttpClient;
import com.cookandroid.ccit_suda.room.Room_list;
import com.cookandroid.ccit_suda.room.Talk;
import com.cookandroid.ccit_suda.room.TalkDatabase;
import com.cookandroid.ccit_suda.room.User_list;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;

public class FlistAdapter extends BaseAdapter {
    Context context;
    ArrayList<friend_list> friend_lists_itemArrayList;
    ViewHolder viewholder;
    ApiInterface api;
    SharedPreferences sharedPreferences;
    int position;
    EchoOptions options = new EchoOptions();
    SocketIOConnector echo;
    private ImageView follow_bt;

    class ViewHolder {
        TextView talkuser;
        ImageView follow_bt;
    }

    public FlistAdapter(Context context, ArrayList<friend_list> friend_lists_itemArrayList) {
        this.context = context;
        this.friend_lists_itemArrayList = friend_lists_itemArrayList;
    }

    @Override
    public int getCount() {
        return friend_lists_itemArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return friend_lists_itemArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.profile, null);
            viewholder = new ViewHolder();
            viewholder.talkuser = convertView.findViewById(R.id.talkusername);
            friend_lists_itemArrayList.get(position).getName();

            Log.v("위치", friend_lists_itemArrayList.get(position).getName());
            viewholder.follow_bt = convertView.findViewById(R.id.follow_bt);
            if (friend_lists_itemArrayList.get(position).getName().equals(friend_lists_itemArrayList.get(position).getFollow())) {
                Log.v("조건문실행", "실행됨");
                viewholder.follow_bt.setBackground(ContextCompat.getDrawable(context, R.drawable.unfollow));
            }
            convertView.setTag(viewholder);
        } else {
            viewholder = (ViewHolder) convertView.getTag();
        }
        viewholder.follow_bt = convertView.findViewById(R.id.follow_bt);
        viewholder.talkuser.setText(friend_lists_itemArrayList.get(position).getName());
        View finalConvertView = convertView;
        viewholder.follow_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("포지션", (friend_lists_itemArrayList.get(position).getName()));
                Toast.makeText(context, friend_lists_itemArrayList.get(position).getName(), Toast.LENGTH_SHORT).show();
//                finalConvertView.setVisibility(View.GONE);
//                notifyDataSetChanged();
                followRequest(friend_lists_itemArrayList.get(position).getName(), getItemId(position), v);


            }
        });
        return convertView;
    }

    //        팔로우 버튼 통신
    public void followRequest(String name, long btpo, View bt) {
        String url = "follows"; //ex) 요청하고자 하는 주소가 http://10.0.2.2/login 이면 String url = login 형식으로 적으면 됨
        api = HttpClient.getRetrofit().create(ApiInterface.class);
        HashMap<String, String> params = new HashMap<>();
        sharedPreferences = context.getSharedPreferences("File", 0);
        String userinfo = sharedPreferences.getString("userinfo", "");
        TalkDatabase talkDatabse;
        talkDatabse = TalkDatabase.getDatabase(context);
        params.put("user1", userinfo);
        Log.v("user기록", userinfo.toString());
        params.put("user2", name);
        Log.v("talkuser기록", name);

        Log.v("버튼의 위치", String.valueOf(btpo));
        Call<String> call = api.requestPost(url, params);
        // 비동기로 백그라운드 쓰레드로 동작
        call.enqueue(new Callback<String>() {
            // 통신성공 후 텍스트뷰에 결과값 출력
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                //서버에서 넘겨주는 데이터는 response.body()로 접근하면 확인가능
                Log.e("바뀐포인트", "12346");
                try {
                    JSONObject data = new JSONObject(response.body());
                    String followcount = data.getString("data");
                    JSONArray room_list = new JSONArray(data.getString("room_list"));
                    String room_index = data.getString("room_index");


                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (!talkDatabse.talkDao().isRowIsExist_user_list(name)) {
                                User_list user_list = new User_list(null, name, room_index);
                                talkDatabse.talkDao().insert_user_list(user_list);
                            } else {
                                talkDatabse.talkDao().delete_user_list(name);
                            }
                            for (int i = 0; i < room_list.length(); i++) {
                                try {
                                    JSONObject jsonObject = room_list.getJSONObject(i);
                                    String user = jsonObject.getString("user");
                                    String chat_room = jsonObject.getString("chat_room");
                                    String room_name = jsonObject.getString("room_name");
                                    String lately_chat_idx = jsonObject.getString("lately_chat_idx");
                                    Room_list r = new Room_list(null, user, chat_room, room_name, lately_chat_idx);
                                    if (!talkDatabse.talkDao().isRowIsExist_user_room_list(user, chat_room)) {
                                        talkDatabse.talkDao().insert_room_list(r);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    });

                    echo = ((boardActivity) boardActivity.context_board).echo;

                    if (followcount.equals("1")) {


//                        EchoOptions echoOptions = new EchoOptions();
//                        echoOptions.host = HttpClient.url;
//                        SocketIOConnector socketIOConnector = new SocketIOConnector(echoOptions);



                        bt.setBackground(ContextCompat.getDrawable(context, R.drawable.follow));
                        Toast.makeText(context, "언팔로우하셨습니다", Toast.LENGTH_SHORT).show();
                    } else {
                        if(!echo.isChannelExists("laravel_database_" + room_index)){
                            echo.channel("laravel_database_" + room_index)
                                    .listen("chartEvent", new EchoCallback() {
                                        @Override
                                        public void call(Object... args) {

                                            Log.d("웃기지마랄라", String.valueOf(args[1]));
                                            String qwe;
                                            String qwe1;
                                            String time;
                                            int qwe2;
                                            String chat_idx;
                                            String user_count;
                                            String image_status;
                                            String image_uri;
                                            try {
                                                Intent intent = new Intent("msg" + room_index);

                                                JSONObject jsonObject = new JSONObject(args[1].toString());
                                                qwe = jsonObject.getString("user");
                                                qwe1 = jsonObject.getString("message");
                                                qwe2 = Integer.parseInt(jsonObject.getString("channel"));
                                                chat_idx = jsonObject.getString("chat_idx");
                                                time = jsonObject.getString("time");
                                                user_count = jsonObject.getString("user_count");
                                                image_status = jsonObject.getString("image_status");
                                                image_uri = jsonObject.getString("message");
                                                if(image_status.equals("1")){
                                                    qwe1 = "사진을 보냈습니다.";
                                                }
                                                Talk t = new Talk(null, qwe, qwe1, qwe2, time, chat_idx, "0", user_count, image_status,image_uri);
                                                Log.v("1", String.valueOf(t));
                                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                                intent.putExtra("chat_idx", chat_idx);
                                                talkDatabse.talkDao().insert(t);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                        }


                        bt.setBackground(ContextCompat.getDrawable(context, R.drawable.unfollow));
                        Toast.makeText(context, "팔로우하셨습니다", Toast.LENGTH_SHORT).show();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // 통신실패
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.v("통신실패", String.valueOf("error : " + t.toString()));
            }
        });
    }
}
