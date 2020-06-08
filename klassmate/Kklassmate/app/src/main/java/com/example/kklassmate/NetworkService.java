package com.example.kklassmate;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

//원하는 Call에 맞추어 NetworkService 에 interface 추가함

public interface NetworkService {
    //POST method 으로 DataModel 보냄
    @POST("/input")
    Call<ResultModel> joinSentence(@Body DataModel dataModel);
}
