package com.example.kklassmate;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Hashtable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.Gson;

//데이터베이스에서 채팅 내용 가져오는 Activity

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    MyAdapter mAdapter;
    EditText etText;
    ImageButton btnFinish, btnSend,btnDown;
    public static String stEmail,stCode,stDepart,content,Real_Answer;
    String Uid;
    FirebaseDatabase database;
    ArrayList<ChatModel> chatArrayList;
    Hashtable<String, String> master = new Hashtable<>();
    DatabaseReference databaseReference;

    // NetworkService 초기화
    private NetworkService networkService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        networkService = Controller.getInstance().getNetworkService();

        stEmail = getIntent().getStringExtra("email");
        Uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        btnFinish = (ImageButton) findViewById(R.id.btnFinish);
        btnSend = (ImageButton)findViewById(R.id.btnSend);
        btnDown = (ImageButton)findViewById(R.id.btnDown);
        etText = (EditText)findViewById(R.id.etText);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(Uid);


        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    stCode = ds.child("Code").getValue(String.class);
                    stDepart = ds.child("Depart").getValue(String.class);
                    Log.d(TAG, "code : " + stCode);
                    Log.d(TAG, "depart : " + stDepart);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        };

        databaseReference.addListenerForSingleValueEvent(valueEventListener);

        chatArrayList = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        layoutManager = new LinearLayoutManager(this);
        mAdapter = new MyAdapter(chatArrayList,stEmail);

        ((LinearLayoutManager) layoutManager).setStackFromEnd(true);

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        // 데이터를 읽어오는 코드
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                ChatModel chat = dataSnapshot.getValue(ChatModel.class);
                //String commentkey = dataSnapshot.getKey();
                chatArrayList.add(chat);
                mAdapter.notifyDataSetChanged();
                showItemList();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        };

        DatabaseReference ref = database.getInstance().getReference().child("Chatrooms").child(Uid);
        ref.addChildEventListener(childEventListener);


        // 메시지 내용과 email을 데이터베이스에 넣는 코드
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String stText = etText.getText().toString();
                content = stText;
                etText.setText(null);

                Hashtable<String, String> numbers = new Hashtable<String, String>();
                numbers.put("email", stEmail); // 이메일
                numbers.put("text",content); // 내용
                numbers.put("code",stCode); // 학번
                numbers.put("depart",stDepart); // 학과

                joinSentence();

                database.getInstance().getReference().child("Chatrooms").child(Uid).push().setValue(numbers).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mAdapter.notifyDataSetChanged();
                        showItemList();
                        master.put("email", "master@naver.com");
                        master.put("text", Real_Answer);

                        database.getInstance().getReference().child("Chatrooms").child(Uid).push().setValue(master);

                    }
                });

            }
        });                                                                                                                                                                                    

    recyclerView.setAdapter(mAdapter);
        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showItemList();
            }
        });
    }

    public void showItemList(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                recyclerView.scrollToPosition(mAdapter.getItemCount()-1);
            }
        }, 100);

    }

    // Interface 사용
    private void joinSentence() {
        if (networkService != null) {
            DataModel join = new DataModel(content, stDepart, Integer.valueOf(stCode));
            Call<ResultModel> joinSentenceCall = networkService.joinSentence(join);
            joinSentenceCall.enqueue(new Callback<ResultModel>() {
                @Override
                public void onResponse(Call<ResultModel> call, Response<ResultModel> response) {
                    if (response.isSuccessful()) {
                        // 요청 성공하면 resultModel에서의 Answer 부분만 저장하여 출력함
                        ResultModel Answer = response.body();
                        String Json_Answer = new Gson().toJson(Answer);
                        ResultModel resultModel = new Gson().fromJson(Json_Answer,ResultModel.class);
                        Real_Answer = resultModel.get("Answer");
                        Real_Answer = Real_Answer.replaceAll(System.getProperty("line.separator"), " ");

                    }

                    else {
                        Log.e("Fail", new Gson().toJson(response.errorBody()));
                        // 요청 실패, 응답 코드 봐야 함

                        if (response.code() == 500) ;

                        else if (response.code() == 503) ;

                        else if (response.code() == 401) ;

                        else if (response.code() == 404) ;
                    }
                }

                @Override
                public void onFailure(Call<ResultModel> call, Throwable t) {
                    // 요청 실패
                    Log.e("onFailure", t.toString());
                }

            });

        }

    }
}