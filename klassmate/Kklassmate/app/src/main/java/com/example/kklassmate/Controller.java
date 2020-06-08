package com.example.kklassmate;
import android.app.Application;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Controller extends Application {
    private String baseUrl;
    private static Controller instance;
    public static Controller getInstance() {return instance;}
    @Override
    public void onCreate(){
        super.onCreate();
        Controller.instance = this;
        buildNetworkService();}

    private NetworkService networkService;
    public NetworkService getNetworkService() {return networkService;}

    public void buildNetworkService(){
        synchronized (Controller.class){
            if(networkService == null){
                baseUrl = "http://User_URL:User_Port"; //원하는 URL&Port 작성
                Gson gson = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .create();
                GsonConverterFactory factory = GsonConverterFactory.create(gson);
                // Timeout 설정
                OkHttpClient okHttpClient = new OkHttpClient.Builder()
                        .connectTimeout(2, TimeUnit.MINUTES)
                        .readTimeout(2, TimeUnit.MINUTES)
                        .writeTimeout(2, TimeUnit.MINUTES)
                        .build();
                // Retrofit 설정
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(factory)
                        .build();
                // networkService create
                networkService = retrofit.create(NetworkService.class);
            }
        }
    }

}
