package com.example.kklassmate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

//수강신청자료집, 채팅화면, 로그아웃, 어플종료 중 선택하는 Activity
public class ChoiceActivity extends AppCompatActivity {
    String stEmail;
    ImageButton btnChat, btnDownload, btnFinish, btnlogout ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        stEmail = getIntent().getStringExtra("email");
        btnChat = (ImageButton)findViewById(R.id.btnChat);
        btnDownload = (ImageButton)findViewById(R.id.btnDownload);
        btnlogout = (ImageButton)findViewById(R.id.button);
        btnFinish = (ImageButton)findViewById(R.id.btnFinish);

        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            // 로그인 버튼 클릭 시, 발생하는 이벤트
            public void onClick(View view) {
                Intent in = new Intent(ChoiceActivity.this,ChatActivity.class);
                in.putExtra("email",stEmail);
                startActivity(in);
            }
        });

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent (ChoiceActivity.this,pdfActivity.class);
                startActivity(intent);
            }
        });


        btnlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChoiceActivity.this, MainActivity.class);
                startActivity(intent);
                ChoiceActivity.this.finish();
            }
        });

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}