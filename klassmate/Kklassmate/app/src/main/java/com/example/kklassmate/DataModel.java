package com.example.kklassmate;

//보내고 싶은 데이터에 맞추어 DataModel 객체 생성
public class DataModel {
    private String Senten;
    private String depart;
    private int code;

    public  DataModel(String senten, String depart, int code){

        this.Senten = senten;
        this.depart = depart;
        this.code = code;

    }

    public void setSentence(String Senten) {
        this.Senten = Senten;
    }

    public void setDepart(String depart) {
        this.depart = depart;
    }

    public void setCode(int code) {
        this.code = code;
    }


}
