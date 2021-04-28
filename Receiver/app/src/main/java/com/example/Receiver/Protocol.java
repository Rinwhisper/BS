package com.example.Receiver;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.util.Arrays;

//自定义协议类，用于对二维码扫出的 String 字符串拆包
//所有传输的数据都应该是 UTF-8 的 bytes/byte[]
//和 Sender Protocol 对应
//        """
//        flag: unsigned char  0-255
//            0 数据包，包中携带的数据为文件中的数据
//            1 末尾 md5 校验包，携带的数据为整个文件的 md5
//        ------
//        id_: unsigned short  0-65535
//            这一块的编号\n
//        ------
//        length: unsigned short  0-65535
//        ------
//        CRC32: unsigned int  0-4,294,967,295
//        ------
//        data: 需要进行处理的数据，应为二进制数据\n
//
//        处理完毕后整个包的结构为：\n
//                MAGIC_NUMBER    |    flag    |   id_  |   length  |   CRC32 check   |   data
//                b"QR"             1           2           2           4                 *       （单位：字节）

public class Protocol {

//    标志普通数据包
    static final int FLAG_DATA = 0;

//    标志 md5 校验包
    static final int FLAG_MD5 = 1;

//    标志文件名包
    static final int FLAG_FILENAME = 2;

//    魔数
    static final byte[] MAGIC_NUMBER = {'Q', 'R'};

//    magic_number 长度
    private static final int MAGIC_NUMBER_LENGTH = 2;

//    head 长度
    private static final int HEAD_LENGTH = 11;

//  对 base64 编码的字符串解码，返回解码后的 byte[]
    static public byte[] base64Decode(String src){
        return Base64.decode(src, Base64.DEFAULT);
    }


//    解析 byte[]
//    填充 Tuple 对象，名字参考 Sender Protocol
//    注意，这里每个数据位都提升一级，因为java没有无符号类型
    static public void parse(Tuple<byte[], Short, Integer, Integer, Long, byte[]> dst, byte[] src){

        byte[] magic_number = Arrays.copyOfRange(src, 0, Protocol.MAGIC_NUMBER_LENGTH);

        byte[] head_ = Arrays.copyOfRange(src, Protocol.MAGIC_NUMBER_LENGTH, Protocol.HEAD_LENGTH);
        byte[] data = Arrays.copyOfRange(src, Protocol.HEAD_LENGTH, src.length);

//        解析 除 magic_number 以外的 head
        ByteBuffer head = ByteBuffer.wrap(head_);
        short flag = (short)(head.get() & 0xff);
        int id = (int)(head.getShort() & 0xffff);
        int length = (int)(head.getShort() & 0xffff);
        long CRC32 = (long)(head.getInt() & 0xffffffffL);

//        填充dst
        dst.setData(magic_number, flag, id, length, CRC32, data);
    }

//    快捷方法
    static public void produce(Tuple<byte[], Short, Integer, Integer, Long, byte[]> dst, String src){
        Protocol.parse(dst, Protocol.base64Decode(src));
    }
}
