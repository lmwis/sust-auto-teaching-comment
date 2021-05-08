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
        AutoTeachingComment autoTeachingComment = new AutoTeachingComment();
        autoTeachingComment.doComment("username","password");
    }
}
