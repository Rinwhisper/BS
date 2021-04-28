package com.example.Receiver;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.CRC32;

public class ReceiveFileFragment extends Fragment implements View.OnClickListener, DialogInterface.OnClickListener{

//    日志 TAG
    static final String LOG_TAG = "ReceiverFileFragment";

//    packet 检查通过
    static final int CHECK_OK = 0;

//    id 错误
    static final int CHECK_ID_ERROR = 1;

//    长度错误
    static final int CHECK_LENGTH_ERROR = 2;

//    CRC32校验错误
    static final int CHECK_CRC32_ERROR = 3;

//    magic_number 错误
    static final int CHECK_MAGIC_NUMBER_ERROR = 4;


//  下一个期望的packet id
    int next_id;

//    md5 对象
    MessageDigest md5 = null;

//    CRC32 对象
    CRC32 CRC32 = null;

//    开始扫码按钮
    private Button rft_start_button = null;

//    通知栏
    private AlertDialog dialog = null;
    Button dialog_negative_button = null;
    Button dialog_positive_button = null;


//    Protocol 解析返回数据
    private Tuple<byte[], Short, Integer, Integer, Long, byte[]> packet = null;


//    FileThread 线程里的 handler
    private Handler handler = null;




    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receive_file, container, false);

//        初始化
        rft_start_button = (Button)view.findViewById(R.id.rft_start_button);
        rft_start_button.setOnClickListener(this);

        dialog = new AlertDialog.Builder(Objects.requireNonNull(getContext()))
                .setTitle(R.string.rft_dialog_prompt_title_text)
                .setMessage(R.string.rft_dialog_start_dialog_message_text)
                .setNegativeButton(R.string.rft_dialog_cancel_text, this)
                .setPositiveButton(R.string.rft_dialog_start_scan_text, this)
                .create();
        dialog.setCancelable(false);

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        CRC32 = new CRC32();

        next_id = 0;

        packet = new Tuple<byte[], Short, Integer, Integer, Long, byte[]>(null, (short)0, 0, 0, 0L, null);

//        获得 Activity 里的 Handler
        getHandler();

        return view;
    }

//    获得 activity 里的 handler
    private void getHandler(){
        handler = Objects.requireNonNull(((MainActivity) Objects.requireNonNull(getContext())).getHandler());
    }


    //    重置:
//    next_id
//    md5
//    packet
//    CRC32
    private void reset(){
        md5.reset();
        next_id = 0;
        packet.setData(null, (short) 0, 0, 0, 0L, null);
        CRC32.reset();

//        向 FileThread 发送一个删除文件的信息
//        主要是由于中途退出删除以保存的文件逻辑不好写，所以写在这
        handler.sendMessage(makeMessage(FileThread.FILE_DELETE, null));
    }




//    跳转到 ScanActivity.class 扫码
    public void scan(){
        startActivityForResult(new Intent(getContext(), ScanActivity.class), next_id);
    }

//    构造 message
    private Message makeMessage(int flag, byte[] data){
        Message message = Message.obtain();
        message.what = flag;
        message.obj = data;
        return message;
    }

//    更新 next_id
    private void updateNextId(){
        next_id = (next_id + 1) % 65536;
        if(next_id == 0){
//            跳过 id 0
//            因为 id = 0 标志着创建文件
            next_id = 1;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            try {
                Protocol.produce(packet, data.getStringExtra("SCAN_RESULT"));
            }catch (IllegalArgumentException e){
//                二维码不能被 base64 解码
                showDialog(R.string.rft_dialog_error_title_text,
                        R.string.rft_dialog_error_magic_number_message_text,
                        0,
                        R.string.rft_dialog_OK_text);
                return;
            }
            switch (check()){
                case CHECK_OK:{
                    switch (packet.getFlag()){
                        case Protocol.FLAG_FILENAME:{
//                            第一个 packet data 是 文件名
                            if(packet.getId() == 0){
//                                创建文件
                                handler.sendMessage(makeMessage(FileThread.FILE_CREATE, packet.getData()));
                                updateNextId();
//                              通知用户
//                                Toast.makeText(getContext(), R.string.rft_receive_success_text, Toast.LENGTH_SHORT).show();
                                showDialog(R.string.rft_dialog_prompt_title_text,
                                        R.string.rft_dialog_next_text,
                                        0,
                                        R.string.rft_dialog_next_button_text);
//                                scan();
                            }else{
//                                flag == FLAG_FILENAME 时 id 必须为 1
//                                这也意味着 FLAG_FILENAME 应该是第一个packet
                                showDialog(R.string.rft_dialog_error_title_text,
                                        R.string.rft_dialog_error_mismatched_flag_id_text,
                                        0,
                                        R.string.rft_dialog_OK_text);
                            }
                            break;
                        }
                        case Protocol.FLAG_DATA:{
                            md5.update(packet.getData());
                            handler.sendMessage(makeMessage(FileThread.FILE_APPEND, packet.getData()));
                            updateNextId();
                            Toast.makeText(getContext(), R.string.rft_receive_success_text, Toast.LENGTH_SHORT).show();
//                            scan();
                            showDialog(R.string.rft_dialog_prompt_title_text,
                                    R.string.rft_dialog_next_text,
                                    0,
                                    R.string.rft_dialog_next_button_text);
                            break;
                        }
                        case Protocol.FLAG_MD5:{
//                            md5 数据包
//                            检查 md5 是否正确

//                            digest 方法会重置 md5 对象
                            byte[] md5_ = md5.digest();
                            if(!Arrays.equals(md5_, packet.getData())){
//                                md5 不相同
                                handler.sendMessage(makeMessage(FileThread.FILE_DELETE, null));
                                showDialog(R.string.rft_dialog_error_title_text,
                                        R.string.rft_dialog_error_md5_text,
                                        R.string.rft_dialog_OK_text,
                                        0);
                            }else {
//                                md5 校验通过
                                handler.sendMessage(makeMessage(FileThread.FILE_CLOSE, null));
                                showDialog(R.string.rft_dialog_prompt_title_text,
                                        R.string.rft_dialog_receive_success_text,
                                        R.string.rft_dialog_OK_text,
                                        0);
                            }
                            break;
                        }
                    }


                    break;
                }
                case CHECK_ID_ERROR:{
                    showDialog(getString(R.string.rft_dialog_error_title_text),
                            String.format(getString(R.string.rft_dialog_error_id_message_text), next_id, packet.getId()),
                            null,
                            getString(R.string.rft_dialog_OK_text));
                    break;
                }
                case CHECK_LENGTH_ERROR:{
                    showDialog(R.string.rft_dialog_error_title_text, R.string.rft_dialog_error_length_message_text,
                            0, R.string.rft_dialog_OK_text);
                    break;
                }
                case CHECK_CRC32_ERROR:{
                    showDialog(R.string.rft_dialog_error_title_text, R.string.rft_dialog_error_CRC32_message_text,
                            0, R.string.rft_dialog_OK_text);
                    break;
                }
                case CHECK_MAGIC_NUMBER_ERROR:{
                    showDialog(R.string.rft_dialog_error_title_text,
                            R.string.rft_dialog_error_magic_number_message_text,
                            0,
                            R.string.rft_dialog_OK_text);
                }
            }
        }else if(resultCode == Activity.RESULT_CANCELED){
//            用户点击了退出键
            showDialog(R.string.rft_dialog_prompt_title_text,
                    R.string.rft_dialog_cancel_message_text,
                    R.string.rft_dialog_confirm_stop_text,
                    R.string.rft_dialog_continue_text
                    );
        }
    }

//    检查 packet head 中的 id length CRC32
    private int check(){
        if(!Arrays.equals(packet.getMagicNumber(), Protocol.MAGIC_NUMBER)){
            return CHECK_MAGIC_NUMBER_ERROR;
        }
        if(packet.getId() != next_id){
            return CHECK_ID_ERROR;
        }
        if(packet.getLength() != packet.getData().length){
            return CHECK_LENGTH_ERROR;
        }
        CRC32.update(packet.getData());
        if(packet.getCRC32() != CRC32.getValue()){
            CRC32.reset();
            return CHECK_CRC32_ERROR;
        }
        CRC32.reset();
        return CHECK_OK;
    }


//    dialog 的点击事件
//    dialog点击只有两种情况
//    1. 回到主界面，并重置Fragment状态(negative)
//    2. 跳转到ScanActivity，通过scan()方法(positive)
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_NEGATIVE){
//          情况1
//          回到主界面什么都不需要做
//          重置Fragment状态: reset()


            reset();
        }else if(which == DialogInterface.BUTTON_POSITIVE){
//          情况2
//          跳转ScanActivity通过scan()
            scan();
        }
    }

//    rft_start_button 的点击事件
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.rft_start_button){
            if(dialog_negative_button == null && dialog_positive_button == null){
                dialog.show();
                dialog_negative_button = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                dialog_positive_button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            }else{
                showDialog(R.string.rft_dialog_prompt_title_text, R.string.rft_dialog_start_dialog_message_text,
                        R.string.rft_dialog_cancel_text, R.string.rft_dialog_start_scan_text);
            }
        }
    }

//    定制 dialog
//    negative_button_text_id = 0 negative_button_text = null 隐藏按钮
//    positive_button_text_id = 0 positive_button_text = null 隐藏按钮
    private void showDialog(int title_id, int message_id, int negative_button_text_id, int positive_button_text_id){
        String title = getString(title_id);
        String message = getString(message_id);
        String negative_button_text = null;
        String positive_button_text = null;
        if(negative_button_text_id != 0){
            negative_button_text = getString(negative_button_text_id);
        }
        if(positive_button_text_id != 0){
            positive_button_text = getString(positive_button_text_id);
        }

        showDialog(title, message, negative_button_text, positive_button_text);
    }

//    见 重载 showDialog()
    private void showDialog(String title, String message, String negative_button_text, String positive_button_text){
        setDialog(title, message, negative_button_text, positive_button_text);
        dialog.show();
    }



//    见 showDialog()
    private void setDialog(String title, String message, String negative_button_text, String positive_button_text) {
        dialog.setTitle(title);
        dialog.setMessage(message);
        if(negative_button_text == null){
//            隐藏按钮
            dialog_negative_button.setVisibility(View.GONE);
        }else{
//            首先设置按钮可见
            dialog_negative_button.setVisibility(View.VISIBLE);
//            再设置按钮的text
            dialog_negative_button.setText(negative_button_text);
        }
//        同上
        if(positive_button_text == null){
            dialog_positive_button.setVisibility(View.GONE);
        }else{
            dialog_positive_button.setVisibility(View.VISIBLE);
            dialog_positive_button.setText(positive_button_text);
        }
    }
}
