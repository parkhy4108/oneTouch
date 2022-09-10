package com.dev_musashi.onetouch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AndroidCamera extends Activity implements SurfaceHolder.Callback{
    private static byte[] bytes;
    private static final String TAG = "TAG" ;
    private static final int REQUEST_CODE_STORAGE_PERMS = 100;
    public SharedPreferences pref_history;
    public static Camera camera;
    CameraManager cameraManager;
    public static boolean hasFlash;
    public int isLighOn =0;
    public boolean isFlashOn = false;
    float mDist = 0;
    int picture_count;
    Button flash;
    public int quality_count = 0;
    public int move_count = 0;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    FrameLayout picture_frame;
    public TableLayout prev_table;
    static int width;
    static int height;
    FrameLayout first_frame;
    ImageView iv;
    Intent intent_camera;
    LinearLayout linear,button_layout;
    MediaActionSound sound = new MediaActionSound();
    ListView listview;
    ArrayList<Listitem> tableDataList;
    MyAdapter myAdapter;
    String[] picture_bytearray;
    boolean previewing = false;;
    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;
    byte[] currentData_test;
    private MediaScanner ms = MediaScanner.newInstance(AndroidCamera.this);
    int count=0;
    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (savedInstanceState !=null){
            bytes = savedInstanceState.getByteArray("bytes");
            Log.d(String.valueOf(bytes), "Byte");
            intent_camera = new Intent();
            intent_camera.putExtra("image", bytes);
        }           // 화면 회전시 초기화 방지 instance로 저장해서 회전해도 값 가지고 있게

        getWindow().setFormat(PixelFormat.UNKNOWN);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);


        myAdapter = new MyAdapter(this, tableDataList);

        pref_history = getSharedPreferences("history", MODE_PRIVATE);
        final SharedPreferences.Editor editor_history = pref_history.edit();
        picture_count = pref_history.getInt("picture_count", 0);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        prev_table = new TableLayout(AndroidCamera.this);
        surfaceView = new SurfaceView(AndroidCamera.this);
        linear = new LinearLayout(AndroidCamera.this);
        picture_frame = new FrameLayout(AndroidCamera.this);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        flash = findViewById(R.id.flash);
        first_frame = findViewById(R.id.first_frame);
        button_layout = findViewById(R.id.button_layout);

        first_frame.addView(surfaceView);

        if (!hasPermissions()) {requestNecessaryPermissions(); }
        Intent intent_get_camera = getIntent();
        final int select_count = intent_get_camera.getIntExtra("select_count",0);
        Log.d(String.valueOf(select_count), "넘겨받은 서식번호");
//        final int select_count = 0;
        final SharedPreferences pref_Table = getSharedPreferences(select_count + "Table", MODE_PRIVATE);
        final int tr_length = pref_Table.getInt(select_count + "Table_Total_Row", 5);

        picture_frame.removeAllViews();


        TableRow[] row = new TableRow[tr_length];
        for (int tr = 0; tr < tr_length; tr++) {
            row[tr] = new TableRow(AndroidCamera.this);
            TextView tr_column0 = new TextView(AndroidCamera.this);
            TextView tr_column1 = new TextView(AndroidCamera.this);
            for (int td = 0; td < 2; td++) {
                String text = pref_Table.getString(select_count + "Table" + tr + "row" + td + "column", null);
                if (td == 0) {
                    TableRow.LayoutParams td1param = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    td1param.weight = 2.0f;
                    tr_column0.setText(text);
                    tr_column0.setTextColor(Color.BLACK);
                    tr_column0.setTextSize(9);
                    tr_column0.setPadding(20, 5, 20, 5);
                    tr_column0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    tr_column0.setBackground(getResources().getDrawable(R.drawable.table_border));
                    tr_column0.setLayoutParams(td1param);
                    row[tr].addView(tr_column0);
                } else {
                    if (text == null) {
                        text = " ";
                    }
                    TableRow.LayoutParams td2param = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    td2param.weight = 8.0f;
                    tr_column1.setText(text);
                    tr_column1.setTextColor(Color.BLACK);
                    tr_column1.setTextSize(9);
                    tr_column1.setPadding(20, 5, 30, 5);
                    tr_column1.setBackground(getResources().getDrawable(R.drawable.table_border));
                    tr_column1.setLayoutParams(td2param);
                    row[tr].addView(tr_column1);
                }
            }
            prev_table.addView(row[tr]);
        }
        prev_table.post(new Runnable() {
            @Override
            public void run() {
                width = prev_table.getWidth();
                height = prev_table.getHeight();
                Log.d("width", String.valueOf(width));
                Log.d("height", String.valueOf(height)); }});
        prev_table.setBackground(getResources().getDrawable(R.drawable.table_border));

        linear.addView(prev_table);
        picture_frame.addView(linear);
        first_frame.addView(picture_frame);

        prev_table.setOnLongClickListener(new View.OnLongClickListener() {
            DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
            int rotation = getWindowManager().getDefaultDisplay().getRotation();

            @Override
            public boolean onLongClick(View v) {
                switch (rotation){
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (move_count %2 == 0){
                            int width_move = dm.widthPixels - picture_frame.getWidth();
                            picture_frame.removeView(linear);
                            linear.removeView(prev_table);
                            picture_frame.setX(width_move);
                            linear.addView(prev_table);
                            picture_frame.addView(linear);
                            move_count++;
                        }
                        else if (move_count %2 == 1){
                            picture_frame.setX(0);
                            move_count++;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case  Surface.ROTATION_270:
                        if (move_count %2 == 0){
                            LinearLayout linearpicture = findViewById(R.id.linearpicture);
                            int width_move = linearpicture.getWidth() - picture_frame.getWidth();
                            picture_frame.removeView(linear);
                            linear.removeView(prev_table);
                            picture_frame.setX(width_move);
                            linear.addView(prev_table);
                            picture_frame.addView(linear);
                            move_count++;
                        }else if (move_count %2 == 1){
                            picture_frame.setX(0);
                            move_count++;
                        }
                        break;
                }
                return true;
            }                               //테이블 꾹 눌렀을때 옆으로 이동
        });

        ImageButton btn = findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(View v) {
                picture_count++;
                intent_camera = new Intent();

                camera.takePicture(null, null, jpegCallback);
                StringBuilder table_info = new StringBuilder( );
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                String formatDate = sdfNow.format(date);
                for(int tr = 0 ; tr<tr_length; tr++){
                    String text = pref_Table.getString(select_count + "Table" + tr + "row" + 1 + "column", null);
                   table_info.append(text).append("/");
                }
                Log.d("table_info", String.valueOf(table_info));

                if (picture_count >35){
                    picture_count = 35;
                    editor_history.remove("picture_time"+1).apply();
                    editor_history.remove("picture_info"+1).apply();
                    for (int i=2; i<picture_count+1 ; i++){
                        String picture_time = pref_history.getString("picture_time"+i, null);
                        String picture_info = pref_history.getString("picture_info"+i, null);
                        editor_history.putString("picture_time"+(i-1), picture_time).apply();
                        editor_history.putString("picture_info"+(i-1), picture_info).apply();
                    }
                    editor_history.putInt("picture_count", picture_count).apply();
                    editor_history.putString("picture_time"+ picture_count, formatDate).apply();
                    editor_history.putString("picture_info" + picture_count, String.valueOf(table_info)).apply();

                }else{
                    editor_history.putInt("picture_count", picture_count).apply();
                    editor_history.putString("picture_time"+ picture_count, formatDate).apply();
                    editor_history.putString("picture_info" + picture_count, String.valueOf(table_info)).apply();
                }       // 히스토리 35개 초과하면 맨처음꺼 지우고 뒤부터 하나씩 땡기기

                tableDataList = new ArrayList<>();
                tableDataList.add(new Listitem(String.valueOf(table_info),formatDate));
                myAdapter.notifyDataSetChanged();

            }
        });

        hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);



    }

    @Override
    public void onResume(){
        super.onResume();
        myAdapter.upDateItemList(tableDataList);
    }                                                               //리스트뷰 바로 갱신위한 메소드
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Get the pointer ID
        Camera.Parameters params =camera.getParameters();
        int action = event.getAction();
        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerSpacing(event);
            }
            else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                camera.cancelAutoFocus();
                handleZoom(event, params);
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                handleFocus(event, params);
            }
        }
        return true;
    }                                                                           //카메라 확대 축소 터치이벤트
    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_camera);

        }
        else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_camera);

        }
        super.onConfigurationChanged(newConfig);
    }           // 회전시 레이아웃 각각 설정 가로모드 세로모드

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @SuppressLint("WrongConstant")
        public void onPictureTaken(byte[] data, Camera camera) {
            int w = camera.getParameters().getPictureSize().width;
            int h = camera.getParameters().getPictureSize().height;
            Bitmap bitmap_camera = BitmapFactory.decodeByteArray(data,0, data.length);

            BitmapFactory.Options options;
            Bitmap bitmap_camera_resize = null;
//            Bitmap resized = Bitmap.createScaledBitmap(bitmap_camera_resize, bitmap_camera_resize.getWidth(), bitmap_camera_resize.getHeight(), true);
            Matrix matrix = new Matrix();
            matrix.postRotate(90.0f);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap_camera, 0, 0, bitmap_camera.getWidth(), bitmap_camera.getHeight(), matrix, true);
            Bitmap overlayBitmap = Bitmap.createBitmap(rotatedBitmap,0,0,rotatedBitmap.getWidth(),rotatedBitmap.getHeight());


            if(quality_count%3==0){
                options = new BitmapFactory.Options();
                options.inSampleSize = 5;
                bitmap_camera_resize = BitmapFactory.decodeByteArray(data,0,data.length,options);
            }else if(quality_count%3==1){
                options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                bitmap_camera_resize = BitmapFactory.decodeByteArray(data,0,data.length,options);
            }else if(quality_count%3==2){
                options = new BitmapFactory.Options();
                options.inSampleSize = 3;
                bitmap_camera_resize = BitmapFactory.decodeByteArray(data,0,data.length,options);
            }
            assert bitmap_camera_resize != null;
            Bitmap rotatedBitmap_resize = Bitmap.createBitmap(bitmap_camera_resize, 0, 0, bitmap_camera_resize.getWidth(), bitmap_camera_resize.getHeight(), matrix, true);

            Bitmap overlayBitmap1 = Bitmap.createBitmap(rotatedBitmap_resize,0,0,rotatedBitmap_resize.getWidth(),rotatedBitmap_resize.getHeight());
            Bitmap overlayBitmap2 = bitmap_camera_resize.copy(Bitmap.Config.ARGB_8888,true);

            Canvas canvas = new Canvas(overlayBitmap);
            Canvas canvas_send1 = new Canvas(overlayBitmap1);
            Canvas canvas_send2 = new Canvas(overlayBitmap2);

            canvas.drawBitmap(rotatedBitmap,0,0,null);
            canvas_send1.drawBitmap(overlayBitmap1,0,0,null);
            canvas_send2.drawBitmap(overlayBitmap2,0,0,null);

            first_frame.setDrawingCacheEnabled(true);
            first_frame.buildDrawingCache();
            Bitmap bitmap_frame = first_frame.getDrawingCache();
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            Bitmap bitmap_temp2 = null;
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees =0;
                    bitmap_temp2 = Bitmap.createScaledBitmap(bitmap_frame,h,w,true);

                    Matrix matrix_0 = new Matrix();
                    matrix_0.postRotate(degrees);
                    Bitmap bitmap_temp_0 = Bitmap.createBitmap(bitmap_temp2, 0, 0,bitmap_temp2.getWidth(), bitmap_temp2.getHeight() , matrix_0, true);
                    Bitmap bitmap_temp_01 = Bitmap.createScaledBitmap(bitmap_temp_0,overlayBitmap1.getWidth(),overlayBitmap1.getHeight(), true);

                    canvas_send1.drawBitmap(bitmap_temp_01, 0,0,null);
                    ByteArrayOutputStream stream_0 = new ByteArrayOutputStream();

                    overlayBitmap1.compress(Bitmap.CompressFormat.JPEG, 100, stream_0);
                    byte[] currentData_0 = stream_0.toByteArray();
                    bytes = currentData_0;
                    intent_camera.putExtra("image", currentData_0);

                    break;

                case Surface.ROTATION_90:
                    degrees = 90;
                    bitmap_temp2 = Bitmap.createScaledBitmap(bitmap_frame,w,h,true);

                    Bitmap bitmap_temp_90 = Bitmap.createScaledBitmap(bitmap_frame,overlayBitmap2.getWidth(),overlayBitmap2.getHeight(), true);


                    canvas_send2.drawBitmap(bitmap_temp_90, 0,0,null);
                    ByteArrayOutputStream stream_90 = new ByteArrayOutputStream();
                    overlayBitmap2.compress(Bitmap.CompressFormat.JPEG, 100, stream_90);
                    byte[] currentData_90 = stream_90.toByteArray();
                    bytes = currentData_90;
                    intent_camera.putExtra("image", currentData_90);
                    break;

                case Surface.ROTATION_180:
                    degrees = 180;
                    bitmap_temp2 = Bitmap.createScaledBitmap(bitmap_frame,h,w,true);
                    Matrix matrix1 = new Matrix();
                    matrix1.postRotate(degrees);
                    Bitmap bitmap_temp_180 = Bitmap.createBitmap(bitmap_temp2, 0, 0,bitmap_temp2.getWidth(), bitmap_temp2.getHeight() , matrix1, true);
                    Bitmap bitmap_temp_1801 = Bitmap.createScaledBitmap(bitmap_temp_180,overlayBitmap1.getWidth(),overlayBitmap1.getHeight(), true);

                    canvas_send1.drawBitmap(bitmap_temp_1801, 0,0,null);
                    ByteArrayOutputStream stream_180 = new ByteArrayOutputStream();
                    overlayBitmap1.compress(Bitmap.CompressFormat.JPEG, 100, stream_180);
                    byte[] currentData_180 = stream_180.toByteArray();
                    bytes = currentData_180;
                    intent_camera.putExtra("image", currentData_180);

                    break;

                case Surface.ROTATION_270:
                    degrees = 270;
                    bitmap_temp2 = Bitmap.createScaledBitmap(bitmap_frame,w,h,true);
                    Bitmap bitmap_temp_270 = Bitmap.createScaledBitmap(bitmap_frame,overlayBitmap2.getWidth(),overlayBitmap2.getHeight(), true);
                    canvas_send2.drawBitmap(bitmap_temp_270, 0,0,null);
                    ByteArrayOutputStream stream_270 = new ByteArrayOutputStream();
                    overlayBitmap2.compress(Bitmap.CompressFormat.JPEG, 100, stream_270);
                    byte[] currentData_270 = stream_270.toByteArray();
                    bytes = currentData_270;
                    intent_camera.putExtra("image", currentData_270);
                    break;
            }
            Matrix matrix1 = new Matrix();
            matrix1.postRotate(degrees);
            Bitmap bitmap_frame2 = Bitmap.createBitmap(bitmap_temp2, 0, 0,bitmap_temp2.getWidth(), bitmap_temp2.getHeight() , matrix1, true);
            canvas.drawBitmap(bitmap_frame2, 0,0,null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inSampleSize = 4;
            overlayBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] currentData = stream.toByteArray();

            Uri uriTarget = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
//            intent_camera.putExtra("Uri",uriTarget);
            FileOutputStream outStream = null;
            try {
                assert uriTarget != null;
                outStream = (FileOutputStream) getContentResolver().openOutputStream(uriTarget);
                assert outStream != null;
                outStream.write(currentData);
                outStream.flush();
                outStream.close();
                Log.d("TAG", "Image saved: " + uriTarget.toString());
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            Toast.makeText(getApplicationContext(),"저장 완료", Toast.LENGTH_SHORT).show();
//            Toast.makeText(getApplicationContext(), "저장 완료", 100).show();
            camera.startPreview();
        }
    };

    @Override
    public void onBackPressed() {
        if (intent_camera == null){
            finish();
        }else{
            setResult(RESULT_OK, intent_camera);
        }
        super.onBackPressed();
    }       //뒤로가기 버튼 클릭시 인텐트 정보 전달

    private void turnOnFlash() {
        if (!isFlashOn) {
            Camera.Parameters params = camera.getParameters();
            if (camera == null || params == null) {
                return;
            }

            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
            isFlashOn = true;
            isLighOn=1;
        }
    }
    private void turnOffFlash() {
        if (isFlashOn) {
            Camera.Parameters params = camera.getParameters();
            if (camera == null || params == null) {
                return;
            }

            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            isFlashOn = false;
            isLighOn=0;
        }
    }
    public void Dialog () {
        if (!hasFlash) {
            // device doesn't support flash
            // Show alert message and close the application
            final AlertDialog alert = new AlertDialog.Builder(AndroidCamera.this).create();
            alert.setTitle("Error");
            alert.setMessage("플래쉬 기능이 지원되지 않습니다. Sorry, your device doesn't support flash light!");
            alert.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    alert.dismiss();
                }
            });
            alert.show();
            return;
        }
    }
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.

        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            camera.stopPreview();

        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        try {
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;
            final Camera.Parameters params = camera.getParameters();
            final Camera.Parameters params_best = camera.getParameters();
            final Camera.Parameters params_good = camera.getParameters();
            final Camera.Parameters params_normal = camera.getParameters();

            List <Camera.Size> pictureSizes= params.getSupportedPictureSizes();
            List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();

            Collections.sort(pictureSizes, new Comparator<Camera.Size>() {
                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });
            Collections.sort(previewSizes, new Comparator<Camera.Size>() {
                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });
            List<Camera.Size> sort_pictureSizes = sortArray(pictureSizes);
            List<Camera.Size> sort_previewSizes = sortArray(previewSizes);

            List<Camera.Size> resultSizes;

            final int preview_width = sort_previewSizes.get(sort_previewSizes.size()-1).width;
            final int preview_height = sort_previewSizes.get(sort_previewSizes.size()-1).height;
            Log.d(String.valueOf(preview_width), "previewWIDTHBEST");
            Log.d(String.valueOf(preview_height), "previewHEIGHTBEST");

            resultSizes = BestPicturePreview(sort_pictureSizes,sort_previewSizes);

            final int picture_width_best = resultSizes.get(0).width;
            final int picture_height_best = resultSizes.get(0).height;
            final int picture_width_good = resultSizes.get(1).width;
            final int picture_height_good = resultSizes.get(1).height;
            final int picture_width_normal = resultSizes.get(2).width;
            final int picture_height_normal = resultSizes.get(2).height;

            final Button quality = findViewById(R.id.quality);
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    quality.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (quality_count % 3 == 0) {
                                Log.d("최고화질 진입", "최고화질 진입 성공");
                                quality.setText("최고화질");

                                FrameLayout.LayoutParams surfaview_param1 = new FrameLayout.LayoutParams(preview_height, preview_width);
                                FrameLayout.LayoutParams picture_param1 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param1.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param1);
                                surfaceView.setLayoutParams(surfaview_param1);
                                Log.d(String.valueOf(preview_width), "previewBest");
                                Log.d(String.valueOf(preview_height), "previewBest");
//                                FrameLayout.LayoutParams picture_param1 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//                                FrameLayout.LayoutParams linear_params1 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//                                LinearLayout.LayoutParams table_params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

//                                table_params1.gravity = Gravity.BOTTOM;
//                                linear_params1.gravity = Gravity.BOTTOM;
////                                picture_param1.gravity = Gravity.BOTTOM;
//
//                                prev_table.setLayoutParams(table_params1);
//                                linear.setLayoutParams(linear_params1);
//                                picture_frame.setLayoutParams(picture_param1);
//                                surfaceView.setLayoutParams(surfaview_param1);

                                params_best.setPictureSize(picture_width_best, picture_height_best);
                                params_best.setPreviewSize(preview_width, preview_height);
                                camera.setParameters(params_best);
                                quality_count++;
                            }
                            else if (quality_count % 3 == 1) {
                                Log.d("고화질 진입", "고화질 진입 성공");
                                quality.setText("고화질");
                                FrameLayout.LayoutParams surfaview_param2 = new FrameLayout.LayoutParams(preview_height, preview_width);
                                FrameLayout.LayoutParams picture_param2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param2.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param2);
                                surfaceView.setLayoutParams(surfaview_param2);
                                params_good.setPictureSize(picture_width_good, picture_height_good);
                                params_good.setPreviewSize(preview_width, preview_height);
                                camera.setParameters(params_good);
                                quality_count++;
                            }
                            else {
                                Log.d("일반화질 진입", "일반화질 진입 성공");
                                quality.setText("일반화질");
                                FrameLayout.LayoutParams surfaview_param3 = new FrameLayout.LayoutParams(preview_height, preview_width);
                                FrameLayout.LayoutParams picture_param3 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param3.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param3);
                                surfaceView.setLayoutParams(surfaview_param3);
                                params_normal.setPictureSize(picture_width_normal, picture_height_normal);
                                params_normal.setPreviewSize(preview_width, preview_height);
                                quality_count++;
                            }
                        }
                    });
                    break;

                case Surface.ROTATION_90:
                    degrees = 90;
                    FrameLayout.LayoutParams surfaview_param_2 = new FrameLayout.LayoutParams(preview_width, preview_height);
                    surfaceView.setLayoutParams(surfaview_param_2);
                    FrameLayout.LayoutParams picture_param_90 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    picture_param_90.gravity = Gravity.BOTTOM;
                    picture_frame.setLayoutParams(picture_param_90);
                    params.setPictureSize(picture_width_good, picture_height_good);
                    params.setPreviewSize(preview_width, preview_height);
                    quality.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (quality_count % 3 == 0) {
                                quality.setText("최고화질");
                                FrameLayout.LayoutParams surfaview_param1 = new FrameLayout.LayoutParams(preview_width, preview_height);
                                FrameLayout.LayoutParams picture_param1 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param1.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param1);
                                surfaceView.setLayoutParams(surfaview_param1);
                                params_best.setPictureSize(picture_width_best, picture_height_best);
                                params_best.setPreviewSize(preview_width, preview_height);
                                camera.setParameters(params_best);
                                quality_count++;
                            }
                            else if (quality_count % 3 == 1) {
                                quality.setText("고화질");
                                FrameLayout.LayoutParams surfaview_param2 = new FrameLayout.LayoutParams(preview_width, preview_height);
                                FrameLayout.LayoutParams picture_param2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param2.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param2);
                                surfaceView.setLayoutParams(surfaview_param2);
                                params_good.setPictureSize(picture_width_good, picture_height_good);
                                params_good.setPreviewSize(preview_width, preview_height);
                                camera.setParameters(params_good);
                                quality_count++;
                            }
                            else {
                                quality.setText("일반화질");
                                FrameLayout.LayoutParams surfaview_param3 = new FrameLayout.LayoutParams(preview_width, preview_height);
                                FrameLayout.LayoutParams picture_param3 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param3.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param3);
                                surfaceView.setLayoutParams(surfaview_param3);
                                params_normal.setPictureSize(picture_width_normal, picture_height_normal);
                                params_normal.setPreviewSize(preview_width, preview_height);
                                quality_count++;
                            }
                        }
                    });
                    break;

                case Surface.ROTATION_180:
                    degrees = 180;
                    quality.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (quality_count % 3 == 0) {
                                quality.setText("최고화질");
                                FrameLayout.LayoutParams surfaview_param1 = new FrameLayout.LayoutParams(preview_height, preview_width);
                                FrameLayout.LayoutParams picture_param1 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param1.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param1);
                                surfaceView.setLayoutParams(surfaview_param1);
                                params_best.setPictureSize(picture_width_best, picture_height_best);
                                params_best.setPreviewSize(preview_width, preview_height);
                                camera.setParameters(params_best);
                                quality_count++;
                            }
                            else if (quality_count % 3 == 1) {
                                quality.setText("고화질");
                                FrameLayout.LayoutParams surfaview_param2 = new FrameLayout.LayoutParams(preview_height, preview_width);
                                FrameLayout.LayoutParams picture_param2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param2.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param2);
                                surfaceView.setLayoutParams(surfaview_param2);
                                params_good.setPictureSize(picture_width_good, picture_height_good);
                                params_good.setPreviewSize(preview_width, preview_height);
                                camera.setParameters(params_good);
                                quality_count++;
                            }
                            else {
                                quality.setText("일반화질");
                                FrameLayout.LayoutParams surfaview_param3 = new FrameLayout.LayoutParams(preview_height, preview_width);
                                FrameLayout.LayoutParams picture_param3 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param3.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param3);
                                surfaceView.setLayoutParams(surfaview_param3);
                                params_normal.setPictureSize(picture_width_normal, picture_height_normal);
                                params_normal.setPreviewSize(preview_width, preview_height);
                                quality_count++;
                            }
                        }
                    });
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    FrameLayout.LayoutParams surfaview_param_3 = new FrameLayout.LayoutParams(preview_width, preview_height);
                    FrameLayout.LayoutParams picture_param_270 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    picture_param_270.gravity = Gravity.BOTTOM;
                    picture_frame.setLayoutParams(picture_param_270);
                    surfaceView.setLayoutParams(surfaview_param_3);
                    params.setPictureSize(picture_width_good,picture_height_good);
                    params.setPreviewSize(preview_width,preview_height);
                    quality.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (quality_count % 3 == 0){
                                quality.setText("최고화질");
                                FrameLayout.LayoutParams surfaview_param1 = new FrameLayout.LayoutParams(preview_width, preview_height);
                                FrameLayout.LayoutParams picture_param1 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param1.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param1);
                                surfaceView.setLayoutParams(surfaview_param1);
                                params_best.setPictureSize(picture_width_best,picture_height_best);
                                params_best.setPreviewSize(preview_width,preview_height);
                                camera.setParameters(params_best);
                                quality_count++;
                            }
                            else if (quality_count % 3 == 1){
                                quality.setText("고화질");
                                FrameLayout.LayoutParams surfaview_param2 = new FrameLayout.LayoutParams(preview_width, preview_height);
                                FrameLayout.LayoutParams picture_param2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param2.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param2);
                                surfaceView.setLayoutParams(surfaview_param2);
                                params_good.setPictureSize(picture_width_good,picture_height_good);
                                params_good.setPreviewSize(preview_width,preview_height);
                                camera.setParameters(params_good);
                                quality_count++;
                            }
                            else{
                                quality.setText("일반화질");
                                FrameLayout.LayoutParams surfaview_param3 = new FrameLayout.LayoutParams(preview_width, preview_height);
                                FrameLayout.LayoutParams picture_param3 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                picture_param3.gravity = Gravity.BOTTOM;
                                picture_frame.setLayoutParams(picture_param3);
                                surfaceView.setLayoutParams(surfaview_param3);
                                params_normal.setPictureSize(picture_width_normal,picture_height_normal);
                                params_normal.setPreviewSize(preview_width,preview_height);
                                quality_count++;
                            }
                        }
                    });
                    break;
            }
            final int result  = (90 - degrees + 360) % 360;
            flash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (hasFlash){
                        if (isLighOn==1){
                            turnOffFlash();
                        } else {
                            turnOnFlash();
                        }
                    } else {
                        Dialog();
                    }
                }
            });


            camera.setDisplayOrientation(result);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
        }

    }
    @SuppressLint("LongLogTag")
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera
            camera = Camera.open();
            Camera.Parameters params = camera.getParameters();
//            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            List <Camera.Size> pictureSizes= params.getSupportedPictureSizes();
            List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
            Collections.sort(pictureSizes, new Comparator<Camera.Size>() {
                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });
            Collections.sort(previewSizes, new Comparator<Camera.Size>() {
                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });

            List<Camera.Size> sort_pictureSizes = sortArray(pictureSizes);
            List<Camera.Size> sort_previewSizes = sortArray(previewSizes);

            for (int i=0;i<sort_pictureSizes.size();i++) {
                Log.i("sortPictureSize", "Supported Size: " + "width: " + sort_pictureSizes.get(i).width + "height : " + sort_pictureSizes.get(i).height);
            }
            for (int i=0;i<sort_pictureSizes.size();i++) {
                Log.i("sortPictureratio", "Picture ratio: " + ((float)sort_pictureSizes.get(i).width/ (float)sort_pictureSizes.get(i).height));
            }
            for (int i=0;i<sort_previewSizes.size();i++) {
                Log.i("sortPreviewSize", "Supported Size: " + "width: " + sort_previewSizes.get(i).width + "height : " + sort_previewSizes.get(i).height);
            }
            for (int i=0;i<sort_previewSizes.size();i++) {
                Log.i("Previewratio", "Preview ratio: " + ((float)sort_previewSizes.get(i).width /(float)sort_previewSizes.get(i).height));
            }

            List<Camera.Size> resultSizes;
            int preview_width = sort_previewSizes.get(sort_previewSizes.size()-1).width;
            int preview_height = sort_previewSizes.get(sort_previewSizes.size()-1).height;

            resultSizes = BestPicturePreview(sort_pictureSizes,sort_previewSizes);
//            resultSizes = GoodPicturePreview(sort_pictureSizes,sort_previewSizes);

            for (int i = 0; i < resultSizes.size(); i++) {
                Log.d("resultSize", "Supported Size: " + resultSizes.get(i).width + "height : " + resultSizes.get(i).height);
            }

            int picture_width = resultSizes.get(1).width;
            int picture_height = resultSizes.get(1).height;
            Log.d("previewwidth", String.valueOf(preview_width));
            Log.d("previewheight", String.valueOf(preview_height));
            FrameLayout.LayoutParams surfaview_param = new FrameLayout.LayoutParams(preview_height, preview_width);
//            FrameLayout.LayoutParams surfaview_param = new FrameLayout.LayoutParams(preview_width, preview_height);
            FrameLayout.LayoutParams picture_param = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            FrameLayout.LayoutParams linear_params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams table_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            table_params.gravity = Gravity.BOTTOM;
            linear_params.gravity = Gravity.BOTTOM;
            picture_param.gravity = Gravity.BOTTOM;

            prev_table.setLayoutParams(table_params);
            linear.setLayoutParams(linear_params);
            picture_frame.setLayoutParams(picture_param);
            surfaceView.setLayoutParams(surfaview_param);
            params.setPictureSize(picture_width,picture_height);
            params.setPreviewSize(preview_width,preview_height);
            camera.setParameters(params);
        }
        catch (RuntimeException e) {
            System.err.println(e);
            return;
        }
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            return;
        }
    }
    public void surfaceDestroyed(SurfaceHolder holder) {
// TODO Auto-generated method stub
        camera.stopPreview();
//        camera.setPreviewCallback(null);
        camera.release();
        camera = null;
        previewing = false;
    }
    private boolean hasPermissions() {
        int res = 0;
        // list all permissions which you want to check are granted or not.
        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        for (String perms : permissions) {
            res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)) {
                // it return false because your app dosen't have permissions.
                return false;
            }

        }
        // it return true, your app has permissions.
        return true;
    }
    private void requestNecessaryPermissions() {
        // make array of permissions which you want to ask from user.
        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // have arry for permissions to requestPermissions method.
            // and also send unique Request code.
            requestPermissions(permissions, REQUEST_CODE_STORAGE_PERMS);
        }
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grandResults) {
        // this boolean will tell us that user granted permission or not.
        boolean allowed = true;
        switch (requestCode) {
            case REQUEST_CODE_STORAGE_PERMS:
                for (int res : grandResults) {
                    // if user granted all required permissions then 'allowed' will return true.
                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                    Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                // if user denied then 'allowed' return false
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                allowed = false;
                break;
        }
        if (allowed) {
            // if user granted permissions then do your work.
            //startCamera();
            doRestart(this);
        } else {
            // else give any custom waring message.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(AndroidCamera.this, "Camera Permissions denied", Toast.LENGTH_SHORT).show();
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(AndroidCamera.this, "Storage Permissions denied", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }
    public static void doRestart(Context c) {
        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        assert mgr != null;
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    } else {
                        Log.e(TAG, "Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Log.e(TAG, "Was not able to restart application, PM null");
                }
            } else {
                Log.e(TAG, "Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Was not able to restart application");
        }
    }

    public List<Camera.Size> BestPicturePreview(List<Camera.Size> pictureSizes, List<Camera.Size> previewSizes ) {
        List<Camera.Size> resultSizes = new ArrayList<>();
        float preview_ratio = (float) previewSizes.get(previewSizes.size() - 1).width / (float) previewSizes.get(previewSizes.size() - 1).height;
        for (int i = pictureSizes.size() - 1; i > 0; i--) {
            float picture_ratio = ((float) pictureSizes.get(i).width / (float) pictureSizes.get(i).height);
            if (preview_ratio == picture_ratio) {
                resultSizes.add(pictureSizes.get(i));
            }
        }
        return resultSizes;
    }
    public List<Camera.Size> GoodPicturePreview(List<Camera.Size> pictureSizes, List<Camera.Size> previewSizes ) {
        List<Camera.Size> resultSizes = new ArrayList<>();
        float preview_ratio = (float) previewSizes.get(pictureSizes.size()/2 +((pictureSizes.size()-(pictureSizes.size()/2))/2)).width / (float) pictureSizes.get(pictureSizes.size()/2 +((pictureSizes.size()-(pictureSizes.size()/2))/2)).height;
        for (int i = previewSizes.size() - 1; i > 0; i--) {
            float picture_ratio = ((float) previewSizes.get(i).width / (float) previewSizes.get(i).height);
            if (picture_ratio == preview_ratio) {
                resultSizes.add(previewSizes.get(i));
            }
        }
        return resultSizes;
    }
    public List<Camera.Size> NormalPicturePreview(List<Camera.Size> pictureSizes, List<Camera.Size> previewSizes ) {
        List<Camera.Size> resultSizes = new ArrayList<>();
        float picture_ratio = (float) pictureSizes.get(pictureSizes.size()/2).width / (float) pictureSizes.get(pictureSizes.size()/2).height;
        for (int i = previewSizes.size() - 1; i > 0; i--) {
            float preview_ratio = ((float) previewSizes.get(i).width / (float) previewSizes.get(i).height);
            if (picture_ratio == preview_ratio) {
                resultSizes.add(previewSizes.get(i));
            }
        }
        return resultSizes;
    }
    public List<Camera.Size> sortArray(List<Camera.Size> resultSizes) {
        for (int i = 0; i < resultSizes.size()-1; i++) {
            for (int j = i + 1; j < resultSizes.size(); j++) {
                if (resultSizes.get(i).width > resultSizes.get(j).width ) {
                    if (resultSizes.get(i).height >= resultSizes.get(j).height){
                        int temp_width = resultSizes.get(i).width;
                        int temp_height = resultSizes.get(i).height;
                        resultSizes.get(i).width = resultSizes.get(j).width;
                        resultSizes.get(i).height = resultSizes.get(j).height;
                        resultSizes.get(j).width = temp_width;
                        resultSizes.get(j).height = temp_height;
                    }else if (resultSizes.get(i).height < resultSizes.get(j).height){
                        int temp_width = resultSizes.get(i).width;
                        int temp_height = resultSizes.get(i).height;
                        resultSizes.get(i).width = resultSizes.get(j).width;
                        resultSizes.get(i).height = resultSizes.get(j).height;
                        resultSizes.get(j).width = temp_width;
                        resultSizes.get(j).height = temp_height;
                    }
                    else if (resultSizes.get(i).width == resultSizes.get(j).width){
                        if (resultSizes.get(i).height >= resultSizes.get(j).height){
                            int temp_width = resultSizes.get(i).width;
                            int temp_height = resultSizes.get(i).height;
                            resultSizes.get(i).width = resultSizes.get(j).width;
                            resultSizes.get(i).height = resultSizes.get(j).height;
                            resultSizes.get(j).width = temp_width;
                            resultSizes.get(j).height = temp_height;
                        }
                    }
//                }else if (resultSizes.get(i).width == resultSizes.get(j).width && ){
//                    int temp_width = resultSizes.get(i).width;
//                    int temp_height = resultSizes.get(i).height;
//                    resultSizes.get(i).width = resultSizes.get(j).width;
//                    resultSizes.get(i).height = resultSizes.get(j).height;
//                    resultSizes.get(j).width = temp_width;
//                    resultSizes.get(j).height = temp_height;
//                    else if(){
//
//                    }
                }
            }
        }
        return  resultSizes;
    }
    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        camera.setParameters(params);
    }
    public void handleFocus(MotionEvent event, Camera.Parameters params) {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    // currently set to auto-focus on single touch
                }
            });
        }
    }
    private float getFingerSpacing(MotionEvent event) {

        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState){
        //상위 클래스를 호출하는 것은 필수 - 뷰의 계층 구조 상태를 복원하기 위함
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putByteArray("bytes", bytes);
    }

//    public void InitializeTableData(){
//        tableDataList = new ArrayList<>();
//
//        int picture_count = pref_history.getInt("picture_count",0);
//
//        for (int i=0 ; i<picture_count; i++){
//            String picture_time = pref_history.getString("picture_time"+ (i+1),null);
//            String table_info = pref_history.getString("history"+(i+1),null);
//            tableDataList.add(new Listitem(table_info,picture_time));
//        }
//    }
}
