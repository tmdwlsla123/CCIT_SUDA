package com.cookandroid.ccit_suda;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;
import com.android.volley.request.StringRequest;
import com.cookandroid.ccit_suda.UtilClass.UtilClass;
import com.cookandroid.ccit_suda.retrofit2.ApiInterface;
import com.cookandroid.ccit_suda.retrofit2.CallbackWithRetry;
import com.cookandroid.ccit_suda.retrofit2.HttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
//import com.android.volley.toolbox.StringRequest;

//import com.android.volley.VolleyError;
//import com.android.volley.toolbox.StringRequest;

public class PostUploadActivity extends AppCompatActivity {
    log a = new log();
    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date1 = new Date();
    String date = format1.format(date1);
    boolean err = false;
    boolean chk = true;
    Spinner spinner;
    List<String> spinnerArray;
    private final int GET_GALLERY_IMAGE = 200;
    private ImageView imageView;
    private ImageButton imgDel;
    String imgPath;
    ArrayAdapter<String> adapter;
    ApiInterface api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_upload);

        final EditText InputPostName = (EditText) findViewById(R.id.et_postname); // 글 제목 입력창
        final EditText InputPostContent = (EditText) findViewById(R.id.et_postcontent);   // 글 내용 입력창
        spinner = findViewById(R.id.spinner_cate);
        spinnerArray = new ArrayList<>();

        //갖고올 카테고리 함수구현
        get_categorie();
//        spinnerArray.add("itme1");
        adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);


//        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.v("TAG", String.valueOf(spinner.getSelectedItemPosition()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        imageView = (ImageView) findViewById(R.id.imgView);  //이미지 등록 버튼
        imgDel = (ImageButton) findViewById(R.id.imgbtn2);

        imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, GET_GALLERY_IMAGE);
            }
        });
        ImageButton imgdel = (ImageButton) findViewById(R.id.imgbtn2); //이미지 삭제 버튼

        imgdel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageView.setImageBitmap(null);
                imgPath = (null);
            }
        });
        ImageButton back = (ImageButton) findViewById(R.id.bt_backlist);   // 돌아가기 버튼
        Button upload = (Button) findViewById(R.id.bt_upload);    // 글 작성 버튼

        back.setOnClickListener(new View.OnClickListener() {         // 뒤로가기
            @Override
            public void onClick(View view) {
                a.appendLog((date + "/M/boardActivity/0"));
                Intent intent = new Intent(getApplicationContext(), boardActivity.class);
                startActivity(intent);
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {         // 글 작성 버튼
            @Override
            public void onClick(View view) {                // 글 작성
                String postName = InputPostName.getText().toString();
                String postContent = InputPostContent.getText().toString();

                check(postName, postContent);
                if (!(postName.isEmpty() || postContent.isEmpty())) {
//                    Toast.makeText(getApplicationContext(), "성공", Toast.LENGTH_SHORT).show();
                    sendRequest();
                } else {
                    Toast.makeText(getApplicationContext(), "공백이 있는지 확인해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });


        //외부 저장소에 권한 필요, 동적 퍼미션
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionResult = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionResult == PackageManager.PERMISSION_DENIED) {
                String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, 10);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 10:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "외부 메모리 읽기/쓰기 사용 가능", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "외부 메모리 읽기/쓰기 제한", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    // 이미지 실험
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
//            Toast.makeText(this, "RESULT_OK", Toast.LENGTH_SHORT).show();
            Uri uri = data.getData();
            if (uri != null) {
                imageView.setImageURI(uri);
                //갤러리앱에서 관리하는 DB정보가 있는데, 그것이 나온다 [실제 파일 경로가 아님!!]
                //얻어온 Uri는 Gallery앱의 DB번호임. (content://-----/2854)
                //업로드를 하려면 이미지의 절대경로(실제 경로: file:// -------/aaa.png 이런식)가 필요함
                //Uri -->절대경로(String)로 변환
                imgPath = getRealPathFromUri(uri);   //임의로 만든 메소드 (절대경로를 가져오는 메소드)

                //이미지 경로 uri 확인해보기
//                new AlertDialog.Builder(this).setMessage(uri.toString() + "\n" + imgPath).create().show();
            }
        } else {
            Toast.makeText(this, "이미지 선택을 하지 않았습니다.", Toast.LENGTH_SHORT).show();
        }


    }

    String getRealPathFromUri(Uri uri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, uri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    public void check(String postName, String postContent) {
        if (postName.equals("") || postContent.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("ERR");
            builder.setMessage("공백인 항목이 있습니다. 공백은 입력할 수 없습니다.");
            builder.setCancelable(false);

            builder.setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            err = true;
                        }
                    });
            builder.show();
        } else {
            err = false;
        }
    }
    public void get_categorie() {
        String url = "get_categorie"; //ex) 요청하고자 하는 주소가 http://10.0.2.2/login 이면 String url = login 형식으로 적으면 됨
        api = HttpClient.getRetrofit().create( ApiInterface.class );
        Call<String> call = api.requestGet(url);

        // 비동기로 백그라운드 쓰레드로 동작
        call.enqueue(new CallbackWithRetry<String>() {
            // 통신성공 후
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
//                        서버에서 넘겨주는 데이터는 response.body()로 접근하면 확인가능
                Log.v("retrofit2",response.body().toString());
                try {
                    JSONArray jsonArray = new JSONArray(response.body());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        spinnerArray.add(jsonObject.getString("categorie"));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);

            }

            // 통신실패
            @Override
            public void onFailure(Call<String> call, Throwable t) { super.onFailure(call,t);
                Log.v("retrofit2",String.valueOf("error : "+t.toString()));
                a.appendLog(date + "/" + "E" + "/PostUploadActivity/" + t.toString());
                Toast.makeText(getApplicationContext(), "서버와 통신이 원할하지 않습니다. 네트워크 연결상태를 확인해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }



//
    public void sendRequest() {
        EditText InputPostName = (EditText) findViewById(R.id.et_postname); // 글 제목 입력창
        EditText InputPostContent = (EditText) findViewById(R.id.et_postcontent);  // 글 내용 입력창
        ImageView InputImageView = (ImageView) findViewById(R.id.imgView);  //이미지 등록
        SharedPreferences sharedPreferences = getSharedPreferences("File", 0);
        String userinfo = sharedPreferences.getString("userinfo", "");
        String name = InputPostName.getText().toString();
        String content = InputPostContent.getText().toString();
        int categorie = spinner.getSelectedItemPosition();


//        MultipartBody.Part body1 = prepareFilePart("image", imgPath);

        String url = "add_post"; //ex) 요청하고자 하는 주소가 http://10.0.2.2/login 이면 String url = login 형식으로 적으면 됨
        api = HttpClient.getRetrofit().create(ApiInterface.class);
        HashMap<String, String> params = new HashMap<>();
        //요청 객체에 보낼 데이터를 추가
        params.put("Text", content);
        params.put("Title", name);
        params.put("categorie", String.valueOf(categorie));
        params.put("writer", userinfo);

        //이미지 파일 추가
        MultipartBody.Part filepart = null;
        if (imgPath != null) {
            File file = new File(imgPath);
            byte[] dd = UtilClass.getStreamByteFromImage(file);
            RequestBody requestFile =
                    RequestBody.create(
                            MediaType.parse(imgPath),
                            dd
                    );
            filepart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        }


        Call<String> call = api.requestFilePost(url, params, filepart);

        // 비동기로 백그라운드 쓰레드로 동작
        call.enqueue(new CallbackWithRetry<String>() {
            // 통신성공 후 텍스트뷰에 결과값 출력
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
//서버에서 넘겨주는 데이터는 response.body()로 접근하면 확인가능
                Log.v("retrofit2", String.valueOf(response));
                Log.v("retrofit2", String.valueOf(response.body()));
                a.appendLog(date + "/" + "U" + "/PostUploadActivity/post_add");
//                new AlertDialog.Builder(PostUploadActivity.this).setMessage("응답:" + imgPath).create().show();
                a.appendLog(date + "/" + "M" + "/boardActivity/0");
                Intent intent = new Intent(getApplicationContext(), boardActivity.class);
                startActivity(intent);
            }

            // 통신실패
            @Override
            public void onFailure(Call<String> call, Throwable t) { super.onFailure(call,t);
                Log.v("retrofit2", String.valueOf("error : " + t.toString()));
            }
        });
    }

    //edittext 아웃포커스
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View focusView = getCurrentFocus();
        if (focusView != null) {
            Rect rect = new Rect();
            focusView.getGlobalVisibleRect(rect);
            int x = (int) ev.getX(), y = (int) ev.getY();
            if (!rect.contains(x, y)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
                focusView.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}