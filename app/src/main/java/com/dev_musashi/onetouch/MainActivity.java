package com.dev_musashi.onetouch;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    public static Object current_titleButton_Tag;
    private SharedPreferences pref;
    private SharedPreferences pref_Table;
    private SharedPreferences pref_history;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private MediaScanner ms = MediaScanner.newInstance(MainActivity.this);
    Intent intent_camera;
    int count;
    int table_count;
    FrameLayout Frame;
    ImageView iv_result;
    MyAdapter myAdapter;
    TableLayout[] TOP_TableLayout;
    ListView listview;
    ArrayList<Listitem> tableDataList;
    Button btn_capture;
    ImageButton btn_gallery,  btn_form_add;
    HorizontalScrollView form_scrollview;
    LinearLayout layout_form_btn;
    int i;
    private MediaScanner mMediaScanner; // 사진 저장 시 갤러리 폴더에 바로 반영사항을 업데이트 시켜주려면 이 것이 필요하다(미디어 스캐닝)

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intent_camera = new Intent(MainActivity.this, AndroidCamera.class);

        // 사진 저장 후 미디어 스캐닝을 돌려줘야 갤러리에 반영됨.
        final Intent intent = new Intent(this,AndroidCamera.class);
        pref_history = getSharedPreferences("history",MODE_PRIVATE);
        pref = getSharedPreferences("file", MODE_PRIVATE);
        final SharedPreferences.Editor editor = pref.edit();
        count = pref.getInt("current_count", 0);
        TOP_TableLayout = new TableLayout[count];
        iv_result = findViewById(R.id.iv_result);
        form_scrollview = findViewById(R.id.form_scrollview);
        btn_gallery = findViewById(R.id.btn_gallery);
        btn_capture = findViewById(R.id.btn_capture);
        btn_form_add = findViewById(R.id.btn_form_add);
        Frame = findViewById(R.id.frame);
        layout_form_btn = findViewById(R.id.layout_form_btn);
        listview = findViewById(R.id.listview);
        onResume();
        this.InitializeTableData();                                                             //테이블데이터 초기화
        myAdapter = new MyAdapter(this, tableDataList);
        listview.setAdapter(myAdapter);
        if (tableDataList.size() > 0) {    //데이터가 추가, 수정되었을때
            myAdapter.notifyDataSetChanged();
        } else {    //뷰에 표시될 데이타가 없을때
            myAdapter.notifyDataSetInvalidated();
        }

        final TextView default_tv = new TextView(MainActivity.this);
        final TextView default_table = new TextView(MainActivity.this);
        default_tv.setText("<  + 버튼을 눌러 서식을 추가  >");
        default_table.setText("<  저장된 서식이 없습니다  >");

        if (TOP_TableLayout.length == 0) {
            layout_form_btn.addView(default_tv);
            Frame.addView(default_table);
        }               //처음 count 불러와서 count 0이면 서식 추가하시오 addview
        else {
            restore_btn();
        }                                                           // 아니면 데이터 불러오기

        btn_gallery.setClickable(true);
        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setType("image/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    // TODO : 미디어 스캔
                    ms.mediaScanning(String.format("/sdcard/Picture/%d.jpg", System.currentTimeMillis()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                }
                startActivity(intent);
            }
        });                                                 // 갤러리 진입 버튼 갤러리 진입만 확인용

        btn_form_add.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                layout_form_btn.removeView(default_tv);
                Frame.removeView(default_table);
                Dialog_add_delete();
            }
        });

        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(intent_camera,REQUEST_IMAGE_CAPTURE);
            }
        });         //카메라 버튼 클릭

        editor.apply();

    }
//
//    public static byte[] binaryStringToByteArray(String s) {
//        int count = s.length() / 8;
//        byte[] b = new byte[count];
//        for (int i = 1; i < count; ++i) {
//            String t = s.substring((i - 1) * 8, i * 8);
//            b[i - 1] = binaryStringToByte(t);
//        }
//        return b;
//    }
//
//    public static byte binaryStringToByte(String s) {
//        byte ret = 0, total = 0;
//        for (int i = 0; i < 8; ++i) {
//            ret = (s.charAt(7 - i) == '1') ? (byte) (1 << i) : 0;
//            total = (byte) (ret | total);
//        }
//        return total;
//    }

    @Override
    public void onResume(){
        super.onResume();
        InitializeTableData();
        myAdapter = new MyAdapter(this, tableDataList);
        listview.setAdapter(myAdapter);
        listview.setSelection(myAdapter.getCount()-1);
    }               //리스트뷰 갱신

    public void Dialog_add_delete() {
//        /*
//        count 3 Table[3] 이면 원소 0,1,2,3 4개의 배열을 가진 테이블이 만들어진다
//        0번테이블 1번테이블 2번테이블 3번테이블 ---> table_count = 4 (테이블갯수)
//        + 4번테이블 추가 하면
//        temp_테이블에 총 5개의 테이블이 저장되어야 하므로 5개의 테이블 생성
//        temptable의 배열 갯수 count +1 => 5개
//        temptable의 0부터 3까지 기존의 테이블을 복사한다.
//        temptable[count].addview =>  이렇게 하면 count 4이기때문에
//        */
        pref_Table = getSharedPreferences(count + "Table", MODE_PRIVATE);
        final int TOP_Table_length = TOP_TableLayout.length;
        final int table_count = count;

        final int New_Table_length = TOP_Table_length + 1;
        final FrameLayout Frame = findViewById(R.id.frame);
        final ScrollView Scollview_in = new ScrollView(MainActivity.this);
        final ScrollView Scollview_out = new ScrollView(MainActivity.this);
        final LinearLayout Linear_wrap = new LinearLayout(MainActivity.this);
        final TableLayout[] temp_TableLayout = new TableLayout[table_count + 1];              //temp테이블

        final LinearLayout Linear_add_delete = new LinearLayout(MainActivity.this);
        final Button title_button = new Button(MainActivity.this);
        final SharedPreferences.Editor editor = pref.edit();
        final SharedPreferences.Editor editor_Table = pref_Table.edit();


        final int select_count = count;             // select count는 서식을 만들었을때의 카운트만 들어감 0번테이블을 만들면 이때 select_count 는 0

        final EditText title = new EditText(MainActivity.this);
        final ScrollView.LayoutParams scrollview_param = new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final ScrollView.LayoutParams scroll_in_param = new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        final LinearLayout.LayoutParams Linear_wrap_param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        FrameLayout container = new FrameLayout(MainActivity.this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(230,0,230,0);
        title.setLayoutParams(params);
        title.setSingleLine();
        title.setHint("세종로 현장");
        title.setGravity(Gravity.CENTER);
        title.setPadding(30,0,0,30);
        container.addView(title);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("서식 저장");
        builder.setMessage("서식명을 입력하시오");
        builder.setView(container);
        builder.setPositiveButton("저장", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                for (int i = 0; i < New_Table_length; i++) {
                    temp_TableLayout[i] = new TableLayout(MainActivity.this);
                }                                                                                                    // 초기화 시킴
                System.arraycopy(TOP_TableLayout, 0, temp_TableLayout, 0, TOP_Table_length);        // 기존에 존재하는 테이블을 복사해서 temp로 만든 table에 복사

                temp_TableLayout[count].setLayoutParams(scroll_in_param);
                Linear_add_delete.setLayoutParams(Linear_wrap_param);
                Scollview_in.setLayoutParams(Linear_wrap_param);
                Scollview_out.setLayoutParams(scrollview_param);
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
                String formatDate = sdfNow.format(date);

                final Handler Handler = new Handler();          //scroll 딜레이를 위한 핸들러
                String text = title.getText().toString();           // 서식명을 받아온다
                title_button.setTag(formatDate);
                title_button.setText(text);                        //받아온 txt로 서식명 지정
                title_button.setSelected(false);
                title_button.setBackgroundResource(R.drawable.click);
                LinearLayout.LayoutParams title_param = new LinearLayout.LayoutParams(210, ViewGroup.LayoutParams.MATCH_PARENT);
                title_param.leftMargin = 5;
                title_param.rightMargin = 5;
                title_param.bottomMargin = 10;
                title_button.setPadding(0, 0, 0, 0);
                title_button.setLayoutParams(title_param);
                title_button.setTextSize(12);
                title_button.setTextColor(Color.WHITE);
                layout_form_btn.addView(title_button);      //btn 배치

                int tr_length = pref_Table.getInt(table_count + "Table_Total_Row", 5); //몇번테이블의 로우가 몇개 있는지 확인후 로드
                editor.putString(table_count + "title", text).apply();        // ex 0Title == test 값 저장
                editor.apply();


                Handler.postDelayed(new Runnable() {
                    public void run() {
                        form_scrollview.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                    }
                }, 100);                        // 스크롤뷰 추가 후 딜레이로 자동 뒤로 보내기

                final TableRow[] row = new TableRow[tr_length];  // 테이블로우 5개 기본 생성
                final TableRow.LayoutParams td1param = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
                final TableRow.LayoutParams td2param = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
                final TableRow.LayoutParams td3param = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);


                td1param.weight = 1.6f;                 // 테이블로우에 들어갈 1번째 td 뷰 param weight 설정
                td2param.weight = 7.5f;                 // 테이블로우에 들어갈 2번째 td 뷰 param weight 설정
                td3param.weight = 0.9f;                 // 테이블로우에 들어갈 3번째 td 뷰 param weight 설정
                td3param.setMargins(0,10,0,0);

                for (int tr = 0; tr < tr_length; tr++) {         //로우가 0부터 4까지 5개 tr_length=5 보다 작게(처음에 tr이 5개다 설정)
                    if (tr == 0) {
                        row[tr] = new TableRow(MainActivity.this);
                        final EditText tr0_column0 = new EditText(MainActivity.this);
                        final EditText tr0_column1 = new EditText(MainActivity.this);
                        ImageButton tr0_column2 = new ImageButton(MainActivity.this);
                        final int finalTr = tr;
                        for (int td = 0; td < 3; td++) {
                            if (td == 0) {
                                final int finalTd = td;
                                tr0_column0.setText("공사명");
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", "공사명").apply();
                                tr0_column0.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr0_column0 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr + "row" + finalTd + "column", txt_tr0_column0).apply();
                                    }
                                });
                                tr0_column0.setTextColor(Color.BLACK);
                                tr0_column0.setTextSize(12);
                                tr0_column0.setPadding(0, 20, 0, 50);
                                tr0_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                tr0_column0.setLayoutParams(td1param);
                                row[tr].addView(tr0_column0);
                            } else if (td == 1) {
                                final int finalTd1 = td;
                                tr0_column1.setText(null);
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", null).apply();
                                tr0_column1.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr0_column1 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr + "row" + finalTd1 + "column", txt_tr0_column1).apply();
                                    }
                                });
                                tr0_column1.setTextColor(Color.BLACK);
                                tr0_column1.setTextSize(12);
                                tr0_column1.setPadding(20, 20, 0, 50);
                                tr0_column1.setLayoutParams(td2param);
                                row[tr].addView(tr0_column1);
                            } else {
                                tr0_column2.setPadding(0, 0, 0, 0);
                                tr0_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                                tr0_column2.setLayoutParams(td3param);
                                row[tr].addView(tr0_column2);
                                tr0_column2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        tr0_column0.setText(null);
                                        tr0_column1.setText(null);
                                        editor_Table.remove(i + "Table" + finalTr + "row" + 0 + "column");
                                        editor_Table.remove(i + "Table" + finalTr + "row" + 1 + "column");
                                    }
                                });
                            }
                        }
                        temp_TableLayout[table_count].addView(row[tr]);
                    }             // 0번째 tr
                    else if (tr == 1) {
                        row[tr] = new TableRow(MainActivity.this);
                        final EditText tr1_column0 = new EditText(MainActivity.this);
                        final EditText tr1_column1 = new EditText(MainActivity.this);
                        ImageButton tr1_column2 = new ImageButton(MainActivity.this);
                        final int finalTr1 = tr;
                        for (int td = 0; td < 3; td++) {
                            if (td == 0) {
                                final int finalTd1 = td;
                                tr1_column0.setText("공  종");
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", "공  종").apply();
                                tr1_column0.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr1_column0 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr1 + "row" + finalTd1 + "column", txt_tr1_column0).apply();
                                    }
                                });
                                tr1_column0.setTextColor(Color.BLACK);
                                tr1_column0.setTextSize(12);
                                tr1_column0.setPadding(0, 20, 0, 50);
                                tr1_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                tr1_column0.setLayoutParams(td1param);
                                row[tr].addView(tr1_column0);
                            }
                            else if (td == 1) {
                                final int finalTd1 = td;
                                tr1_column1.setText(null);
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", null).apply();
                                tr1_column1.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr1_column1 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr1 + "row" + finalTd1 + "column", txt_tr1_column1).apply();
                                    }
                                });
                                tr1_column1.setTextColor(Color.BLACK);
                                tr1_column1.setTextSize(12);
                                tr1_column1.setPadding(20, 20, 0, 50);
                                tr1_column1.setLayoutParams(td2param);
                                row[tr].addView(tr1_column1);
                            }
                            else {
                                tr1_column2.setPadding(0, 0, 0, 0);
                                tr1_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                                tr1_column2.setLayoutParams(td3param);
                                row[tr].addView(tr1_column2);
                                tr1_column2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        tr1_column0.setText("");
                                        tr1_column1.setText("");
                                    }
                                });
                            }
                        }
                        temp_TableLayout[table_count].addView(row[tr]);
                    }           // 1번째 tr
                    else if (tr == 2) {
                        row[tr] = new TableRow(MainActivity.this);
                        final EditText tr2_column0 = new EditText(MainActivity.this);
                        final EditText tr2_column1 = new EditText(MainActivity.this);
                        ImageButton tr2_column2 = new ImageButton(MainActivity.this);
                        final int finalTr2 = tr;
                        for (int td = 0; td < 3; td++) {
                            if (td == 0) {
                                final int finalTd2 = td;
                                tr2_column0.setText("위  치");
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", "위  치").apply();
                                tr2_column0.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr2_column0 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr2 + "row" + finalTd2 + "column", txt_tr2_column0).apply();
                                    }
                                });
                                tr2_column0.setTextColor(Color.BLACK);
                                tr2_column0.setTextSize(12);
                                tr2_column0.setPadding(0, 20, 0, 50);
                                tr2_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                tr2_column0.setLayoutParams(td1param);
                                row[tr].addView(tr2_column0);
                            } else if (td == 1) {
                                final int finalTd2 = td;
                                tr2_column1.setText(null);
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", null).apply();
                                tr2_column1.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr2_column1 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr2 + "row" + finalTd2 + "column", txt_tr2_column1).apply();
                                    }
                                });
                                tr2_column1.setTextColor(Color.BLACK);
                                tr2_column1.setTextSize(12);
                                tr2_column1.setPadding(20, 20, 0, 50);
                                tr2_column1.setLayoutParams(td2param);
                                row[tr].addView(tr2_column1);
                            } else {
                                tr2_column2.setPadding(0, 0, 0, 0);
                                tr2_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                                tr2_column2.setLayoutParams(td3param);
                                row[tr].addView(tr2_column2);
                                tr2_column2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        tr2_column0.setText("");
                                        tr2_column1.setText("");
                                    }
                                });
                            }
                        }
                        temp_TableLayout[count].addView(row[tr]);
                    }           // 2번째 tr
                    else if (tr == 3) {
                        row[tr] = new TableRow(MainActivity.this);
                        final EditText tr3_column0 = new EditText(MainActivity.this);
                        final EditText tr3_column1 = new EditText(MainActivity.this);
                        ImageButton tr3_column2 = new ImageButton(MainActivity.this);
                        final int finalTr3 = tr;
                        for (int td = 0; td < 3; td++) {
                            if (td == 0) {
                                final int finalTd3 = td;
                                tr3_column0.setText("내  용");
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", "내  용").apply();
                                tr3_column0.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr3_column0 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr3 + "row" + finalTd3 + "column", txt_tr3_column0).apply();
                                    }
                                });
                                tr3_column0.setTextColor(Color.BLACK);
                                tr3_column0.setTextSize(12);
                                tr3_column0.setPadding(0, 20, 0, 50);
                                tr3_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                tr3_column0.setLayoutParams(td1param);
                                row[tr].addView(tr3_column0);
                            } else if (td == 1) {
                                final int finalTd3 = td;
                                tr3_column1.setText(null);
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", null).apply();
                                tr3_column1.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr3_column1 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr3 + "row" + finalTd3 + "column", txt_tr3_column1).apply();
                                    }
                                });
                                tr3_column1.setTextColor(Color.BLACK);
                                tr3_column1.setTextSize(12);
                                tr3_column1.setPadding(20, 20, 0, 50);
                                tr3_column1.setLayoutParams(td2param);
                                row[tr].addView(tr3_column1);
                            } else {
                                tr3_column2.setPadding(0, 0, 0, 0);
                                tr3_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                                tr3_column2.setLayoutParams(td3param);
                                row[tr].addView(tr3_column2);
                                tr3_column2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        tr3_column0.setText("");
                                        tr3_column1.setText("");
                                    }
                                });
                            }
                        }
                        temp_TableLayout[table_count].addView(row[tr]);
                    }           // 3번째 tr
                    else {
                        row[tr] = new TableRow(MainActivity.this);
                        final EditText tr4_column0 = new EditText(MainActivity.this);
                        final EditText tr4_column1 = new EditText(MainActivity.this);
                        ImageButton tr4_column2 = new ImageButton(MainActivity.this);
                        final int finalTr4 = tr;
                        for (int td = 0; td < 3; td++) {
                            if (td == 0) {
                                final int finalTd4 = td;
                                tr4_column0.setText("일  자");
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", "일  자").apply();
                                tr4_column0.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr4_column0 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr4 + "row" + finalTd4 + "column", txt_tr4_column0).apply();
                                    }
                                });
                                tr4_column0.setTextColor(Color.BLACK);
                                tr4_column0.setTextSize(12);
                                tr4_column0.setPadding(0, 20, 0, 50);
                                tr4_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                tr4_column0.setLayoutParams(td1param);
                                row[tr].addView(tr4_column0);
                            } else if (td == 1) {
                                final int finalTd4 = td;
                                tr4_column1.setText(null);
                                editor_Table.putString(table_count + "Table" + tr + "row" + td + "column", null).apply();
                                tr4_column1.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr4_column1 = s.toString();
                                        editor_Table.putString(table_count + "Table" + finalTr4 + "row" + finalTd4 + "column", txt_tr4_column1).apply();
                                    }
                                });
                                tr4_column1.setTextColor(Color.BLACK);
                                tr4_column1.setTextSize(12);
                                tr4_column1.setPadding(20, 20, 0, 50);
                                tr4_column1.setLayoutParams(td2param);
                                row[tr].addView(tr4_column1);
                            } else {
                                tr4_column2.setPadding(0, 0, 0, 0);
                                tr4_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                                tr4_column2.setLayoutParams(td3param);
                                row[tr].addView(tr4_column2);
                                tr4_column2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        tr4_column0.setText("");
                                        tr4_column1.setText("");
                                    }
                                });
                            }
                        }
                        temp_TableLayout[table_count].addView(row[tr]);
                    }// 4번째 tr
                }           //tr 0-4 5개 만들고 for문 닫기

                LinearLayout.LayoutParams Linear_param1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
                LinearLayout.LayoutParams Linear_param2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
                Linear_param1.weight = 1.6f;
                Linear_param2.weight = 8.4f;

                ImageView add_tr_button = new ImageView(MainActivity.this);
                add_tr_button.setPadding(0, 0, 0, 0);
                add_tr_button.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_plus));
                add_tr_button.setLayoutParams(Linear_param1);

                View view = new View(MainActivity.this);
                view.setLayoutParams(Linear_param2);
                Linear_add_delete.setOrientation(LinearLayout.HORIZONTAL);
                Linear_add_delete.addView(add_tr_button);
                Linear_add_delete.addView(view);

                add_tr_button.setClickable(true);
                add_tr_button.setOnClickListener(new View.OnClickListener() {   //행 추가 버튼 클릭했을때
                    @Override
                    public void onClick(View v) { // 테이블로우 추가할때는 만든 후여서 table_count = 1임
                        final int tr_length = pref_Table.getInt(table_count + "Table_Total_Row", 5); //0번테이블의 로우가 몇개 있는지 확인후 로드
                        int new_tr_length = tr_length + 1;   // tr은 6 -> tr_count=5 +1
                        editor_Table.putInt(table_count + "Table_Total_Row", new_tr_length).apply();  //0TableTotalRow => 확인을 위한 tr_count = 6으로 저장
                        final TableRow tr_row = new TableRow(MainActivity.this);
                        final EditText tr_column0 = new EditText(MainActivity.this);
                        final EditText tr_column1 = new EditText(MainActivity.this);
                        ImageButton tr_column2 = new ImageButton(MainActivity.this);
                        for (int td = 0; td < 3; td++) {
                            if (td == 0) {
                                final int finalTd0 = td;
                                tr_column0.setText(null);
                                editor_Table.putString(table_count + "Table" + tr_length + "row" + td + "column", null).apply();
                                tr_column0.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr_column0 = s.toString();
                                        editor_Table.putString(table_count + "Table" + tr_length + "row" + finalTd0 + "column", txt_tr_column0).apply();
                                    }
                                });
                                tr_column0.setTextColor(Color.BLACK);
                                tr_column0.setTextSize(12);
                                tr_column0.setPadding(0, 20, 0, 50);
                                tr_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                tr_column0.setLayoutParams(td1param);
                                tr_row.addView(tr_column0);

                                // 0Table5row0column은  tr_column2의 String 값 받아서 저장
                            } else if (td == 1) {
                                final int finalTd1 = td;
                                tr_column1.setText(null);
                                editor_Table.putString(table_count + "Table" + tr_length + "row" + td + "column", null).apply();
                                tr_column1.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        String txt_tr_column1 = s.toString();
                                        editor_Table.putString(table_count + "Table" + tr_length + "row" + finalTd1 + "column", txt_tr_column1).apply();
                                    }
                                });
                                tr_column1.setTextColor(Color.BLACK);
                                tr_column1.setTextSize(12);
                                tr_column1.setPadding(20, 20, 0, 50);
                                tr_column1.setLayoutParams(td2param);
                                tr_row.addView(tr_column1);

                                //4Table5row1column은 tr_column1의 String 값 받아서 저장
                            } else {
                                tr_column2.setPadding(0, 0, 0, 0);
                                tr_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove)); //색깔이랑 length조절하셈
                                tr_column2.setLayoutParams(td3param);
                                tr_row.addView(tr_column2);

                                tr_column2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) { // 추가한 행을 지우는 delete버튼 클릭시
                                        int new_tr_length = pref_Table.getInt(table_count + "Table_Total_Row", 5);
                                        temp_TableLayout[table_count].removeView(tr_row); // 테이블에 추가한 tr_row view 제거
                                        editor_Table.putInt(table_count + "Table_Total_Row", new_tr_length - 1).apply();
                                        //ex. 4TableTotalRow 하나 줄여서 저장
                                        editor_Table.remove(table_count + "Table" + new_tr_length + "row" + 0 + "column").apply();
                                        //ex. 4Table5row0column 값 삭제
                                        editor_Table.remove(table_count + "Table" + new_tr_length + "row" + 1 + "column").apply();
                                        //ex. 4Table5row1column 값 삭제
                                    }
                                });
                            }
                        }
                        Handler.postDelayed(new Runnable() {
                            public void run() {
                                Scollview_out.fullScroll(HorizontalScrollView.FOCUS_DOWN);       // scollview delay
                            }
                        }, 100);

                        temp_TableLayout[table_count].addView(tr_row);
                    }
                });

                temp_TableLayout[table_count].setTag(formatDate);
                SharedPreferences pref_tag = getSharedPreferences("TAG", MODE_PRIVATE);
                TOP_TableLayout = temp_TableLayout;             //temp로 하나 추가한 table배열을 원 배열에 옮긴다.

                Scollview_in.removeAllViews();
                Linear_wrap.removeAllViews();
                Scollview_out.removeAllViews();
                Frame.removeAllViews();

                Scollview_in.addView(temp_TableLayout[table_count]);             //테이블을 안의 scrollview에 배치
                Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
                Linear_wrap.addView(Linear_add_delete);             //리니어에 행추가버튼 배치
                Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
                Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
                Frame.addView(Scollview_out);               //프레임레이아웃에 전체스크롤뷰저장
                count++;
                editor.putInt("current_count", count).apply();      // 현재 count = count 값 저장
            }// 저장 onclick 닫기
        }); //다이얼로그 저장 닫기

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {                      // Dialog 취소
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();

        title_button.setClickable(true); // 서식 버튼 클릭하면 frame view 보여주기
        title_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String button_id  = (String) title_button.getTag();

                Scollview_in.removeAllViews();
                Linear_wrap.removeAllViews();
                Scollview_out.removeAllViews();
                Frame.removeAllViews();

                Object title_button_tag = title_button.getTag();
                if(current_titleButton_Tag == null){
                    current_titleButton_Tag = title_button_tag;
                    title_button.setSelected(true);
                }
                else {
                    if (current_titleButton_Tag == title_button_tag) {
                        current_titleButton_Tag = title_button_tag;
                        title_button.setSelected(true);
                    }
                    else{
                        Button btn = (Button) layout_form_btn.findViewWithTag(current_titleButton_Tag);
                        btn.setSelected(false);

                        title_button.setSelected(true);
                        current_titleButton_Tag = title_button_tag;
                        btn.setBackgroundResource(R.drawable.click);
                    }
                }

                int select_count = 0;
                for (int i=0;i<TOP_TableLayout.length;i++){
                    if (title_button.getTag() == TOP_TableLayout[i].getTag()  ) {
                        select_count = i;
                    }
                }

                for (TableLayout tableLayout : temp_TableLayout) {                   // 자식이 연결된 부모뷰 초기화 해주기
                    if (tableLayout.getParent() != null) {
                        ((ViewGroup) tableLayout.getParent()).removeView(tableLayout);
                    }
                }

                intent_camera.putExtra("select_count", select_count);
                Scollview_in.addView(temp_TableLayout[select_count]);             //테이블을 안의 scrollview에 배치
                Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
                Linear_wrap.addView(Linear_add_delete);             //리니어에 행추가버튼 배치
                Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
                Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
                Frame.addView(Scollview_out);
            }
        });

        title_button.setLongClickable(true);
        title_button.setOnLongClickListener(new View.OnLongClickListener() {    // 타이틀 버튼 long 클릭 시
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("삭제하시겠습니까?");
                builder.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        layout_form_btn.removeView(title_button);
                        TableLayout[] temp_TableLayout = new TableLayout[TOP_TableLayout.length - 1];

                        String button_tag =(String) title_button.getTag();

                        if (button_tag == TOP_TableLayout[0].getTag())  {
                            if (temp_TableLayout.length == 0) {
                                Scollview_in.removeAllViews();
                                Linear_wrap.removeAllViews();
                                Scollview_out.removeAllViews();
                                Frame.removeAllViews();
                                TOP_TableLayout = temp_TableLayout;
                                editor.remove(0 + "title").apply();
                                String Filepath = getApplicationContext().getFilesDir().getParent() + "/" + "shared_prefs/";
                                File dir = new File(Filepath);
                                String[] children = dir.list();
                                String select_Table =0 + "Table.xml";
                                assert children != null;
                                for (String child : children) {
                                    if (child.equals(select_Table)) {
                                        File file_Table = new File(Filepath + select_Table);
                                        file_Table.delete();
                                    }
                                }
                            }  // 0번 테이블을 눌렀는데 다른테이블 없이  0번만 있을때
                            else {
                                System.arraycopy(TOP_TableLayout, 1, temp_TableLayout, 0, temp_TableLayout.length);

                                for (TableLayout tableLayout : temp_TableLayout) {
                                    if (tableLayout.getParent() != null) {
                                        ((ViewGroup) tableLayout.getParent()).removeView(tableLayout);
                                    }
                                }           // 자식이 연결된 부모뷰 존재할수 있기때문에 전부 초기화
                                TOP_TableLayout = temp_TableLayout;

                                Scollview_in.removeAllViews();
                                Linear_wrap.removeAllViews();
                                Scollview_out.removeAllViews();
                                Frame.removeAllViews();                                                 // 기존 프레임레이아웃 제거

                                Scollview_in.addView(temp_TableLayout[0]);             //테이블을 안의 scrollview에 배치
                                Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
                                Linear_wrap.addView(Linear_add_delete);             //리니어에 행추가버튼 배치
                                Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
                                Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
                                Frame.addView(Scollview_out);

                                editor.remove(0 + "title").apply();
                                for (int i=1; i<count ; i++){
                                    String title_name = pref.getString(i+"title", null);
                                    editor.putString(i-1+"title", title_name).apply();
                                }
                                editor.remove(count-1+"title");

                                for(int i=1; i<count; i ++){
                                    int tr_length = pref_Table.getInt(i + "Table_Total_Row", 5); //몇번테이블의 로우가 몇개 있는지 확인후 로드
                                    for (int tr =0 ; tr< tr_length;tr++) {
                                        for (int td= 0; td<2;td++){
                                            String table_contents = pref_Table.getString(i + "Table" + tr + "row" + td + "column", null);
                                            editor_Table.putString(i-1 + "Table" + tr + "row" + td + "column",table_contents).apply();
                                        }
                                    }
                                }

                                String Filepath = getApplicationContext().getFilesDir().getParent() + "/" + "shared_prefs/";
                                File dir = new File(Filepath);
                                String[] children = dir.list();
                                String select_Table = 0 + "Table.xml";
                                assert children != null;
                                for (String child : children) {
                                    if (child.equals(select_Table)) {
                                        File file_Table = new File(Filepath + select_Table);
                                        file_Table.delete();
                                    }
                                }
                                for (int i=1; i< count; i++){
                                    String file = i +"Table.xml";
                                    String rename = i-1 + "Table.xml";
                                    File rename_file = new File(Filepath + file);
                                    rename_file.renameTo(new File(Filepath + rename));
                                }
                            }                                                  // 0번 테이블 눌렀는데 0번 위로 있을때
                            count--;
                            editor.putInt("current_count", count).apply();

                        }        //0번 테이블을 눌렀을때
                        else if(button_tag != TOP_TableLayout[0].getTag())  {
                            if (button_tag != TOP_TableLayout[TOP_TableLayout.length-1].getTag()) {

                                int select_count = 0;
                                for (int i=0; i<=TOP_Table_length;i++){
                                    if (TOP_TableLayout[i].getTag() == button_tag){
                                        select_count = i;
                                    }
                                }

                                System.arraycopy(TOP_TableLayout, 0, temp_TableLayout, 0, select_count);
                                System.arraycopy(TOP_TableLayout, select_count + 1, temp_TableLayout, select_count, TOP_TableLayout.length - (select_count+ 1));

                                for (TableLayout tableLayout : temp_TableLayout) {
                                    if (tableLayout.getParent() != null) {
                                        ((ViewGroup) tableLayout.getParent()).removeView(tableLayout);
                                    }
                                } // 자식이 연결된 부모뷰 초기화 해주기

                                Scollview_in.removeAllViews();
                                Linear_wrap.removeAllViews();
                                Scollview_out.removeAllViews();
                                Frame.removeAllViews();
                                Scollview_in.addView(temp_TableLayout[select_count-1]);             //테이블을 안의 scrollview에 배치
                                Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
                                Linear_wrap.addView(Linear_add_delete);             //리니어에 행추가버튼 배치
                                Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
                                Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
                                Frame.addView(Scollview_out);

                                TOP_TableLayout = temp_TableLayout;

                                editor.remove(select_count + "title").apply();
                                for (int i=select_count+1; i<count ; i++){
                                    String title_name = pref.getString(i+"title", null);
                                    editor.putString(i-1+"title", title_name).apply();
                                }
                                editor.remove(count-1+"title").apply();
                                for(int i=select_count+1; i<count; i ++){
                                    int tr_length = pref_Table.getInt(i + "Table_Total_Row", 5); //몇번테이블의 로우가 몇개 있는지 확인후 로드
                                    for (int tr =0 ; tr< tr_length;tr++) {
                                        for (int td= 0; td<2;td++){
                                            String table_contents = pref_Table.getString(i + "Table" + tr + "row" + td + "column", null);
                                            editor_Table.putString(i-1 + "Table" + tr + "row" + td + "column",table_contents).apply();
                                        }
                                    }
                                }

                                String Filepath = getApplicationContext().getFilesDir().getParent() + "/" + "shared_prefs/";
                                File dir = new File(Filepath);
                                String[] children = dir.list();
                                String select_Table = select_count + "Table.xml";
                                assert children != null;
                                for (String child : children) {
                                    if (child.equals(select_Table)) {
                                        File file_Table = new File(Filepath + select_Table);
                                        file_Table.delete();
                                    }
                                }
                                for (int i =select_count+1 ; i<count ; i++){
                                    String file = i +"Table.xml";
                                    String rename = i-1 + "Table.xml";
                                    File rename_file = new File(Filepath + file);
                                    rename_file.renameTo(new File(Filepath + rename));
                                }

                            }       // 가운데 테이블 삭제할때
                            else { // 맨끝에 삭제하면
                                Scollview_in.removeAllViews();
                                Linear_wrap.removeAllViews();
                                Scollview_out.removeAllViews();
                                Frame.removeAllViews();                                                 // 기존 프레임레이아웃 제거
                                System.arraycopy(TOP_TableLayout, 0, temp_TableLayout, 0, temp_TableLayout.length);
                                for (TableLayout tableLayout : temp_TableLayout) {                   // 자식이 연결된 부모뷰 초기화 해주기
                                    if (tableLayout.getParent() != null) {
                                        ((ViewGroup) tableLayout.getParent()).removeView(tableLayout);
                                    }
                                }

                                Scollview_in.addView(temp_TableLayout[temp_TableLayout.length - 1]);             //테이블을 안의 scrollview에 배치
                                Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
                                Linear_wrap.addView(Linear_add_delete);             //리니어에 행추가버튼 배치
                                Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
                                Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
                                Frame.addView(Scollview_out);

                                TOP_TableLayout = temp_TableLayout;
                                editor.remove(temp_TableLayout.length + "title").apply();
                                String Filepath = getApplicationContext().getFilesDir().getParent() + "/" + "shared_prefs/";
                                File dir = new File(Filepath);
                                String[] children = dir.list();
                                String select_Table = temp_TableLayout.length + "Table.xml";
                                assert children != null;
                                for (String child : children) {
                                    if (child.equals(select_Table)) {
                                        File file_Table = new File(Filepath + select_Table);
                                        file_Table.delete();
                                    }
                                }

                            }                        // 맨끝에 삭제하면
                            count--;
                            editor.putInt("current_count", count).apply();
                        }                                    // title_button.getId가 0번의 id랑 다를때


                    }
                });
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
                return true;
            }
        });
    }           // 다이얼로그로 추가하고 저장 삭제

    @SuppressLint("ClickableViewAccessibility")
    public void restore_btn() {
        final SharedPreferences.Editor editor = pref.edit();

        final FrameLayout Frame = findViewById(R.id.frame);
        final ScrollView Scollview_in = new ScrollView(MainActivity.this);
        final ScrollView Scollview_out = new ScrollView(MainActivity.this);
        final LinearLayout Linear_wrap = new LinearLayout(MainActivity.this);
        final Handler Handler = new Handler();          //scroll 딜레이를 위한 핸들러
        final TableLayout[] temp_TableLayout = new TableLayout[count];
        final LinearLayout[] Linear_add_delete = new LinearLayout[count];
        final ScrollView.LayoutParams scrollview_param = new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final ScrollView.LayoutParams scroll_in_param = new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        final LinearLayout.LayoutParams Linear_wrap_param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);



        for (int i = 0; i < count; i++) {
            temp_TableLayout[i] = new TableLayout(MainActivity.this);
            Linear_add_delete[i] = new LinearLayout(MainActivity.this);
        }// 초기화 시킴

        for (int i = 0; i < count; i++) {
            temp_TableLayout[i].setTag(i);
            temp_TableLayout[i].setLayoutParams(scroll_in_param);
            Linear_add_delete[i].setLayoutParams(Linear_wrap_param);
            Scollview_in.setLayoutParams(Linear_wrap_param);
            Scollview_out.setLayoutParams(scrollview_param);
            final SharedPreferences pref_Table = getSharedPreferences(i + "Table", MODE_PRIVATE);
            final SharedPreferences.Editor editor_Table = pref_Table.edit();

            int tr_length = pref_Table.getInt(i + "Table_Total_Row", 5);
            final TableRow[] row = new TableRow[tr_length];  // 테이블로우 5개 기본 생성
            final TableRow.LayoutParams td1param = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            final TableRow.LayoutParams td2param = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            final TableRow.LayoutParams td3param = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);

            td1param.weight = 1.6f;                 // 테이블로우에 들어갈 1번째 td 뷰 param weight 설정
            td2param.weight = 7.5f;                 // 테이블로우에 들어갈 2번째 td 뷰 param weight 설정
            td3param.weight = 0.9f;
            td3param.setMargins(0,10,0,0);
            final int finalI = i;
            for (int tr = 0; tr < tr_length; tr++) {         //로우가 0부터 4까지 5개 tr_length=5 보다 작게(처음에 tr이 5개다 설정)
                if (tr == 0) {
                    row[tr] = new TableRow(MainActivity.this);
                    final EditText tr0_column0 = new EditText(MainActivity.this);
                    final EditText tr0_column1 = new EditText(MainActivity.this);
                    ImageButton tr0_column2 = new ImageButton(MainActivity.this);
                    final int finalTr = tr;
                    for (int td = 0; td < 3; td++) {
                        if (td == 0) {
                            final int finalTd = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr + "row" + finalTd + "column", "공사명");
                            tr0_column0.setText(text);
                            tr0_column0.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr0_column0 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr + "row" + finalTd + "column", txt_tr0_column0).apply();
                                }
                            });
                            tr0_column0.setTextColor(Color.BLACK);
                            tr0_column0.setTextSize(12);
                            tr0_column0.setPadding(0, 20, 0, 50);
                            tr0_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            tr0_column0.setLayoutParams(td1param);
                            row[tr].addView(tr0_column0);
                        } else if (td == 1) {
                            final int finalTd = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr + "row" + finalTd + "column", null);
                            tr0_column1.setText(text);
                            tr0_column1.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr0_column1 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr + "row" + finalTd + "column", txt_tr0_column1).apply();
                                }
                            });
                            tr0_column1.setTextColor(Color.BLACK);
                            tr0_column1.setTextSize(12);
                            tr0_column1.setPadding(20, 20, 0, 50);
                            tr0_column1.setLayoutParams(td2param);
                            row[tr].addView(tr0_column1);
                        } else {
                            tr0_column2.setPadding(0, 0, 0, 0);
                            tr0_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                            tr0_column2.setLayoutParams(td3param);
                            row[tr].addView(tr0_column2);
                            tr0_column2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tr0_column0.setText(null);
                                    tr0_column1.setText(null);
                                    editor_Table.remove(finalI + "Table" + finalTr + "row" + 0 + "column").apply();
                                    editor_Table.remove(finalI + "Table" + finalTr + "row" + 1 + "column").apply();
                                }
                            });
                        }
                    }
                    temp_TableLayout[i].addView(row[tr]);
                }             // 0번째 tr   공사명
                else if (tr == 1) {
                    row[tr] = new TableRow(MainActivity.this);
                    final EditText tr1_column0 = new EditText(MainActivity.this);
                    final EditText tr1_column1 = new EditText(MainActivity.this);
                    ImageButton tr1_column2 = new ImageButton(MainActivity.this);
                    final int finalTr1 = tr;
                    for (int td = 0; td < 3; td++) {
                        if (td == 0) {
                            final int finalTd1 = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr1 + "row" + finalTd1 + "column", "공 종");
                            tr1_column0.setText(text);
                            tr1_column0.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr1_column0 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr1 + "row" + finalTd1 + "column", txt_tr1_column0).apply();
                                }
                            });
                            tr1_column0.setTextColor(Color.BLACK);
                            tr1_column0.setTextSize(12);
                            tr1_column0.setPadding(0, 20, 0, 50);
                            tr1_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            tr1_column0.setLayoutParams(td1param);
                            row[tr].addView(tr1_column0);
                        } else if (td == 1) {
                            final int finalTd1 = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr1 + "row" + finalTd1 + "column", null);
                            tr1_column1.setText(text);
                            tr1_column1.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr1_column1 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr1 + "row" + finalTd1 + "column", txt_tr1_column1).apply();
                                }
                            });
                            tr1_column1.setTextColor(Color.BLACK);
                            tr1_column1.setTextSize(12);
                            tr1_column1.setPadding(20, 20, 0, 50);
                            tr1_column1.setLayoutParams(td2param);
                            row[tr].addView(tr1_column1);
                        } else {
                            tr1_column2.setPadding(0, 0, 0, 0);
                            tr1_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                            tr1_column2.setLayoutParams(td3param);
                            row[tr].addView(tr1_column2);

                            tr1_column2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tr1_column0.setText(null);
                                    tr1_column1.setText(null);
                                    editor_Table.remove(finalI + "Table" + finalTr1 + "row" + 0 + "column").apply();
                                    editor_Table.remove(finalI + "Table" + finalTr1 + "row" + 1 + "column").apply();
                                }
                            });
                        }
                    }
                    temp_TableLayout[i].addView(row[tr]);
                }           // 1번째 tr 공종
                else if (tr == 2) {
                    row[tr] = new TableRow(MainActivity.this);
                    final EditText tr2_column0 = new EditText(MainActivity.this);
                    final EditText tr2_column1 = new EditText(MainActivity.this);
                    ImageButton tr2_column2 = new ImageButton(MainActivity.this);
                    final int finalTr2 = tr;
                    for (int td = 0; td < 3; td++) {
                        if (td == 0) {
                            final int finalTd2 = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr2 + "row" + finalTd2 + "column", "위 치");
                            tr2_column0.setText(text);
                            tr2_column0.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr2_column1 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr2 + "row" + finalTd2 + "column", txt_tr2_column1).apply();
                                }
                            });
                            tr2_column0.setTextColor(Color.BLACK);
                            tr2_column0.setTextSize(12);
                            tr2_column0.setPadding(0, 20, 0, 50);
                            tr2_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            tr2_column0.setLayoutParams(td1param);
                            row[tr].addView(tr2_column0);
                        } else if (td == 1) {
                            final int finalTd2 = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr2 + "row" + finalTd2 + "column", null);
                            tr2_column1.setText(text);
                            tr2_column1.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr0_column1 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr2 + "row" + finalTd2 + "column", txt_tr0_column1).apply();
                                }
                            });
                            tr2_column1.setTextColor(Color.BLACK);
                            tr2_column1.setTextSize(12);
                            tr2_column1.setPadding(20, 20, 0, 50);
                            tr2_column1.setLayoutParams(td2param);
                            row[tr].addView(tr2_column1);
                        } else {
                            tr2_column2.setPadding(0, 0, 0, 0);
                            tr2_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                            tr2_column2.setLayoutParams(td3param);
                            row[tr].addView(tr2_column2);
                            tr2_column2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tr2_column0.setText(null);
                                    tr2_column1.setText(null);
                                    editor_Table.remove(finalI + "Table" + finalTr2 + "row" + 0 + "column").apply();
                                    editor_Table.remove(finalI + "Table" + finalTr2 + "row" + 1 + "column").apply();
                                }
                            });
                        }
                    }
                    temp_TableLayout[i].addView(row[tr]);
                }           // 2번째 tr 일정
                else if (tr == 3) {
                    row[tr] = new TableRow(MainActivity.this);
                    final EditText tr3_column0 = new EditText(MainActivity.this);
                    final EditText tr3_column1 = new EditText(MainActivity.this);
                    ImageButton tr3_column2 = new ImageButton(MainActivity.this);
                    final int finalTr3 = tr;
                    for (int td = 0; td < 3; td++) {
                        if (td == 0) {
                            final int finalTd3 = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr3 + "row" + finalTd3 + "column", "위 치");
                            tr3_column0.setText(text);
                            tr3_column0.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr3_column0 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr3 + "row" + finalTd3 + "column", txt_tr3_column0).apply();
                                }
                            });
                            tr3_column0.setTextColor(Color.BLACK);
                            tr3_column0.setTextSize(12);
                            tr3_column0.setPadding(0, 20, 0, 50);
                            tr3_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            tr3_column0.setLayoutParams(td1param);
                            row[tr].addView(tr3_column0);
                        } else if (td == 1) {
                            final int finalTd3 = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr3 + "row" + finalTd3 + "column", null);
                            tr3_column1.setText(text);
                            tr3_column1.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr3_column1 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr3 + "row" + finalTd3 + "column", txt_tr3_column1).apply();
                                }
                            });
                            tr3_column1.setTextColor(Color.BLACK);
                            tr3_column1.setTextSize(12);
                            tr3_column1.setPadding(20, 20, 0, 50);
                            tr3_column1.setLayoutParams(td2param);
                            row[tr].addView(tr3_column1);
                        } else {
                            tr3_column2.setPadding(0, 0, 0, 0);
                            tr3_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                            tr3_column2.setLayoutParams(td3param);
                            row[tr].addView(tr3_column2);
                            tr3_column2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tr3_column0.setText(null);
                                    tr3_column1.setText(null);
                                    editor_Table.remove(finalI + "Table" + finalTr3 + "row" + 0 + "column").apply();
                                    editor_Table.remove(finalI + "Table" + finalTr3 + "row" + 1 + "column").apply();
                                }
                            });
                        }
                    }
                    temp_TableLayout[i].addView(row[tr]);
                }           // 3번째 tr 위치
                else if (tr == 4) {
                    row[tr] = new TableRow(MainActivity.this);
                    final EditText tr4_column0 = new EditText(MainActivity.this);
                    final EditText tr4_column1 = new EditText(MainActivity.this);
                    ImageButton tr4_column2 = new ImageButton(MainActivity.this);
                    final int finalTr4 = tr;
                    for (int td = 0; td < 3; td++) {
                        if (td == 0) {
                            final int finalTd4 = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr4 + "row" + finalTd4 + "column", "내 용");
                            tr4_column0.setText(text);
                            tr4_column0.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr4_column0 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr4 + "row" + finalTd4 + "column", txt_tr4_column0).apply();
                                }
                            });
                            tr4_column0.setTextColor(Color.BLACK);
                            tr4_column0.setTextSize(12);
                            tr4_column0.setPadding(0, 20, 0, 50);
                            tr4_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            tr4_column0.setLayoutParams(td1param);
                            row[tr].addView(tr4_column0);
                        } else if (td == 1) {
                            final int finalTd4 = td;
                            String text = pref_Table.getString(finalI + "Table" + finalTr4 + "row" + finalTd4 + "column", null);
                            tr4_column1.setText(text);
                            tr4_column1.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr4_column1 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTr4 + "row" + finalTd4 + "column", txt_tr4_column1).apply();
                                }
                            });
                            tr4_column1.setTextColor(Color.BLACK);
                            tr4_column1.setTextSize(12);
                            tr4_column1.setPadding(20, 20, 0, 50);
                            tr4_column1.setLayoutParams(td2param);
                            row[tr].addView(tr4_column1);
                        } else {
                            tr4_column2.setPadding(0, 0, 0, 0);
                            tr4_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove));
                            tr4_column2.setLayoutParams(td3param);
                            row[tr].addView(tr4_column2);
                            tr4_column2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    tr4_column0.setText(null);
                                    tr4_column1.setText(null);
                                    editor_Table.remove(finalI + "Table" + finalTr4 + "row" + 0 + "column").apply();
                                    editor_Table.remove(finalI + "Table" + finalTr4 + "row" + 1 + "column").apply();
                                }
                            });
                        }
                    }
                    temp_TableLayout[i].addView(row[tr]);
                }
                else {
                    row[tr] = new TableRow(MainActivity.this);
                    final EditText tr_column0 = new EditText(MainActivity.this);
                    final EditText tr_column1 = new EditText(MainActivity.this);
                    ImageButton tr_column2 = new ImageButton(MainActivity.this);
                    final int finalTR = tr;
                    for (int td = 0; td < 3; td++) {
                        if (td == 0) {
                            final int finalTd0 = td;
                            String text = pref_Table.getString(finalI + "Table" + tr + "row" + td + "column", "일 자");
                            tr_column0.setText(text);
                            tr_column0.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr_column0 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTR + "row" + finalTd0 + "column", txt_tr_column0).apply();
                                }
                            });
                            tr_column0.setTextColor(Color.BLACK);
                            tr_column0.setTextSize(12);
                            tr_column0.setPadding(0, 20, 0, 50);
                            tr_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            tr_column0.setLayoutParams(td1param);
                            row[tr].addView(tr_column0);

                        } else if (td == 1) {
                            final int finalTd1 = td;
                            String text = pref_Table.getString(finalI + "Table" + tr + "row" + td + "column", null);
                            tr_column1.setText(text);
                            tr_column1.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr_column1 = s.toString();
                                    editor_Table.putString(finalI + "Table" + finalTR + "row" + finalTd1 + "column", txt_tr_column1).apply();
                                }
                            });
                            tr_column1.setTextColor(Color.BLACK);
                            tr_column1.setTextSize(12);
                            tr_column1.setPadding(20, 20, 0, 50);
                            tr_column1.setLayoutParams(td2param);
                            row[tr].addView(tr_column1);
                        } else {
                            tr_column2.setPadding(0, 0, 0, 0);
                            tr_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove)); //색깔이랑 length조절하셈
                            tr_column2.setLayoutParams(td3param);
                            row[tr].addView(tr_column2);
                            tr_column2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) { // 추가한 행을 지우는 delete버튼 클릭시
                                    int tr_length = pref_Table.getInt(finalI + "Table_Total_Row", 5);
                                    int new_tr_length = tr_length - 1;
                                    temp_TableLayout[finalI].removeView(row[finalTR]); // 테이블에 추가한 tr_row view 제거
                                    editor_Table.putInt(finalI + "Table_Total_Row", tr_length - 1).apply();
                                    //ex. 5TableTotalRow 하나 줄여서 저장
                                    editor_Table.remove(finalI + "Table" + new_tr_length + "row" + 0 + "column").apply();
                                    //ex. 5Table6row0column 값 삭제
                                    editor_Table.remove(finalI + "Table" + new_tr_length + "row" + 1 + "column").apply();
                                    //ex. 5Table6row1column 값 삭제
                                }
                            });

                        }
                    }
                    temp_TableLayout[i].addView(row[tr]);
                }
            }

            LinearLayout.LayoutParams Linear_param1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams Linear_param2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            Linear_param1.weight = 1.6f;
            Linear_param2.weight = 8.4f;

            ImageView add_tr_button = new ImageView(MainActivity.this);
            add_tr_button.setPadding(0, 0, 0, 0);
            add_tr_button.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_plus));
            add_tr_button.setLayoutParams(Linear_param1);

            View view = new View(MainActivity.this);
            view.setLayoutParams(Linear_param2);
            Linear_add_delete[i].setOrientation(LinearLayout.HORIZONTAL);
            Linear_add_delete[i].addView(add_tr_button);
            Linear_add_delete[i].addView(view);

            add_tr_button.setClickable(true);
            add_tr_button.setOnClickListener(new View.OnClickListener() {   //행 추가 버튼 클릭했을때
                @Override
                public void onClick(View v) {
                    final int tr_length = pref_Table.getInt(finalI + "Table_Total_Row", 5); //0번테이블의 로우가 몇개 있는지 확인후 로드
                    int new_tr_length = tr_length + 1;   // tr은 6 -> tr_count=5 +1
                    editor_Table.putInt(finalI + "Table_Total_Row", new_tr_length).apply();  //0TableTotalRow => 확인을 위한 tr_count = 6으로 저장
                    final TableRow tr_row = new TableRow(MainActivity.this);
                    final EditText tr_column0 = new EditText(MainActivity.this);
                    final EditText tr_column1 = new EditText(MainActivity.this);
                    ImageButton tr_column2 = new ImageButton(MainActivity.this);

                    for (int td = 0; td < 3; td++) {
                        if (td == 0) {
                            tr_column0.setText(null);
                            editor_Table.putString(finalI + "Table" + tr_length + "row" + td + "column", null).apply();
                            tr_column0.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr_column0 = s.toString();
                                    editor_Table.putString(table_count + "Table" + tr_length + "row" + 0 + "column", txt_tr_column0).apply();
                                }
                            });
                            tr_column0.setTextColor(Color.BLACK);
                            tr_column0.setTextSize(12);
                            tr_column0.setPadding(0, 20, 0, 50);
                            tr_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            tr_column0.setLayoutParams(td1param);
                            tr_row.addView(tr_column0);
                            // 0Table6row0column은  tr_column2의 String 값 받아서 저장
                        } else if (td == 1) {
                            tr_column1.setText(null);
                            editor_Table.putString(finalI + "Table" + tr_length + "row" + td + "column", null).apply();
                            tr_column1.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    String txt_tr_column1 = s.toString();
                                    editor_Table.putString(finalI + "Table" + tr_length + "row" + 1 + "column", txt_tr_column1).apply();
                                }
                            });
                            tr_column1.setTextColor(Color.BLACK);
                            tr_column1.setTextSize(12);
                            tr_column1.setPadding(20, 20, 0, 50);
                            tr_column1.setLayoutParams(td2param);
                            tr_row.addView(tr_column1);
                            //4Table6row1column은 tr_column1의 String 값 받아서 저장
                        } else {
                            tr_column2.setPadding(0, 0, 0, 0);
                            tr_column2.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stat_remove)); //색깔이랑 length조절하셈
                            tr_column2.setLayoutParams(td3param);
                            tr_row.addView(tr_column2);

                            tr_column2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) { // 추가한 행을 지우는 delete버튼 클릭시
                                    int tr_length = pref_Table.getInt(finalI + "Table_Total_Row", 5);
                                    int new_tr_length = tr_length - 1;
                                    temp_TableLayout[finalI].removeView(tr_row); // 테이블에 추가한 tr_row view 제거
                                    editor_Table.putInt(finalI + "Table_Total_Row", new_tr_length).apply();
                                    //ex. 4TableTotalRow 하나 줄여서 저장
                                    editor_Table.remove(finalI + "Table" + new_tr_length + "row" + 0 + "column").apply();
                                    //ex. 4Table6row0column 값 삭제
                                    editor_Table.remove(finalI + "Table" + new_tr_length + "row" + 1 + "column").apply();
                                    //ex. 4Table6row1column 값 삭제
                                }
                            });
                        }
                    }
                    Handler.postDelayed(new Runnable() {
                        public void run() {
                            Scollview_out.fullScroll(HorizontalScrollView.FOCUS_DOWN);       // scollview delay
                        }
                    }, 100);

                    temp_TableLayout[finalI].addView(tr_row);
                }
            });


        }

        TOP_TableLayout = temp_TableLayout;             //temp로 하나 추가한 table배열을 원 배열에 옮긴다.
        Scollview_in.addView(temp_TableLayout[0]);             //테이블을 안의 scrollview에 배치
        Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
        Linear_wrap.addView(Linear_add_delete[0]);             //리니어에 행추가버튼 배치
        Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
        Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
        Frame.addView(Scollview_out);               //프레임레이아웃에 전체스크롤뷰저장

        for (i = 0; i < count; i++) {
            if (containCheck(i + "title")) {
                final Button title_button = new Button(MainActivity.this);
                title_button.setTag(i);
                title_button.setSelected(false);
                title_button.setBackgroundResource(R.drawable.click);
                String text = pref.getString(i + "title", "서식 new");
                LinearLayout.LayoutParams title_param = new LinearLayout.LayoutParams(210, ViewGroup.LayoutParams.MATCH_PARENT);
                title_param.leftMargin = 5;
                title_param.rightMargin = 5;
                title_param.bottomMargin = 10;
                title_button.setLayoutParams(title_param);
                title_button.setPadding(0, 0, 0, 0);
                title_button.setTextSize(12);
                title_button.setText(text);
                title_button.setTextColor(Color.WHITE);
                layout_form_btn.addView(title_button);



                title_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Object title_button_tag = title_button.getTag();
                        if(current_titleButton_Tag == null){
                            current_titleButton_Tag = title_button_tag;
                            title_button.setSelected(true);
                        }
                        else {
                            if (current_titleButton_Tag == title_button_tag) {
                                current_titleButton_Tag = title_button_tag;
                                title_button.setSelected(true);
                            }
                            else{
                                Button btn = (Button) layout_form_btn.findViewWithTag(current_titleButton_Tag);
                                btn.setSelected(false);

                                title_button.setSelected(true);
                                current_titleButton_Tag = title_button_tag;
                                btn.setBackgroundResource(R.drawable.click);
                            }
                        }

                        int select_count = 0;
                        for (int i=0;i<TOP_TableLayout.length;i++){
                            if (title_button.getTag() == TOP_TableLayout[i].getTag()  ) {
                                select_count = i;
                            }
                        }
                        intent_camera.putExtra("select_count", select_count);
                        Scollview_in.removeAllViews();
                        Linear_wrap.removeAllViews();
                        Scollview_out.removeAllViews();
                        Frame.removeAllViews();

                        for (TableLayout tableLayout : temp_TableLayout) {
                            if (tableLayout.getParent() != null) {
                                ((ViewGroup) tableLayout.getParent()).removeView(tableLayout);
                            }
                        } // 자식이 연결된 부모뷰 초기화 해주기
                        for (LinearLayout linearLayout : Linear_add_delete) {
                            if (linearLayout.getParent() != null) {
                                ((ViewGroup) linearLayout.getParent()).removeView(linearLayout);
                            }
                        }// 자식이 연결된 부모뷰 초기화 해주기

                        Scollview_in.addView(temp_TableLayout[select_count]);             //테이블을 안의 scrollview에 배치
                        Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
                        Linear_wrap.addView(Linear_add_delete[select_count]);             //리니어에 행추가버튼 배치
                        Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
                        Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
                        Frame.addView(Scollview_out);

                    }
                });

                title_button.setLongClickable(true);
                title_button.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("삭제하시겠습니까?");
                        builder.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                layout_form_btn.removeView(title_button);
                                TableLayout[] temp_TableLayout = new TableLayout[TOP_TableLayout.length - 1];

                                if (title_button.getTag() == TOP_TableLayout[0].getTag())  {
                                    if (temp_TableLayout.length == 0) {
                                        Scollview_in.removeAllViews();
                                        Linear_wrap.removeAllViews();
                                        Scollview_out.removeAllViews();
                                        Frame.removeAllViews();
                                        TOP_TableLayout = temp_TableLayout;
                                        editor.remove(0 + "title").apply();
                                        String Filepath = getApplicationContext().getFilesDir().getParent() + "/" + "shared_prefs/";
                                        File dir = new File(Filepath);
                                        String[] children = dir.list();
                                        String select_Table =0 + "Table.xml";
                                        assert children != null;
                                        for (String child : children) {
                                            if (child.equals(select_Table)) {
                                                File file_Table = new File(Filepath + select_Table);
                                                file_Table.delete();
                                            }
                                        }
                                    }  // 0번 테이블을 눌렀는데 다른테이블 없이  0번만 있을때
                                    else {
                                        System.arraycopy(TOP_TableLayout, 1, temp_TableLayout, 0, temp_TableLayout.length);

                                        for (TableLayout tableLayout : temp_TableLayout) {
                                            if (tableLayout.getParent() != null) {
                                                ((ViewGroup) tableLayout.getParent()).removeView(tableLayout);
                                            }
                                        }           // 자식이 연결된 부모뷰 존재할수 있기때문에 전부 초기화
                                        for (LinearLayout linearLayout : Linear_add_delete) {
                                            if (linearLayout.getParent() != null) {
                                                ((ViewGroup) linearLayout.getParent()).removeView(linearLayout);
                                            }
                                        }                               // 자식이 연결된 부모뷰 초기화 해주기
                                        TOP_TableLayout = temp_TableLayout;

                                        Scollview_in.removeAllViews();
                                        Linear_wrap.removeAllViews();
                                        Scollview_out.removeAllViews();
                                        Frame.removeAllViews();                                                 // 기존 프레임레이아웃 제거

                                        Scollview_in.addView(temp_TableLayout[0]);             //테이블을 안의 scrollview에 배치
                                        Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
                                        Linear_wrap.addView(Linear_add_delete[0]);              //리니어에 행추가버튼 배치
                                        Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
                                        Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
                                        Frame.addView(Scollview_out);

                                        editor.remove(0 + "title").apply();
                                        for (int i=1; i<count ; i++){
                                            String title_name = pref.getString(i+"title", null);
                                            editor.putString(i-1+"title", title_name).apply();
                                        }
                                        editor.remove(count-1+"title").apply();
                                        for(int i=1; i<count; i ++){
                                            SharedPreferences pref_Table = getSharedPreferences(i + "Table", MODE_PRIVATE);
                                            SharedPreferences.Editor editor_Table = pref_Table.edit();
                                            int tr_length = pref_Table.getInt(i + "Table_Total_Row", 5); //몇번테이블의 로우가 몇개 있는지 확인후 로드
                                            for (int tr =0 ; tr< tr_length;tr++) {
                                                for (int td= 0; td<2;td++){
                                                    String table_contents = pref_Table.getString(i + "Table" + tr + "row" + td + "column", null);
                                                    editor_Table.putString(i-1 + "Table" + tr + "row" + td + "column",table_contents).apply();
                                                }
                                            }
                                        }

                                        String Filepath = getApplicationContext().getFilesDir().getParent() + "/" + "shared_prefs/";
                                        File dir = new File(Filepath);
                                        String[] children = dir.list();
                                        String select_Table = 0 + "Table.xml";
                                        assert children != null;
                                        for (String child : children) {
                                            if (child.equals(select_Table)) {
                                                File file_Table = new File(Filepath + select_Table);
                                                file_Table.delete();
                                            }
                                        }
                                        for (int i=1; i< count; i++){
                                            String file = i +"Table.xml";
                                            String rename = i-1 + "Table.xml";
                                            File rename_file = new File(Filepath + file);
                                            rename_file.renameTo(new File(Filepath + rename));
                                        }
                                    }                                                  // 0번 테이블 눌렀는데 0번 위로 있을때
                                    count--;
                                    editor.putInt("current_count", count).apply();

                                }        //0번 테이블을 눌렀을때
                                else if(title_button.getTag() != TOP_TableLayout[0].getTag())  {
                                    if (title_button.getTag() != TOP_TableLayout[TOP_TableLayout.length-1].getTag()) {
                                        int select_count = 0;
                                        for (int i=0; i<TOP_TableLayout.length;i++){
                                            if (TOP_TableLayout[i].getTag() == title_button.getTag()){
                                                select_count = i;
                                            }
                                        }
                                        System.arraycopy(TOP_TableLayout, 0, temp_TableLayout, 0, select_count);
                                        System.arraycopy(TOP_TableLayout, select_count + 1, temp_TableLayout, select_count, TOP_TableLayout.length - (select_count+ 1));

                                        for (TableLayout tableLayout : temp_TableLayout) {
                                            if (tableLayout.getParent() != null) {
                                                ((ViewGroup) tableLayout.getParent()).removeView(tableLayout);
                                            }
                                        } // 자식이 연결된 부모뷰 초기화 해주기
                                        for (LinearLayout linearLayout : Linear_add_delete) {
                                            if (linearLayout.getParent() != null) {
                                                ((ViewGroup) linearLayout.getParent()).removeView(linearLayout);
                                            }
                                        }                                                       // 자식이 연결된 부모뷰 초기화 해주기

                                        Scollview_in.removeAllViews();
                                        Linear_wrap.removeAllViews();
                                        Scollview_out.removeAllViews();
                                        Frame.removeAllViews();
                                        Scollview_in.addView(temp_TableLayout[select_count-1]);             //테이블을 안의 scrollview에 배치
                                        Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
                                        Linear_wrap.addView(Linear_add_delete[select_count - 1]);             //리니어에 행추가버튼 배치
                                        Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
                                        Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
                                        Frame.addView(Scollview_out);

                                        TOP_TableLayout = temp_TableLayout;

                                        editor.remove(select_count + "title").apply();
                                        for (int i=select_count+1; i<count ; i++){
                                            String title_name = pref.getString(i+"title", null);
                                            editor.putString(i-1+"title", title_name).apply();
                                        }
                                        editor.remove(count-1+"title").apply();

                                        for(int i=select_count+1; i<count; i ++){
                                            SharedPreferences pref_Table = getSharedPreferences(i + "Table", MODE_PRIVATE);
                                            SharedPreferences.Editor editor_Table = pref_Table.edit();
                                            int tr_length = pref_Table.getInt(i + "Table_Total_Row", 5); //몇번테이블의 로우가 몇개 있는지 확인후 로드
                                            for (int tr =0 ; tr< tr_length;tr++) {
                                                for (int td= 0; td<2;td++){
                                                    String table_contents = pref_Table.getString(i + "Table" + tr + "row" + td + "column", null);
                                                    editor_Table.putString(i-1 + "Table" + tr + "row" + td + "column",table_contents).apply();
                                                }
                                            }
                                        }

                                        String Filepath = getApplicationContext().getFilesDir().getParent() + "/" + "shared_prefs/";
                                        File dir = new File(Filepath);
                                        String[] children = dir.list();
                                        String select_Table = select_count + "Table.xml";
                                        assert children != null;
                                        for (String child : children) {
                                            if (child.equals(select_Table)) {
                                                File file_Table = new File(Filepath + select_Table);
                                                file_Table.delete();
                                            }
                                        }
                                        for (int i =select_count+1 ; i<count ; i++){
                                            String file = i +"Table.xml";
                                            String rename = i-1 + "Table.xml";
                                            File rename_file = new File(Filepath + file);
                                            rename_file.renameTo(new File(Filepath + rename));
                                        }

                                    }       // 가운데 테이블 삭제할때
                                    else { // 맨끝에 삭제하면
                                        Scollview_in.removeAllViews();
                                        Linear_wrap.removeAllViews();
                                        Scollview_out.removeAllViews();
                                        Frame.removeAllViews();                                                 // 기존 프레임레이아웃 제거
                                        System.arraycopy(TOP_TableLayout, 0, temp_TableLayout, 0, temp_TableLayout.length);
                                        for (TableLayout tableLayout : temp_TableLayout) {                   // 자식이 연결된 부모뷰 초기화 해주기
                                            if (tableLayout.getParent() != null) {
                                                ((ViewGroup) tableLayout.getParent()).removeView(tableLayout);
                                            }
                                        }
                                        Scollview_in.addView(temp_TableLayout[temp_TableLayout.length - 1]);             //테이블을 안의 scrollview에 배치
                                        Linear_wrap.addView(Scollview_in);                  // 리니어에 안 scrollview 배치
                                        Linear_wrap.addView(Linear_add_delete[Linear_add_delete.length - 1]);             //리니어에 행추가버튼 배치
                                        Linear_wrap.setOrientation(LinearLayout.VERTICAL);      // 두개 들어있는 리니어 세로정렬
                                        Scollview_out.addView(Linear_wrap);                 //전체 스크롤뷰에 Wrap리니어 배치
                                        Frame.addView(Scollview_out);

                                        TOP_TableLayout = temp_TableLayout;

                                        editor.remove(temp_TableLayout.length + "title").apply();

                                        String Filepath = getApplicationContext().getFilesDir().getParent() + "/" + "shared_prefs/";
                                        File dir = new File(Filepath);
                                        String[] children = dir.list();
                                        String select_Table = temp_TableLayout.length + "Table.xml";
                                        assert children != null;
                                        for (String child : children) {
                                            if (child.equals(select_Table)) {
                                                File file_Table = new File(Filepath + select_Table);
                                                file_Table.delete();
                                            }
                                        }

                                    }                        // 맨끝에 삭제하면
                                    count--;
                                    editor.putInt("current_count", count).apply();
                                }                                    // title_button.getId가 0번의 id랑 다를때

                            }
                        });
                        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        builder.show();
                        return true;
                    }
                });
            }
        }
    }                   //다시 불러오기

    public boolean containCheck(String key) {
        return pref.contains(key);
    }       // check용 값 있는지 없는지

    public void InitializeTableData(){
        tableDataList = new ArrayList<>();

        int picture_count = pref_history.getInt("picture_count",0);

        for (int i=0 ; i<picture_count+1; i++){
            if (i==0){
                String text = "보드판 내용이 순차적으로 입력됩니다";
                String null_time = null;
                tableDataList.add(new Listitem(text,null_time));
            }
            else{
                String picture_time = pref_history.getString("picture_time"+ (i),null);
                String table_info = pref_history.getString("picture_info"+(i),null);
                tableDataList.add(new Listitem(table_info,picture_time));
            }
        }
    } //테이블 데이터 초기화

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            byte[] byteArray =data.getByteArrayExtra("image");
            if (  byteArray!=null){
                Bitmap image = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
                if (iv_result !=null){
                    iv_result.setImageBitmap(null);
                    iv_result.setImageBitmap(image);
                }
                else{
                    iv_result.setImageBitmap(image);
                }

            }

        } else {
            Toast.makeText(MainActivity.this, "REQUEST_ACT가 아님", Toast.LENGTH_SHORT).show();
        }
    }       //카메라 intent_camera  불러오기 바이트정보 들어있음

}


