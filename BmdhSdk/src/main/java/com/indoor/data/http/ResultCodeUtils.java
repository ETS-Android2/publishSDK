package com.indoor.data.http;

public class ResultCodeUtils {

    public static String getHttpResultMsg(String code){

        String resultMsg="";
        switch (code) {
            case RESULTCODE.SUCCESS:
                resultMsg = "操作未授权";
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
    class RESULTCODE {
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
    }
}
