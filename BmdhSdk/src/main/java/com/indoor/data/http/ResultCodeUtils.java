package com.indoor.data.http;

import android.text.TextUtils;

public class ResultCodeUtils {

    /**
     * 鉴权失败
     *
     * @param code
     * @return
     */
    public static boolean isAuthorErr(String code){
        if(TextUtils.isEmpty(code)){
            return false;
        }
        return code.equals(RESULTCODE.VALIDATION_ERROR);
    }

    /**
     * Token失败
     *
     * @param code
     * @return
     */
    public static boolean isTokenErr(String code){
        if(TextUtils.isEmpty(code)){
            return false;
        }
        return code.startsWith("403");
    }

    /**
     * 操作请求是否成功
     *
     * @param code
     * @return
     */
    public static boolean isRequestOptionSuccess(String code){
        if(TextUtils.isEmpty(code)){
            return false;
        }
        return code.startsWith(RESULTCODE.SUCCESS);
    }

    public static String getHttpResultMsg(String code){

        String resultMsg="";
        switch (code) {
            case RESULTCODE.SUCCESS:
                resultMsg = "操作成功";
                break;
            case RESULTCODE.UPDATE_STATE_NO:
                resultMsg = "无可用更新";
                break;
            case RESULTCODE.SYSTEM_ERROR:
                resultMsg = "系统异常";
                break;
            case RESULTCODE.GATEWAY_ERROR:
                resultMsg = "服务器不可用";
                break;
            case RESULTCODE.SYSTEM_BUSY:
                resultMsg = "系统繁忙,请稍候再试";
                break;
            case RESULTCODE.VALIDATION_ERROR:
                resultMsg = "校验异常";
                break;
            case RESULTCODE.FORBIDDEN:
                resultMsg = "无权访问";
                break;
            case RESULTCODE.TOKEN_TIMEOUT:
                resultMsg = "token过期";
                break;
            case RESULTCODE.INVALID_TOKEN:
                resultMsg = "无效token";
                break;
            case RESULTCODE.UNAUTHORIZED_HEADER_IS_EMPTY:
                resultMsg = "无权访问,请求头为空";
                break;
            case RESULTCODE.GATEWAY_NOT_FOUND_SERVICE:
                resultMsg = "服务未找到";
                break;
            default:
                resultMsg = "网络错误";
                break;
        }
        return resultMsg;
    }


    /**
     * 约定异常 这个具体规则需要与服务端或者领导商讨定义
     */
    public class RESULTCODE {
        /**
         * 操作成功
         */
        public static final String SUCCESS = "200";
        /**
         * 无可用更新
         */
        public static final String UPDATE_STATE_NO = "200-1";

        /**
         * 系统异常
         */
        public static final String SYSTEM_ERROR = "500";
        /**
         * 网关异常
         */
        public static final String GATEWAY_ERROR = "500-1";

        /**
         * 系统繁忙,请稍候再试
         */
        public static final String SYSTEM_BUSY = "500-2";

        /**
         * 校验异常
         */
        public static final String VALIDATION_ERROR = "500-3";


        /**
         * 无权访问
         */
        public static final String FORBIDDEN = "403";
        /**
         * token过期
         */
        public static final String TOKEN_TIMEOUT = "403-1";

        /**
         * 无效token
         */
        public static final String INVALID_TOKEN = "403-2";

        /**
         * 无权访问,请求头为空
         */
        public static final String UNAUTHORIZED_HEADER_IS_EMPTY = "403-3";

        /**
         * 服务未找到
         */
        public static final String GATEWAY_NOT_FOUND_SERVICE = "404";

        /**
         * 网络异常
         */
        public static final String EXCEPTION_ERROR = "00040";

        /**
         * 离线状态
         */
        public static final String OFFLINE = "00041";
    }
}
