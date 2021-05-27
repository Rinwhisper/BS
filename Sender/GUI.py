'''
Author       : Rinwhisper
Date         : 2021-03-03 18:53:35
LastEditTime : 2021-03-06 18:42:40
LastEditors  : Rinwhisper
FilePath     : \毕设代码\GUI.py
Description  : 
'''
import os
import tkinter.messagebox
import tkinter.filedialog
import tkinter as tk 
from tkinter import ttk
import tkinter.font as tf


import queue
from Protocol import Protocol
import hashlib
import threading
from PIL import ImageTk
from PIL import Image

class GUI:
    '''
    GUI 类不是一个新的 wid_get ， 它是一个代掌 tk.Tk() 窗口的类， 创建此类的目的是便于对 tk.Tk() 窗口进行操作\n
    root 为此类代掌的窗口，默认为 tk.Tk()\n

    窗口结构：\n
    root
        first_frame
            label_f_1 提示
            label_f_2 提示信息
            button_f 选择文件按钮
        second_frame
            label_s_1 提示
            label_s_2 显示选择的文件
            button_s_1 退回 first_frame 重新选择文件
            button_s_2 确认文件，前往 third_frame 传输文件
        third_frame
            label_t_1 提示
            label_t_2 现实选择的文件
            button_t_1 继续按钮，显示下一张二维码
            button_t_2 完成按钮，传输完毕时显示，回到 first_frame
    '''

#####################################
#   main COMMAND to filethread
    # 标志退出filethread线程
    COMMAND_QUIT = 3

    # 发送文件
    COMMAND_SEND = 4


#####################################
#   packet type flag
    # 标志普通数据包
    FLAG_DATA = 0

    # 标志 md5 校验包
    FLAG_MD5 = 1

    # 标志文件名包
    FLAG_FILENAME = 2

#####################################
#   filethread 通知 main 文件发送完毕
    DATA_FINISH = 5

    def __init__(self, root = None):
        self.root = root or tk.Tk()
        self.root.protocol("WM_DELETE_WINDOW", self.onWindowClose)
        # self.root.resizable(False, False)

#       控制文件读写的锁和变量
        self.lock = threading.Lock()
        self.stop = False


        # 线程之间命令 queue
        # main -> filethread
        self.command_queue = queue.Queue()

        # 线程之间数据 queue
        # filethread -> main
        self.data_queue = queue.Queue(5)

        # 线程之间原始数据 queue
        # filethread -> main
        self.raw_data_queue = queue.Queue(5)



        # filethread 打开的文件指针
        self.fr = None

        # 当前 id
        self.id = 0


        # 每次从文件中读出的 size
        self.read_size = 1000


        # self.image 的作用是始终保持指向 third_frame 中 canvas 里的图像，满足第二个要求
        # canvas 里的图像有两个要求
        # 一是格式，必须为 PhotoImage or BitmapImage 格式
        # 二是要保持持续引用
        self.previous_image_obj = None
        self.current_image_obj = None

        self.previous_raw_image_obj = None
        self.current_raw_image_obj = None

        self.image = None

        # md5对象，用于计算整个文件的md5
        self.md5 = hashlib.md5()

        self.canvas_size = 600

        self.setTitle()
        
        self.root.resizable(False, False)

        # self.fullScreen()
        self.createFirstFrame()
        self.setFirstFrameSize()

        # 启动 处理数据 的生产者线程
        threading.Thread(target = self.produceDataThread).start()





    def setTitle(self, message: str = "基于屏幕二维码的单向文件传输系统"):
        self.root.title(message)

    def setSize(self, length: int, width: int):
        self.root.geometry(str(length) + "x" + str(width))
    
    def setFirstFrameSize(self, length = 500, width = 400):
        self.root.attributes('-fullscreen', False)
        self.setSize(length, width)

    def setSecondFrameSize(self, length = 790, width = 400):
        self.root.attributes('-fullscreen', False)
        self.setSize(length, width)

    def mainloop(self):
        self.root.mainloop()

    def choiceFileThenSecondFrame(self):
        """
        选择文件，返回文件路径
        """
        filepath = tkinter.filedialog.askopenfilename(title = "选择将要传输的文件")
        if filepath == "":
            # 点了取消
            tk.messagebox.showinfo(title = "提示", message = "请选择一个文件！")
            return
        self.hideFirstFrame()
        self.recoverSecondFrame(filepath)
        
    def secondFrameBackToFirstFrame(self):
        self.setFirstFrameSize()
        self.hideSecondFrame()
        self.recoverFirstFrame()
        
    def secondFrameToThirdFrame(self):
        filepath = self.label_s_2.cget("text")

        self.fullScreen()

        # 首先清除 两个 queue
        self.clearQueue(self.raw_data_queue)
        self.clearQueue(self.data_queue)

        # 确定传输文件，向 filethread 发布命令
        self.sendCommand(GUI.COMMAND_SEND, filepath)

        self.hideSecondFrame()
        self.recoverThirdFrame()
        
    def thirdFrameToFirstFrame(self):
        self.hideThirdFrame()
        self.recoverFirstFrame()
        self.setFirstFrameSize()


    # 关闭窗口时停止 filethread 线程
    def onWindowClose(self):
        self.sendCommand(GUI.COMMAND_QUIT, None)
        self.root.destroy()


    def sendCommand(self, flag, data):
        self.command_queue.put({
            "what": flag,
            "obj": data
        })




    def produceDataThread(self):
        while(True):
            command = self.command_queue.get()
            if(command["what"] == GUI.COMMAND_QUIT):
                return
            elif(command["what"]  == GUI.COMMAND_SEND):
                if(self.fr is None):
                    # 发送 filename

                    # filename 是 str
                    _, filename = os.path.split(command["obj"])
                    self.raw_data_queue.put({
                        "flag": GUI.FLAG_FILENAME,
                        "id": self.id,
                        "data": filename.encode("utf-8")
                    })
                    self.data_queue.put({
                        "flag": GUI.FLAG_FILENAME,
                        "id": self.id,
                        "data": Protocol.produce(GUI.FLAG_FILENAME, self.id, filename.encode("utf-8"))
                    })
                    self.updateId()

                    # 发送 data
                    for data in self.readFile(command["obj"]):
                        self.md5.update(data)
                        self.raw_data_queue.put({
                            "flag": GUI.FLAG_DATA,
                            "id": self.id,
                            "data": data
                        })
                        self.data_queue.put({
                            "flag": GUI.FLAG_DATA,
                            "id": self.id,
                            "data": Protocol.produce(GUI.FLAG_DATA, self.id, data)
                        })
                        self.updateId()
                    
                    # 发送 md5 
                    md5 = self.md5.digest()
                    self.raw_data_queue.put({
                        "flag": GUI.FLAG_MD5,
                        "id": self.id,
                        "data": md5
                    })
                    self.data_queue.put({
                        "flag": GUI.FLAG_MD5,
                        "id": self.id,
                        "data": Protocol.produce(GUI.FLAG_MD5, self.id, md5)
                    })

                    # 通知 main 文件发送完毕
                    self.raw_data_queue.put(GUI.DATA_FINISH)
                    self.data_queue.put(GUI.DATA_FINISH)

                    # 重置状态
                    self.resetFileThreadState()

                else:
                    raise Exception("收到 SEND 命令，但 fr 不为空")

    def readFile(self, filepath: str):
        # 迭代器
        self.fr = open(filepath, "rb")
        while True:
            data = self.fr.read(self.read_size)
            self.lock.acquire()
            if (not self.stop) and data:
                self.lock.release()
                yield data
            else:
                self.lock.release()
                self.fr.close()
                self.fr = None
                return


    # 更新 next_id
    def updateId(self):
        self.id = (self.id + 1) % 65536
        if(self.id == 0):
            # 跳过 id 0
            # 因为 id = 0 标志着创建文件
            self.id = 1
        
    



    # 重置 filethread 状态
    def resetFileThreadState(self):
        self.id = 0
        self.md5 = hashlib.md5()

        self.lock.acquire()
        self.stop = False
        self.lock.release()

        

    def existSecondFrame(self) -> bool:
        if hasattr(self, "second_frame"):
            return True
        else:
            return False
    
    def existThirdFrame(self) -> bool:
        if hasattr(self, "third_frame"):
            return True
        else:
            return False
    
    def createFirstFrame(self):
        self.first_frame = tk.Frame(self.root, width = 500, height = 400)
        self.label_f_1 = ttk.Label(self.first_frame, text = "提示：", font = tf.Font(size=15, weight=tf.BOLD))
        self.label_f_2 = ttk.Label(self.first_frame, text = "在传输过程中，请不要退出，按照系统提示进行操作")
        self.button_f = ttk.Button(self.first_frame, text = "选择文件", command = self.choiceFileThenSecondFrame)
        self.label_f_1.grid(row = 0, column = 1, pady = 10, sticky = tk.W)
        self.label_f_2.grid(row = 1, column = 1, pady = 10)
        self.button_f.grid(row = 10, column = 1, pady = 80)
        self.first_frame.pack()

    def hideFirstFrame(self):
        self.first_frame.pack_forget()

    def recoverFirstFrame(self):
        self.first_frame.pack()

    def createSecondFrame(self, filepath: str):
        self.second_frame = tk.Frame(self.root)
        self.label_s_1 = ttk.Label(self.second_frame, text = "您选择的文件为：", font = tf.Font(size=15, weight=tf.BOLD))
        self.label_s_2 = ttk.Label(self.second_frame, text = filepath, width = 50, wraplength = 300, font = tf.Font(size=11))
        self.button_s_1 = ttk.Button(self.second_frame, text = "重新选择文件", command = self.secondFrameBackToFirstFrame)
        self.button_s_2 = ttk.Button(self.second_frame, text = "开始文件传输", command = self.secondFrameToThirdFrame)
        self.label_s_1.grid(row = 0, column = 1, pady = 20, sticky = tk.W)
        self.label_s_2.grid(row = 1, column = 1, pady = 10, ipadx = 0)
        self.button_s_1.grid(row = 2, column = 0, pady = 80, padx = 71)
        self.button_s_2.grid(row = 2, column = 2, pady = 80, padx=0, ipadx=0)
        self.second_frame.grid()

    def hideSecondFrame(self):
        self.second_frame.grid_forget()

    def recoverSecondFrame(self, filepath: str):
        if self.existSecondFrame():
            # second_frame 已经存在，直接 recover
            self.label_s_2.config(text = filepath)
            self.second_frame.grid()
        else:
            # second_frame 不存在，也就是说 second_frame 从来没被创建过，直接创建
            self.createSecondFrame(filepath)
        self.setSecondFrameSize()
        
            
    def createThirdFrame(self):
        self.third_frame = tk.Frame(self.root)
        self.label_t_1 = ttk.Label(self.third_frame, text = "当前 ID：", font = tf.Font(size=15, weight=tf.BOLD))
        self.label_t_2 = ttk.Label(self.third_frame, font = tf.Font(size=15))
        self.canvas = tk.Canvas(self.third_frame, width = self.canvas_size, height = self.canvas_size, bg = "white")
        # self.button_t_1 = ttk.Button(self.third_frame, text = "上一张", command = self.nextQRCode)
        self.button_t_2 = ttk.Button(self.third_frame, text = "下一张", command = self.nextQRCode)
        self.button_t_3 = ttk.Button(self.third_frame, text = "刷新", command = self.flushCurrentQRCode)
        self.button_t_4 = ttk.Button(self.third_frame, text = "终止传输", command = self.showDialog)
        self.button_t_5 = ttk.Button(self.third_frame, text = "完成", command = self.thirdFrameToFirstFrame)
        self.label_t_1.pack()
        self.label_t_2.pack()
        self.canvas.pack()
        self.button_t_2.pack()
        self.button_t_3.pack()
        self.button_t_4.pack()
    
        self.third_frame.pack()
        

    def hideThirdFrame(self):
        self.third_frame.pack_forget()
    
    def recoverThirdFrame(self):
        if self.existThirdFrame():
            # third_frame 已经存在，直接恢复

            # 隐藏完成按钮
            self.button_t_5.pack_forget() 
            # 显示继续按钮
            self.button_t_2.pack()
            self.button_t_3.pack()
            self.button_t_4.pack()

            self.third_frame.pack()

        else:
            # third_frame 不存在，创建
            self.createThirdFrame()
        self.fullScreen()
        self.nextQRCode()


    # def preQRCode(self):

    def showDialog(self):
        stop_send = tk.messagebox.askokcancel("提示", "确定终止传输吗？")
        if stop_send:
            self.stopSend()
            self.thirdFrameToFirstFrame()
        else:
            pass

    def fullScreen(self):
        # self.setSize(self.root.winfo_screenwidth(), self.root.winfo_screenheight())
        self.root.attributes('-fullscreen', True)

    # 中途退出传输文件
    def stopSend(self):
        self.lock.acquire()
        self.stop = True
        self.lock.release()

        self.clearQueue(self.raw_data_queue)
        self.clearQueue(self.data_queue)

        
    def clearQueue(self, q: queue.Queue):
        while(True):
            if(q.qsize() != 0):
                q.get()
            else:
                return


    def flushCurrentQRCode(self):
        new = Protocol.produce(self.current_raw_image_obj["flag"], self.current_raw_image_obj["id"], self.current_raw_image_obj["data"])
        self.image = ImageTk.PhotoImage(image = self.resize(new, (self.canvas_size, self.canvas_size)))
        self.canvas.create_image(self.canvas_size / 2, self.canvas_size / 2, image = self.image, anchor = "center")


    def nextQRCode(self):
        self.previous_image_obj = self.current_image_obj
        self.current_image_obj = self.data_queue.get()

        self.previous_raw_image_obj = self.current_raw_image_obj
        self.current_raw_image_obj = self.raw_data_queue.get()

        if self.current_image_obj == GUI.DATA_FINISH:
            self.image = None
            self.canvas.create_text(self.canvas_size / 2, self.canvas_size / 2, text = "传输完毕，请点击 完成 按钮返回")
            # 隐藏下一张，刷新，终止传输按钮
            self.button_t_2.pack_forget()
            self.button_t_3.pack_forget()
            self.button_t_4.pack_forget()

            # 显示完成按钮
            self.button_t_5.pack()
            
        else:
            # 保持引用
            self.label_t_2.configure(text = self.current_image_obj["id"])
            self.image = ImageTk.PhotoImage(image = self.resize(self.current_image_obj["data"] , (self.canvas_size, self.canvas_size)))
            self.canvas.create_image(self.canvas_size / 2, self.canvas_size / 2, image = self.image, anchor = "center")

    def resize(self, image, size):
        return image.resize(size, Image.ANTIALIAS)
