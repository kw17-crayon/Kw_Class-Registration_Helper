package com.example.kklassmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Hashtable;

// 이메일, 비밀번호, 학번, 학과 입력하여 회원가입 하는 Activity
public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    ImageButton signup_btn;
    EditText signup_email, signup_code,signup_password,signup_depart;
    private FirebaseAuth Auth;
    Hashtable<String, String> Users;
    FirebaseDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signup_btn = (ImageButton)findViewById(R.id.signup_btn);
        signup_email = (EditText)findViewById(R.id.signup_email);
        signup_code = (EditText)findViewById(R.id.signup_code);
        signup_depart = (EditText)findViewById(R.id.signup_Depart);
        signup_password = (EditText)findViewById(R.id.signup_password);
        Auth = FirebaseAuth.getInstance();

        signup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String stEmail = signup_email.getText().toString();
                final String stPassword = signup_password.getText().toString();
                final String stCode = signup_code.getText().toString();
                final String stDepart = signup_depart.getText().toString();

                if(stEmail.isEmpty()){
                    Toast.makeText(SignupActivity.this,"이메일을 입력해주세요.",Toast.LENGTH_LONG).show();
                    return;
                }
                if(stPassword.isEmpty()){
                    Toast.makeText(SignupActivity.this,"비밀번호를 입력해주세요.",Toast.LENGTH_LONG).show();
                    return;
                }
                if(stPassword.length() < 6){
                    Toast.makeText(SignupActivity.this,"비밀번호는 6자리 이상으로 설정해주세요.",Toast.LENGTH_LONG).show();
                    return;
                }
                if(stCode.length() != 10){
                    Toast.makeText(SignupActivity.this,"학번을 확인해주세요.",Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(SignupActivity.this,"회원가입 완료", Toast.LENGTH_SHORT).show();
                Auth.createUserWithEmailAndPassword(stEmail, stPassword).addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {

                                    String uid = Auth.getInstance().getCurrentUser().getUid();
                                    Users = new Hashtable<String, String>();
                                    Users.put("Email",stEmail);
                                    Users.put("Code",stCode);
                                    Users.put("Depart",stDepart);

                                    database.getInstance().getReference().child("Users").child(uid).push().setValue(Users).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            SignupActivity.this.finish();
                                        }
                                    });
                                } else {
                                    Toast.makeText(SignupActivity.this, "본인 확인 실패", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}