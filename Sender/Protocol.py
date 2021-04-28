'''
Author       : Rinwhisper
Date         : 2021-03-04 23:55:39
LastEditTime : 2021-03-06 14:13:27
LastEditors  : Rinwhisper
FilePath     : \毕设代码\Protocol.py
Description  : 
'''
import zlib
import struct
import base64
import qrcode

class Protocol:
    """
    自定义的协议类，用于对从文件中读取的数据进行封装\n
    输入为二进制数据流，输出为经过Base64编码的字符串\n
    """

    MAGIC_NUMBER = b"QR"

    # 标志普通数据包
    FLAG_DATA = 0

    # 标志md5校验包
    FLAG_MD5 = 1

    # 标志文件名包
    FLAG_FILENAME = 2


    @staticmethod
    def addHead(flag, id_, data: bytes) -> bytes:
        """
        MAGIC_NUMBER: 2 bytes b"QR"
            标志二维码的类型
        flag: unsigned char  0-255
            0 数据包，包中携带的数据为文件中的数据
            1 末尾 md5 校验包，携带的数据为整个文件的 md5
        ------
        id_: unsigned short  0-65535
            这一块的编号\n
        ------
        length: unsigned short  0-65535 
        ------
        CRC32: unsigned int  0-4,294,967,295
        ------
        data: 需要进行处理的数据，应为二进制数据\n

        处理完毕后整个包的结构为：\n
        MAGIC_NUMBER    |    flag    |   id_  |   length  |   CRC32 check   |   data
           b"QR"             1           2           2           4                 *       （单位：字节）

        """
        assert (flag >= 0 and flag <= 255), "flag 超出范围"
        assert (id_ >= 0 and id_ <= 65535), "id_ 超出范围"
        length = len(data)
        assert (length >= 0 and length <= (65535)), "data 长度超标，当前最多能容纳的长度为: {}".format(65535)
        
        CRC32 = zlib.crc32(data) & 0xffffffff

        head = struct.pack(">2sBHHI", Protocol.MAGIC_NUMBER, flag, id_, length, CRC32)
        return head + data

    @staticmethod
    def base64Encode(packet: bytes) -> str:
        """
        输入二进制，输出 base64 编码 utf-8 字符串
        """
        base64_bytes_str = base64.standard_b64encode(packet)
        return base64_bytes_str.decode()

    @staticmethod
    def produce(flag, id_, data: bytes):
        """
        输入 flag id_ data 输出经过封装、base64编码的 utf-8 字符串 的 二维码 PIL image
        """
        return qrcode.make(Protocol.base64Encode(Protocol.addHead(flag, id_, data)))
    

    



