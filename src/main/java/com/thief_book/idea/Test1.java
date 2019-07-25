package com.thief_book.idea;

import java.io.IOException;
import java.io.RandomAccessFile;

public class Test1 {
    public static void main(String[] args) throws IOException {
        RandomAccessFile ra = null;
        String str = "";
        try {
//            ra = new RandomAccessFile("F:\\魔鬼搭讪学.txt", "r");


            str = new String(ra.readLine().getBytes("ISO-8859-1"), "gbk");
            //实例化当前行数
            System.out.println(str);


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ra.close();
        }
    }
}
