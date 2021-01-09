package com.lmwis.util;


import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @Description:
 * @Author: lmwis
 * @Date 2021-01-04 21:23
 * @Version 1.0
 */
public class Main {
    public static void main(String[] args) throws ParseException {
//        AutoTeachingComment autoTeachingComment = new AutoTeachingComment();
//        autoTeachingComment.doComment("username","password");
//        System.out.println(System.currentTimeMillis());
//        System.out.println(new Date().getTime());
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .parse("1970-1-1 00:00:01").getTime());
//        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//                .format(0));
        long time1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .parse("2020-1-1 00:00:00").getTime();
        long time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .parse("2021-1-1 00:00:00").getTime();
        System.out.println(time2-time1);
        System.out.println((time2-time1)/1000);
        System.out.println((time2-time1)/(1000*60));
        System.out.println((time2-time1)/(1000*60*24));
        System.out.println((time2-time1)/(1000*60*60*24));
    }
}
