package com.example.Receiver;

//见 Sender Protocol
public class Tuple<A, B, C, D, E, F> {
    private A magic_number;
    private B flag;
    private C id;
    private D length;
    private E CRC32;
    private F data;

    public A getMagicNumber(){
        return magic_number;
    }

    public B getFlag() {
        return flag;
    }

    public C getId() {
        return id;
    }

    public D getLength() {
        return length;
    }

    public E getCRC32() {
        return CRC32;
    }

    public F getData() {
        return data;
    }

    public Tuple(A magic_number, B flag, C id, D length, E CRC32, F data){
        this.magic_number = magic_number;
        this.flag = flag;
        this.id = id;
        this.length = length;
        this.CRC32 = CRC32;
        this.data = data;
    }

    public void setData(A magic_number, B flag, C id, D length, E CRC32, F data){
        this.magic_number = magic_number;
        this.flag = flag;
        this.id = id;
        this.length = length;
        this.CRC32 = CRC32;
        this.data = data;
    }
}
