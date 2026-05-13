package com.marketai.backend.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(0, "success"),

    // 通用错误
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 认证模块 1xxx
    EMAIL_EXISTS(1001, "邮箱已被注册"),
    INVALID_EMAIL(1002, "邮箱格式不正确"),
    INVALID_PASSWORD(1003, "密码长度至少8位"),
    INVALID_NICKNAME(1004, "昵称长度2-20个字符"),
    AUTH_FAILED(1005, "账号或密码错误"),
    TOKEN_INVALID(1006, "Token无效或已过期"),

    // 项目模块 2xxx
    PROJECT_NOT_FOUND(2001, "项目不存在"),
    PROJECT_FORBIDDEN(2002, "无权操作该项目"),
    KEYWORD_LIMIT(2003, "项目关键词总数不能超过20个"),

    // 洞察分析模块 3xxx
    INSIGHT_TASK_NOT_FOUND(3001, "分析任务不存在"),
    INSIGHT_REPORT_NOT_FOUND(3002, "洞察报告不存在"),
    INSIGHT_TASK_LIMIT(429, "并发分析任务已达上限(3个)，请等待当前任务完成后再提交"),

    // 用户画像模块 4xxx
    PERSONA_NOT_FOUND(4001, "用户画像不存在"),
    PERSONA_TASK_NOT_FOUND(4002, "画像生成任务不存在"),
    PERSONA_TASK_LIMIT(4003, "并发画像生成任务已达上限(3个)，请稍后再试"),
    PERSONA_COUNT_INVALID(4004, "生成数量应在 3-5 之间"),

    // 竞品分析模块 5xxx
    COMPETITOR_NOT_FOUND(5001, "竞品不存在"),
    COMPETITOR_TASK_NOT_FOUND(5002, "竞品分析任务不存在"),
    COMPETITOR_TASK_LIMIT(5003, "并发竞品分析任务已达上限(3个)，请稍后再试"),
    COMPETITOR_LIST_EMPTY(5004, "项目尚未配置竞品名单，请先在项目设置中添加竞品");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}