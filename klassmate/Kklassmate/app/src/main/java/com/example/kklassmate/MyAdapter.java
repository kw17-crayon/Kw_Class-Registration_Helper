package com.example.kklassmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private ArrayList<ChatModel> mDataset;
    String stMyEmail="";

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public MyViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.tvChat);
        }
    }
    @Override
    public int getItemViewType(int position) {
        if (mDataset.get(position).email.equals(stMyEmail)){
            return 1; // 내꺼
        }else{
            return 2;
        }
    }

    public MyAdapter(ArrayList<ChatModel> myDataset,String stEmail) {
        mDataset = myDataset;
        this.stMyEmail = stEmail;
    }

    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_text_view, parent, false);
        if (viewType == 1){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_text_view, parent, false);
        }

        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // 실제 아이템들을 어떻게 보여줄 것인지
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.textView.setText(mDataset.get(position).getText()); // getText : ChatModel에 있는 Text함수
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
