package com.example.Receiver;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.Receiver.AppException.WriteMultipleFileException;
import com.example.Receiver.AppException.WriteNullFileException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class FileThread extends Thread{
    private static final String name = "FileThread";

//    创建文件
    static final int FILE_CREATE = 0;

//    填充文件
    static final int FILE_APPEND = 1;

//    关闭文件
    static final int FILE_CLOSE = 2;

//    删除文件
    static final int FILE_DELETE = 3;


//    app 下载目录
    String app_download_dir;



//    当前正在写的文件
    String filepath = null;

//    指向文件的 fw
    BufferedOutputStream fw = null;

//    默认 buffer 大小
    static int default_buffer_size = 8000;

    private Looper looper;

    public final Object handlerLock = new Object();
    public Handler handler = null;

    FileThread(@NonNull String app_download_dir){
        super(name);
        this.app_download_dir = app_download_dir;
    }

//  结束线程
    public void finish(){
//        删掉发送到一半的文件
        Message message = Message.obtain();
        message.what = FILE_DELETE;
        handler.sendMessage(message);
        Log.e("FILETHEAD", String.valueOf(Looper.myLooper()));
//        不在接收 message
        looper.quitSafely();
    }

    @Override
    public void run() {
        super.run();
        Looper.prepare();
        synchronized (handlerLock){
            initHandler();
        }
        looper = Looper.myLooper();
        Looper.loop();
    }

//  在这写 handler
    private void initHandler(){
        Log.e("FILETHREAD", String.valueOf(Looper.myLooper()));
        handler = new Handler(Looper.myLooper(), (msg -> {
            switch (msg.what){
                case FileThread.FILE_CREATE:{
                    if(fw != null){
                        throw new WriteMultipleFileException();
                    }else {
                        try {
                            filepath = app_download_dir + File.separator + (new String((byte[])msg.obj, StandardCharsets.UTF_8));
                            fw = new BufferedOutputStream(new FileOutputStream(filepath), default_buffer_size);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
                case FileThread.FILE_APPEND:{
                    if(fw == null){
                        throw new WriteNullFileException();
                    }else {
                        try {
                            fw.write((byte[])msg.obj);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
                case FileThread.FILE_CLOSE:{
                    if(fw == null){
                        throw new WriteNullFileException("关闭 null fw");
                    }else {
                        try {
                            fw.flush();
                            fw.close();
                            filepath = null;
                            fw = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
                case FileThread.FILE_DELETE:{
//                    这个情况有点特殊，因为中途退出删除保存一半的文件逻辑不好写，
//                    我给他写到了 reset() 里（左键），导致可能经常发送 FILE_DELETE 信号
//                    无视就好
                    if(fw != null){
                        try {
                            fw.flush();
                            fw.close();
                            new File(filepath).delete();
                            filepath = null;
                            fw = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
            return true;
        }));
    }
}
