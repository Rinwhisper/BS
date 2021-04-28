package com.example.Receiver.AppException;

// FILE_APPEND 时 fw == null 触发次异常
public class WriteNullFileException extends RuntimeException{
    public WriteNullFileException(){}
    public WriteNullFileException(String msg){
        super(msg);
    }
}
