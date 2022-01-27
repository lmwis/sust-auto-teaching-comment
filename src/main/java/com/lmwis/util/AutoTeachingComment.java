package com.lmwis.util;

import cn.hutool.http.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 教务系统自动评教
 * @Author: lmwis
 * @Date 2021-01-04 21:24
 * @Version 1.0
 */
public class AutoTeachingComment {
    // sust host
    private static final String HOST="http://bkjw.sust.edu.cn";
    // login url
    private static final String LOGIN_CAS_URL="http://login.sust.edu.cn/cas/login?service=http%3A%2F%2Fbkjw.sust.edu.cn%3A80%2Feams%2Fsso%2Flogin.action%3FtargetUrl%3Dbase64aHR0cDovL2Jrancuc3VzdC5lZHUuY246ODAvZWFtcy9ob21lLmFjdGlvbg%3D%3D";
    // 两个请求都可以，执行一个就行
//    private static final String PRE_COURSE_INFO_URL_1=HOST+"/eams/studentCourseTable.action?_=1606800721378";
    //    private static final String PRE_COURSE_INFO_URL_2=HOST+"/eams/dataQuery.action";
    /**
     * 评教列表
     */
    private static final String COMMENT_LIST_URL=HOST+"/eams/quality/stdEvaluate.action?_=1609766969972";
    /**
     * 评教提交url
     */
    private static final String COMMENT_SUBMIT_URL = HOST+"/eams/quality/stdEvaluate!finishAnswer.action";
    /**
     * 登录固定参数
     */
    private static final String KEY_1="currentMenu";
    private static final String KEY_2="_eventId";
    private static final String GEOLOCATION_KEY="geolocation";
    private static final String SUBMIT_KEY="submit";
    private static final String VALUE_1="1";
    private static final String VALUE_2="submit";
    private static final String GEOLOCATION="";
    private static final String SUBMIT="%E7%A8%8D%E7%AD%89%E7%89%87%E5%88%BB%E2%80%A6%E2%80%A6";
    // 从页面获取
    private String execution;
    private static final String EXECUTION_KEY = "execution";
    // 页面解析 execution 正则
    private static final String executionRegex = "(?<=<input type=\"hidden\" name=\"execution\" value=\").*?(?=\")";
    /**
     * 解析课程id+教师id
     */
    private static final String evaluationLessonRegex = "(?<=evaluationLesson.id=).*?(?=\")";
    /**
     * 新版教务系统账号 =>学号
     */
    private String username;
    /**
     * 新版教务系统密码
     */
    private String password;

    public void doComment(String username,String password){
        this.username = username;
        this.password = password;

        // 1.第一次请求登录页，从页面隐藏中解析出execution值-->值恒为"e1s1"
        HttpUtil.get(LOGIN_CAS_URL);
        // 正则匹配
//         Pattern pattern = Pattern.compile(executionRegex, Pattern.DOTALL);
//         Matcher matcher = pattern.matcher(result1);
//         if (matcher.find()) {
//             execution= matcher.group();
//         }
        execution="e1s1";
        /*可以不用去解离信息，在get完成之后execution即有效*/
        // 参数封装，发送请求
        Map<String, Object> params = packageLoginParams();
        // 2.第二次请求登录页，执行登录，获取Location请求头和TGC
        // 登录成功状态码为302，失败为401
        HttpResponse execute = HttpRequest.post(LOGIN_CAS_URL).form(params).setFollowRedirects(false).execute();
        if(execute.getStatus()== 401){
            // 登录失败
//            throw new BusinessException(EmCourseExceptError.SUST_JWC_LOGIN_FAIL);
            System.out.println("登录失败，账号或密码错误");
            return ;
        }else if(execute.getStatus()!=302){
            // 不为302未知错误
            System.out.println("登录失败:不为302");
            return ;
        }
        System.out.println("sust教务系统登录成功，用户:"+username);
        String location = execute.header(Header.LOCATION);
        // TGC会自动设置
//        String tgc = execute.getCookie("TGC").getValue();
//        logger.info(tgc);
        // 3.Get访问Location,获取JSESSIONID，JSESSIONID会自动加入cookie中
        HttpRequest.get(location).execute();
        // 4.获取评教页面列表
        String commentListPage = HttpRequest.post(COMMENT_LIST_URL).execute().body();
//        System.out.println(commentListPage);
        List<String> strings = execRegxGroups(commentListPage, evaluationLessonRegex);
        if (strings.size()==0){
            System.out.println("不存在需要评教的课程");
        }else{
            System.out.println("未完成课程评教数量："+strings.size());
        }
        for(String str : strings){
            String evaluationLessonId = str.substring(0,str.indexOf("&"));
            String teacherId = str.substring(str.indexOf("=")+1);
            // 发送请求进行评教
            HttpResponse doComment = HttpRequest.post(COMMENT_SUBMIT_URL)
                    .body(packageCommentSubmitParams(teacherId, evaluationLessonId))
                    .execute();

            System.out.print("课程id:"+evaluationLessonId+"\t教师id:"+teacherId);
            System.out.print("\t请求状态"+doComment.getStatus()+"\t");
            if(doComment.getStatus()!=302){
                System.out.println("评教失败");
            }else{
                System.out.println("评教成功");
            }

        }
        System.out.println("评教结束");


    }

    private String packageCommentSubmitParams(String teacherId, String evaluationLessonId) {
        return "teacher.id="+teacherId
                +"&semester.id=122&evaluationLesson.id="
                +evaluationLessonId
                +"&result1_0.questionName=%E6%95%99%E5%B8%88%E8%B4%A3%E4%BB%BB%E5%BF%83%E5%BC%BA%EF%BC%8C%E5%AF%B9%E5%AD%A6%E7%94%9F%E6%9C%89%E7%88%B1%E5%BF%83%EF%BC%8C%E8%80%90%E5%BF%83%E3%80%82%EF%BC%884%E5%88%86%EF%BC%89%EF%BC%89&result1_0.questionType=%E6%95%99%E5%AD%A6%E6%80%81%E5%BA%A6&result1_0.content=D+4&result1_0.score=4&result1_1.questionName=%E6%95%99%E5%B8%88%E6%B2%BB%E5%AD%A6%E4%B8%A5%E8%B0%A8%EF%BC%8C%E8%AE%A4%E7%9C%9F%E8%B4%9F%E8%B4%A3%E7%B2%BE%E7%A5%9E%E9%A5%B1%E6%BB%A1%EF%BC%8C%E8%AE%B2%E8%AF%BE%E6%9C%89%E7%83%AD%E6%83%85%E3%80%82%EF%BC%884%E5%88%86%EF%BC%89&result1_1.questionType=%E6%95%99%E5%AD%A6%E6%80%81%E5%BA%A6&result1_1.content=D+4&result1_1.score=4&result1_2.questionName=%E6%97%A2%E6%95%99%E4%B9%A6%E5%8F%88%E8%82%B2%E4%BA%BA%EF%BC%8C%E6%B3%A8%E6%84%8F%E5%AF%B9%E5%AD%A6%E7%94%9F%E7%9A%84%E6%95%99%E8%82%B2%E5%BC%95%E5%AF%BC%EF%BC%8C%E4%BB%A5%E8%BA%AB%E4%BD%9C%E5%88%99%E3%80%82%EF%BC%884%E5%88%86%EF%BC%89&result1_2.questionType=%E6%95%99%E5%AD%A6%E6%80%81%E5%BA%A6&result1_2.content=D+4&result1_2.score=4&result1_3.questionName=%E6%95%99%E5%B8%88%E9%81%B5%E5%AE%88%E6%95%99%E5%AD%A6%E7%BA%AA%E5%BE%8B%EF%BC%8C%E6%97%A0%E8%BF%9F%E5%88%B0%E3%80%81%E6%8F%90%E5%89%8D%E4%B8%8B%E8%AF%BE%E3%80%81%E9%9A%8F%E6%84%8F%E8%B0%83%E5%81%9C%E8%AF%BE%E6%83%85%E5%86%B5%E3%80%82%EF%BC%883%E5%88%86%EF%BC%89&result1_3.questionType=%E6%95%99%E5%AD%A6%E6%80%81%E5%BA%A6&result1_3.content=C+3&result1_3.score=3&result1_4.questionName=%E5%A4%87%E8%AF%BE%E8%AE%A4%E7%9C%9F%EF%BC%8C%E6%95%99%E5%AD%A6%E7%9B%AE%E6%A0%87%E6%98%8E%E7%A1%AE%EF%BC%8C%E6%95%99%E5%AD%A6%E5%86%85%E5%AE%B9%E5%AE%8C%E5%A4%87%E3%80%81%E5%85%85%E5%AE%9E%E3%80%81%E6%97%A0%E7%A7%91%E5%AD%A6%E6%80%A7%E3%80%81%E6%94%BF%E7%AD%96%E6%80%A7%E9%94%99%E8%AF%AF%E3%80%82%E5%88%A9%E7%94%A8%E4%BC%98%E8%B4%A8%E6%95%99%E5%AD%A6%E8%B5%84%E6%BA%90%EF%BC%8C%E9%80%82%E7%94%A8%E6%80%A7%E5%BC%BA%E3%80%82%EF%BC%887%E5%88%86%EF%BC%89&result1_4.questionType=%E6%95%99%E5%AD%A6%E5%86%85%E5%AE%B9&result1_4.content=G+7&result1_4.score=7.000000000000001&result1_5.questionName=%E5%9D%9A%E6%8C%81%E7%AB%8B%E5%BE%B7%E6%A0%91%E4%BA%BA%EF%BC%8C%E8%AF%BE%E7%A8%8B%E8%9E%8D%E5%85%A5%E6%80%9D%E6%83%B3%E6%94%BF%E6%B2%BB%E6%95%99%E8%82%B2%E5%85%83%E7%B4%A0%EF%BC%88%E6%95%99%E5%B8%88%E6%8C%96%E6%8E%98%E5%90%84%E9%97%A8%E4%B8%93%E4%B8%9A%E8%AF%BE%E7%A8%8B%E6%89%80%E8%95%B4%E5%90%AB%E7%9A%84%E6%80%9D%E6%83%B3%E6%94%BF%E6%B2%BB%E6%95%99%E8%82%B2%E5%85%83%E7%B4%A0%EF%BC%8C%E6%8A%8A%E5%81%9A%E4%BA%BA%E5%81%9A%E4%BA%8B%E7%9A%84%E5%9F%BA%E6%9C%AC%E9%81%93%E7%90%86%E3%80%81%E7%A4%BE%E4%BC%9A%E4%B8%BB%E4%B9%89%E6%A0%B8%E5%BF%83%E4%BB%B7%E5%80%BC%E8%A7%82%E7%9A%84%E8%A6%81%E6%B1%82%E3%80%81%E5%AE%9E%E7%8E%B0%E6%B0%91%E6%97%8F%E5%A4%8D%E5%85%B4%E7%9A%84%E7%90%86%E6%83%B3%E5%92%8C%E8%B4%A3%E4%BB%BB%E8%9E%8D%E5%85%A5%E4%B8%93%E4%B8%9A%E8%AF%BE%E7%A8%8B%E6%95%99%E5%AD%A6%E4%B8%AD%EF%BC%89%E3%80%82%EF%BC%887%E5%88%86%EF%BC%89&result1_5.questionType=%E6%95%99%E5%AD%A6%E5%86%85%E5%AE%B9&result1_5.content=G+7&result1_5.score=7.000000000000001&result1_6.questionName=%E5%AF%B9%E6%A0%87%E9%AB%98%E9%98%B6%E6%80%A7%E3%80%81%E5%88%9B%E6%96%B0%E6%80%A7%E5%92%8C%E6%8C%91%E6%88%98%E5%BA%A6%E8%A6%81%E6%B1%82%EF%BC%8C%E6%8E%A8%E5%8A%A8%E9%87%91%E8%AF%BE%E5%BB%BA%E8%AE%BE%E4%B9%8B%E9%AB%98%E9%98%B6%E6%80%A7%E6%8C%87%E8%83%BD%E5%A4%9F%E6%8A%8A%E7%9F%A5%E8%AF%86%E3%80%81%E8%83%BD%E5%8A%9B%E3%80%81%E7%B4%A0%E8%B4%A8%E6%9C%89%E6%9C%BA%E8%9E%8D%E5%90%88%EF%BC%8C%E5%9F%B9%E5%85%BB%E8%A7%A3%E5%86%B3%E5%A4%8D%E6%9D%82%E9%97%AE%E9%A2%98%E7%9A%84%E7%BB%BC%E5%90%88%E8%83%BD%E5%8A%9B%E5%92%8C%E9%AB%98%E7%BA%A7%E6%80%9D%E7%BB%B4%E3%80%82%EF%BC%887%E5%88%86%EF%BC%89&result1_6.questionType=%E6%95%99%E5%AD%A6%E5%86%85%E5%AE%B9&result1_6.content=G+7&result1_6.score=7.000000000000001&result1_7.questionName=%E5%AF%B9%E6%A0%87%E9%AB%98%E9%98%B6%E6%80%A7%E3%80%81%E5%88%9B%E6%96%B0%E6%80%A7%E5%92%8C%E6%8C%91%E6%88%98%E5%BA%A6%E8%A6%81%E6%B1%82%EF%BC%8C%E6%8E%A8%E5%8A%A8%E9%87%91%E8%AF%BE%E5%BB%BA%E8%AE%BE%E4%B9%8B%E5%88%9B%E6%96%B0%E6%80%A7%E6%98%AF%E6%8C%87%E8%AF%BE%E7%A8%8B%E5%86%85%E5%AE%B9%E8%A6%81%E5%8F%8D%E6%98%A0%E5%89%8D%E6%B2%BF%E6%80%A7%E5%92%8C%E6%97%B6%E4%BB%A3%E6%80%A7%EF%BC%8C%E6%95%99%E5%AD%A6%E5%BD%A2%E5%BC%8F%E5%91%88%E7%8E%B0%E5%85%88%E8%BF%9B%E6%80%A7%E5%92%8C%E4%BA%92%E5%8A%A8%E6%80%A7%EF%BC%8C%E5%AD%A6%E4%B9%A0%E7%BB%93%E6%9E%9C%E5%85%B7%E6%9C%89%E6%8E%A2%E7%A9%B6%E6%80%A7%E5%92%8C%E4%B8%AA%E6%80%A7%E5%8C%96%E3%80%82%EF%BC%887%E5%88%86%EF%BC%89&result1_7.questionType=%E6%95%99%E5%AD%A6%E5%86%85%E5%AE%B9&result1_7.content=G+7&result1_7.score=7.000000000000001&result1_8.questionName=%E5%AF%B9%E6%A0%87%E9%AB%98%E9%98%B6%E6%80%A7%E3%80%81%E5%88%9B%E6%96%B0%E6%80%A7%E5%92%8C%E6%8C%91%E6%88%98%E5%BA%A6%E8%A6%81%E6%B1%82%EF%BC%8C%E6%8E%A8%E5%8A%A8%E9%87%91%E8%AF%BE%E5%BB%BA%E8%AE%BE%E4%B9%8B%E6%8C%91%E6%88%98%E5%BA%A6%E6%98%AF%E6%8C%87%E8%AF%BE%E7%A8%8B%E6%9C%89%E4%B8%80%E5%AE%9A%E9%9A%BE%E5%BA%A6%EF%BC%8C%E9%9C%80%E8%A6%81%E8%B7%B3%E4%B8%80%E8%B7%B3%E6%89%8D%E8%83%BD%E5%A4%9F%E5%BE%97%E7%9D%80%EF%BC%8C%E8%80%81%E5%B8%88%E5%A4%87%E8%AF%BE%E5%92%8C%E5%AD%A6%E7%94%9F%E8%AF%BE%E4%B8%8B%E6%9C%89%E8%BE%83%E9%AB%98%E8%A6%81%E6%B1%82%E3%80%82%EF%BC%887%E5%88%86%EF%BC%89&result1_8.questionType=%E6%95%99%E5%AD%A6%E5%86%85%E5%AE%B9&result1_8.content=G+7&result1_8.score=7.000000000000001&result1_9.questionName=%E7%90%86%E8%AE%BA%E8%81%94%E7%B3%BB%E5%AE%9E%E9%99%85%EF%BC%8C%E8%83%BD%E7%BB%93%E5%90%88%E5%AD%A6%E7%94%9F%E3%80%81%E7%A4%BE%E4%BC%9A%E9%9C%80%E6%B1%82%E7%9A%84%E5%AE%9E%E9%99%85%E7%BB%84%E7%BB%87%E6%95%99%E5%AD%A6%EF%BC%8C%E5%96%84%E4%BA%8E%E5%90%AF%E5%8F%91%E6%80%9D%E7%BB%B4%E3%80%82%EF%BC%885%E5%88%86%EF%BC%89&result1_9.questionType=%E6%95%99%E5%AD%A6%E6%96%B9%E6%B3%95%E6%89%8B%E6%AE%B5&result1_9.content=E+5&result1_9.score=5&result1_10.questionName=%E6%95%99%E5%AD%A6%E8%AE%BE%E8%AE%A1%E5%90%88%E7%90%86%EF%BC%8C%E6%9D%BF%E4%B9%A6%E8%A7%84%E8%8C%83%E3%80%81%E5%B8%83%E5%B1%80%E6%9C%89%E8%A7%84%E5%88%92%EF%BC%8C%E5%A4%9A%E5%AA%92%E4%BD%93%E5%92%8C%E6%9D%BF%E4%B9%A6%E8%9E%8D%E5%90%88%E8%87%AA%E7%84%B6%E3%80%81%E4%BA%92%E4%B8%BA%E6%9C%89%E7%9B%8A%E8%A1%A5%E5%85%85%E3%80%82%E6%9C%89%E6%95%88%E5%88%A9%E7%94%A8%E4%BF%A1%E6%81%AF%E5%8C%96%E6%89%8B%E6%AE%B5%EF%BC%8C%E5%8A%A0%E5%A4%A7%E8%AF%BE%E7%A8%8B%E6%94%B9%E9%9D%A9%E5%88%9B%E6%96%B0%E3%80%82%EF%BC%885%E5%88%86%EF%BC%89&result1_10.questionType=%E6%95%99%E5%AD%A6%E6%96%B9%E6%B3%95%E6%89%8B%E6%AE%B5&result1_10.content=E+5&result1_10.score=5&result1_11.questionName=%E5%88%9B%E6%96%B0%E6%96%B9%E5%BC%8F%E6%96%B9%E6%B3%95%E9%80%89%E6%8B%A9%E6%81%B0%E5%BD%93%EF%BC%8C%E5%88%9D%E6%AD%A5%E5%BD%A2%E6%88%90%E4%B8%80%E5%AE%9A%E7%9A%84%E5%88%9B%E6%96%B0%E5%9E%8B%E6%95%99%E8%82%B2%E6%A8%A1%E5%BC%8F%EF%BC%8C%E5%AE%9E%E7%8E%B0%E4%BA%86%E2%80%9C%E4%BB%A5%E5%AD%A6%E7%94%9F%E4%B8%BA%E4%B8%AD%E5%BF%83%EF%BC%8C%E4%BA%A7%E5%87%BA%E4%B8%BA%E5%AF%BC%E5%90%91%EF%BC%8C%E6%8C%81%E7%BB%AD%E6%94%B9%E8%BF%9B%E2%80%9D%E6%95%99%E5%AD%A6%E7%90%86%E5%BF%B5%E3%80%82%EF%BC%8810%E5%88%86%EF%BC%89&result1_11.questionType=%E6%95%99%E5%AD%A6%E6%96%B9%E6%B3%95%E6%89%8B%E6%AE%B5&result1_11.content=J+10&result1_11.score=10&result1_12.questionName=%E8%AF%BE%E5%A0%82%E8%AE%B2%E6%8E%88%E5%AF%8C%E6%9C%89%E5%90%B8%E5%BC%95%E5%8A%9B%EF%BC%8C%E5%AD%A6%E7%94%9F%E6%80%9D%E7%BB%B4%E6%B4%BB%E8%B7%83%EF%BC%8C%E5%B8%88%E7%94%9F%E4%BA%92%E5%8A%A8%E5%85%85%E5%88%86%EF%BC%8C%E6%8E%A2%E7%A9%B6%E6%9C%89%E6%B7%B1%E5%BA%A6%E3%80%82%EF%BC%885%E5%88%86%EF%BC%89&result1_12.questionType=%E6%95%99%E5%AD%A6%E6%96%B9%E6%B3%95%E6%89%8B%E6%AE%B5&result1_12.content=E+5&result1_12.score=5&result1_13.questionName=%E8%AF%BE%E5%A0%82%E6%95%99%E5%AD%A6%E7%BB%84%E7%BB%87%E4%B8%A5%E5%AF%86%EF%BC%8C%E6%97%B6%E9%97%B4%E5%AE%89%E6%8E%92%E5%90%88%E7%90%86%EF%BC%8C%E5%BC%A0%E5%BC%9B%E5%BE%97%E5%BD%93%E3%80%82%EF%BC%884%E5%88%86%EF%BC%89&result1_13.questionType=%E6%95%99%E5%AD%A6%E7%AE%A1%E7%90%86&result1_13.content=D+4&result1_13.score=4&result1_14.questionName=%E6%95%A2%E4%BA%8E%E7%AE%A1%E7%90%86%EF%BC%8C%E8%AF%BE%E5%A0%82%E7%BA%AA%E5%BE%8B%E5%A5%BD%E3%80%82%EF%BC%883%E5%88%86%EF%BC%89&result1_14.questionType=%E6%95%99%E5%AD%A6%E7%AE%A1%E7%90%86&result1_14.content=C+3&result1_14.score=3&result1_15.questionName=%E5%96%84%E4%BA%8E%E7%AE%A1%E7%90%86%EF%BC%8C%E5%B8%88%E7%94%9F%E5%85%B3%E7%B3%BB%E5%92%8C%E8%B0%90%E3%80%82%EF%BC%883%E5%88%86%EF%BC%89&result1_15.questionType=%E6%95%99%E5%AD%A6%E7%AE%A1%E7%90%86&result1_15.content=C+3&result1_15.score=3&result1_16.questionName=%E5%AE%8C%E6%88%90%E6%95%99%E5%AD%A6%E8%AE%A1%E5%88%92%EF%BC%8C%E5%A4%9A%E6%95%B0%E5%AD%A6%E7%94%9F%E8%83%BD%E5%A4%9F%E6%8E%A5%E5%8F%97%E5%B9%B6%E6%8E%8C%E6%8F%A1%E8%AF%BE%E7%A8%8B%E7%9A%84%E4%B8%BB%E8%A6%81%E5%86%85%E5%AE%B9%E3%80%82%EF%BC%885%E5%88%86%EF%BC%89&result1_16.questionType=%E6%95%99%E5%AD%A6%E6%95%88%E6%9E%9C&result1_16.content=E+5&result1_16.score=5&result1_17.questionName=%E5%AD%A6%E7%94%9F%E8%83%BD%E5%88%9D%E6%AD%A5%E8%BF%90%E7%94%A8%E6%89%80%E5%AD%A6%E7%9F%A5%E8%AF%86%E8%A7%A3%E5%86%B3%E5%AE%9E%E9%99%85%E9%97%AE%E9%A2%98%E3%80%82%EF%BC%883%E5%88%86%EF%BC%89&result1_17.questionType=%E6%95%99%E5%AD%A6%E6%95%88%E6%9E%9C&result1_17.content=C+3&result1_17.score=3&result1_18.questionName=%E5%AD%A6%E7%94%9F%E5%AD%A6%E4%B9%A0%E8%83%BD%E5%8A%9B%E3%80%81%E5%8A%A8%E6%89%8B%E8%83%BD%E5%8A%9B%E3%80%81%E5%88%9B%E6%96%B0%E8%83%BD%E5%8A%9B%E5%92%8C%E7%BB%BC%E5%90%88%E7%B4%A0%E8%B4%A8%E6%9C%89%E6%8F%90%E9%AB%98%E3%80%82%EF%BC%882%E5%88%86%EF%BC%89&result1_18.questionType=%E6%95%99%E5%AD%A6%E6%95%88%E6%9E%9C&result1_18.content=B+2&result1_18.score=2&result1_19.questionName=%E8%BF%9B%E8%A1%8C%E5%85%A8%E6%96%B9%E4%BD%8D%E3%80%81%E5%A4%9A%E5%BD%A2%E5%BC%8F%E3%80%81%E5%88%86%E9%98%B6%E6%AE%B5%E8%80%83%E6%A0%B8%E6%96%B9%E5%BC%8F%EF%BC%9B%E8%80%83%E6%A0%B8%E8%AF%84%E4%BB%B7%E5%B5%8C%E5%85%A5%E6%95%99%E5%AD%A6%E5%85%A8%E8%BF%87%E7%A8%8B%E3%80%82%EF%BC%885%E5%88%86%EF%BC%89&result1_19.questionType=%E6%95%99%E5%AD%A6%E6%95%88%E6%9E%9C&result1_19.content=E+5&result1_19.score=5&result2_0.questionName=%E6%82%A8%E5%AF%B9%E8%AF%A5%E6%95%99%E5%B8%88%E6%9C%89%E4%BD%95%E5%BB%BA%E8%AE%AE&result2_0.questionType=%E8%AF%BE%E7%A8%8B%E5%BB%BA%E8%AE%AE&result2_0.content=%E6%97%A0&result2_1.questionName=%E6%82%A8%E5%AF%B9%E6%9C%AC%E9%97%A8%E8%AF%BE%E7%A8%8B%E7%9A%84%E5%BC%80%E8%AE%BE%E6%9C%89%E4%BD%95%E5%BB%BA%E8%AE%AE%EF%BC%9F&result2_1.questionType=%E8%AF%BE%E7%A8%8B%E5%BB%BA%E8%AE%AE&result2_1.content=%E6%97%A0&result1Num=20&result2Num=2";
    }

    /**
     * 封装login的post请求参数
     * @return params
     */
    private Map<String, Object> packageLoginParams() {
        Map<String,Object> params = new HashMap<>();
        params.put("username",username);
        params.put("password",password);
        params.put(KEY_1,VALUE_1);
        params.put(KEY_2,VALUE_2);
        params.put(GEOLOCATION_KEY,GEOLOCATION);
        params.put(SUBMIT_KEY,SUBMIT);
        params.put(EXECUTION_KEY,execution);
        return params;
    }

    /**
     * 获取匹配到的第一个
     * @param content
     * @param regx
     * @return
     */
    private String execRegxGroup0(String content,String regx){
        String res="";
        // 正则匹配
        Matcher matcher = Pattern.compile(regx, Pattern.DOTALL).matcher(content);
        if (matcher.find()) {
            res= matcher.group();
        }
        return res;
    }

    /**
     * 获取所有匹配分组
     * @param content
     * @param regx
     * @return
     */
    private List<String> execRegxGroups(String content, String regx){
        List<String> lists = new ArrayList<>();
        // 正则匹配
        Matcher matcher = Pattern.compile(regx, Pattern.DOTALL).matcher(content);
        while(matcher.find()){
            lists.add(matcher.group());
        }
        return lists;
    }
}
