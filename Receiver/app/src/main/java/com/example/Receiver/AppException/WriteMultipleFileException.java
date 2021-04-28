package com.example.Receiver.AppException;

//  前一个文件未关闭的情况下接收到 FILE_CREATE 触发次异常
public class WriteMultipleFileException extends RuntimeException{
    public WriteMultipleFileException(){}

    public WriteMultipleFileException(String msg){
        super(msg);
    }
}
