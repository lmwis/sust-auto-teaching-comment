package com.lmwis.util;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

/**
 * @Description:
 * @Author: lmwis
 * @Date 2021-01-04 21:23
 * @Version 1.0
 */
public class Main {
    public static void main(String[] args) throws ParseException {
        Scanner input = new Scanner(System.in);
        System.out.println("输入sust教务处登陆账号(一般是学号):");
        String username = input.next();
        System.out.println("输入密码：");
        String password = input.next();
        AutoTeachingComment autoTeachingComment = new AutoTeachingComment();
        autoTeachingComment.doComment(username,password);
    }
}
