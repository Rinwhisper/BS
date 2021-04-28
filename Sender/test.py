'''
Author       : Rinwhisper
Date         : 2021-03-03 13:35:56
LastEditTime : 2021-03-18 15:26:37
LastEditors  : Rinwhisper
FilePath     : \Sender\test.py
Description  : 
'''
import tkinter.filedialog
import tkinter.messagebox
import tkinter as tk
from PIL import Image, ImageTk
import qrcode
import sys
import struct
from Protocol import Protocol

from GUI import GUI
app = GUI()
app.mainloop()

# from Protocol import Protocol
# a = Protocol.produce(0, 0, b"abcdefg")
# print(a)
# qr = qrcode.make("任傲赢")
# qr.show()
# a = Protocol.produce(1, 1, "任傲赢".encode())
# print(type(a))
# print(a)
# a = b""
# if a:
#     print("1")

# import hashlib
# md5 = hashlib.md5()
# md5.update(b"fsaf")
# print(md5.digest())
# from tkinter import ttk
# from tkinter import scrolledtext

# win = tk.Tk()

# win.title("first")
# # win.resizable(False, False)

# def fun():
#     button.configure(text = "You entered " + words.get())

# scr = scrolledtext.ScrolledText(win, width = 30, height = 3, wrap = tk.WORD)
# scr.grid(row = 0, column = 0, columnspan = 3)

# win.mainloop()
# a = Protocol.base64Encode(struct.pack(">BHHI", 0, 1, 1, 13))

# a = Protocol.base64Encode(struct.pack(">BHHI", 1, 40000, 12, 3000000000))
# print(a)

# a = 65536
# print(65536 % 65536)

# import base64
# base64_bytes_str = base64.standard_b64encode("ray".encode())
# print("ray".encode())
# print(base64_bytes_str.decode())
# print(Protocol.addHead(0, 0, b"aaa"))

# import os
# file_path = "D:/test/test.py"
# _,tempfilename = os.path.split(file_path)
# (filename,extension) = os.path.splitext(tempfilename)
# print(_)
# print(tempfilename)
# print(filename)
# print(extension)


# from queue import Queue
# import threading

# q = Queue(2)

# threading.Thread(target=)

# q.put(1)
# q.put(2)
# q.put(3)