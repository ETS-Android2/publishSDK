package com.indoor.data.http;

import androidx.annotation.Keep;

/**
 * @author Aaron
 * @description:
 * @date : 2020/1/1 22:13
 */
@Keep
public class HttpStatus {
    /**
     * 返回成功
     */
    public static int STATUS_CODE_SUCESS = 200;


    /**
     * token过期，需要执行登录 old
     */
    public static int STATUS_CODE_TOKEN_OVERDUE_OLD = 401;

    /**
     * token到期，需要执行登录
     */
    public static int STATUS_CODE_TOKEN_OVERDUE = 901;

    /**
     * token失效，需要执行登录
     */
    public static int STATUS_CODE_TOKEN_NOUSE = 902;

    /**
     * token过期，需要执行登录
     */
    public static int STATUS_CODE_TOKEN_FORBIDDEN = 903;

    /**
     * 已被注册或验证码过期
     */
    public static int STATUS_CODE_OTHER_ERR = 500;

    public static boolean isTokenErr(int statusCode){
        return statusCode==HttpStatus.STATUS_CODE_OTHER_ERR||statusCode==HttpStatus.STATUS_CODE_TOKEN_NOUSE||statusCode==HttpStatus.STATUS_CODE_TOKEN_OVERDUE||
                statusCode==HttpStatus.STATUS_CODE_TOKEN_OVERDUE_OLD||statusCode==HttpStatus.STATUS_CODE_TOKEN_FORBIDDEN;
    }

}
