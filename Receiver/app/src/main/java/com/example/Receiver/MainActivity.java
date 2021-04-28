package com.example.Receiver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.animation.Keyframe;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
//  权限
//        要申请的权限
//        写文件
    String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

//         请求权限码
    static final int REQUEST_PERMISSION_CODE = 1;


//  目录
    //    app 根目录
    String app_root_directory;

    //    app 下载根目录
    String app_download_directory;

//    创建的 写文件线程
    FileThread file_thread;

//  FileThread 的 handler
    Handler handler;


//    两个按钮
    private TextView receive_file_tab;
    private TextView my_files_tab;

//    按钮对应的 fragment
    private ReceiveFileFragment rffg;
    private MyFilesFragment mffg;

//    用于设置上面的title
    ActionBar action_bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        申请权限
        getPermission();

//        检查或创建 app 下载文件夹
        createAppDirectory(getString(R.string.app_name));

        bindViews();

        //        初始化保存文件子线程
        initFileThread();

//        开始点击 一下接收文件按钮
        receive_file_tab.performClick();
    }

//    绑定按钮和事件监听器
    private void bindViews(){
        receive_file_tab = findViewById(R.id.receive_file_tab);
        my_files_tab = findViewById(R.id.my_files_tab);
        receive_file_tab.setOnClickListener(this);
        my_files_tab.setOnClickListener(this);

        action_bar = getSupportActionBar();
    }


//    获得写文件权限
    private void getPermission(){
//        API 23 之下会自动授予
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
//            检查有没有获得这些权限

//            没有获得权限
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSION_CODE){
            for(int index = 0; index < permissions.length; index++){
                if(permissions[index].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[index] == PackageManager.PERMISSION_DENIED){
//                    没有获得写文件权限
//                    退出程序
                    finish();
                }
            }
        }
    }

    //    创建 app 存储目录
    private void createAppDirectory(String app_name){
        app_root_directory = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + app_name;
        app_download_directory = app_root_directory + File.separator + "Download";
        File download_dir = new File(app_download_directory);
        if(!download_dir.exists()){
//            文件夹不存在，连带父文件夹一起创建
            download_dir.mkdirs();
        }
    }


//    下面两个 tab
    @Override
    public void onClick(View v) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        int id = v.getId();
        if(id == R.id.receive_file_tab){
            action_bar.setTitle(R.string.receive_file_tab_text);
            receive_file_tab.setSelected(true);
            my_files_tab.setSelected(false);
            if(mffg != null){
                transaction.hide(mffg);
            }
            if(rffg == null){
                rffg = new ReceiveFileFragment();
                transaction.add(R.id.frame_layout, rffg);
            }else{
                transaction.show(rffg);
            }
        }else if(id == R.id.my_files_tab){
            action_bar.setTitle(R.string.my_files_tab_text);
            receive_file_tab.setSelected(false);
            my_files_tab.setSelected(true);
            if(rffg != null){
                transaction.hide(rffg);
            }
            if(mffg == null){
                mffg = new MyFilesFragment();
                Bundle bundle = new Bundle();
                bundle.putString("app_download_directory", app_download_directory);
                mffg.setArguments(bundle);
                transaction.add(R.id.frame_layout, mffg);
            }else {
                transaction.show(mffg);
            }
        }
        transaction.commit();
    }


    //    初始化保存文件子线程
    private void initFileThread() {
        file_thread = new FileThread(app_download_directory);
        file_thread.start();
//        handler = file_thread.handler;
    }

//    返回 activity 创建的 FileThread 中的 handler
//    供 ReceiveFileFragment 使用
//    return: Handler
    public Handler getHandler(){
        while (true){
            synchronized (file_thread.handlerLock){
                if(file_thread.handler != null){
                    return file_thread.handler;
                }
            }
        }
    }


//      MainActivity 销毁的时候结束 FileThread 线程
    @Override
    protected void onDestroy() {
        file_thread.finish();
        super.onDestroy();
    }
}