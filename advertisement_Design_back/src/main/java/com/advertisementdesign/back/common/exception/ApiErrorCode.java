package com.advertisementdesign.back.common.exception;

public enum ApiErrorCode {
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 token 无效"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "请求与当前资源状态冲突"),
    INTERNAL_ERROR(500, "系统异常"),
    BUSINESS_ERROR(1001, "业务异常");

    private final int code;
    private final String message;

    ApiErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
