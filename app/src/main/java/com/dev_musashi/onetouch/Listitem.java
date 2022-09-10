package com.dev_musashi.onetouch;

public class Listitem {
    private String table_info;
    private String foramtDate;

    public Listitem(String table_info, String foramtDate){
        this.table_info = table_info;
        this.foramtDate = foramtDate;
    }  //생성자 생성

    public String getTable_info(){
        return this.table_info;
    }
    public  String getForamtDate(){
        return  this.foramtDate;
    }

}
