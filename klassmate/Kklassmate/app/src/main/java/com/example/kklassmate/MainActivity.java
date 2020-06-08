package com.example.kklassmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// 메인화면
// 로그인 및 회원가입 Activity
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    EditText etlogin, etpassword;
    ImageButton btnlogin;
    ImageButton btnregister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etlogin = (EditText)findViewById(R.id.login);
        etpassword = (EditText)findViewById(R.id.password);
        btnlogin = (ImageButton)findViewById(R.id.btnlogin);
        btnregister = (ImageButton)findViewById(R.id.register);
        mAuth = FirebaseAuth.getInstance();

        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String stEmail = etlogin.getText().toString();
                final String stPassword = etpassword.getText().toString();

                if(stEmail.isEmpty()){
                    Toast.makeText(MainActivity.this,"이메일을 입력해주세요.",Toast.LENGTH_LONG).show();
                    return;
                }
                if(stPassword.isEmpty()){
                    Toast.makeText(MainActivity.this,"비밀번호를 입력해주세요.",Toast.LENGTH_LONG).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(stEmail, stPassword)
                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    String stUserEmail = user.getEmail();
                                    Intent choice = new Intent(MainActivity.this,ChoiceActivity.class);
                                    choice.putExtra("email",stUserEmail);
                                    Toast.makeText(MainActivity.this, "Kklassmate에 오신 것을 환영합니다.", Toast.LENGTH_SHORT).show();
                                    startActivity(choice);
                                    MainActivity.this.finish();
                                } else {
                                    Toast.makeText(MainActivity.this, "로그인을 실패하였습니다.", Toast.LENGTH_SHORT).show();
                                    etlogin.setText(null);
                                    etpassword.setText(null);
                                }
                            }
                        });
            }
        });

        btnregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }
}