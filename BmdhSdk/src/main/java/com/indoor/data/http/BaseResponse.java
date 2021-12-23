package com.indoor.data.http;

import androidx.annotation.Keep;

/**
 * Created by Aaron on  2017/5/10.
 * 该类仅供参考，实际业务返回的固定字段, 根据需求来定义，
 */
@Keep
public class BaseResponse<T> {
    private String resultCode;
    private String resultMsg;
    private T result;
    private long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public boolean isOk() {
        return ResultCodeUtils.isRequestOptionSuccess(resultCode);
    }

    public String getResultMsg() {
        return resultMsg;
    }

    public void setResultMsg(String resultMsg) {
        this.resultMsg = resultMsg;
    }
}
