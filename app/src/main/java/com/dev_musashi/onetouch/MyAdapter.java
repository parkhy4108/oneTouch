package com.dev_musashi.onetouch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MyAdapter extends BaseAdapter {

    Context mContext = null;
    LayoutInflater mLayoutInflater = null;
    ArrayList<Listitem> table_info;
    ListView listview;
    ArrayList<Listitem> tableDataList;

    public MyAdapter(Context context, ArrayList<Listitem>data){
        mContext = context;
        table_info = data;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return table_info.size();
    }

    @Override
    public Listitem getItem(int position) {
        return table_info.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        @SuppressLint({"ViewHolder", "InflateParams"}) View view = mLayoutInflater.inflate(R.layout.activity_listitem,null);
        TextView table_text = view.findViewById(R.id.table_text);
        TextView time = view.findViewById(R.id.time);
        table_text.setText(table_info.get(position).getTable_info());
        time.setText(table_info.get(position).getForamtDate());
        return view;
    }

    public void upDateItemList(ArrayList<Listitem> table_info){
        this.table_info = table_info;
        notifyDataSetChanged();
    }
}
