package com.ikuai.inetspeed.core.data.error

/**
 * INetSpeed 统一错误码体系
 * 所有模块的错误必须映射到这些错误码
 */
enum class ErrorCode(
    val code: String,
    val userMessage: String,
) {
    SERVER_UNREACHABLE(
        code = "SERVER_UNREACHABLE",
        userMessage = "服务器不可达，请检查地址或网络",
    ),
    PORT_BLOCKED(
        code = "PORT_BLOCKED",
        userMessage = "端口可能未开放",
    ),
    BINARY_MISSING(
        code = "BINARY_MISSING",
        userMessage = "测速组件缺失，请重新安装或修复",
    ),
    BINARY_UNSUPPORTED(
        code = "BINARY_UNSUPPORTED",
        userMessage = "当前设备不支持该测速组件",
    ),
    PARAM_INVALID(
        code = "PARAM_INVALID",
        userMessage = "参数不合法，请调整后重试",
    ),
    PROTOCOL_UNSUPPORTED(
        code = "PROTOCOL_UNSUPPORTED",
        userMessage = "当前协议不可用",
    ),
    TEST_TIMEOUT(
        code = "TEST_TIMEOUT",
        userMessage = "测试超时",
    ),
    PARSE_FAILED(
        code = "PARSE_FAILED",
        userMessage = "结果解析失败，可查看原始输出",
    ),
    PERMISSION_DENIED(
        code = "PERMISSION_DENIED",
        userMessage = "请授权后重试",
    ),
    SYNC_CONFLICT(
        code = "SYNC_CONFLICT",
        userMessage = "已按同步策略处理冲突",
    ),
    UNKNOWN(
        code = "UNKNOWN",
        userMessage = "发生未知错误",
    ),
}

/**
 * 统一异常类，携带错误码和可选的诊断信息
 */
class INetSpeedException(
    val errorCode: ErrorCode,
    val diagnosticInfo: Map<String, String> = emptyMap(),
    cause: Throwable? = null,
) : Exception(errorCode.userMessage, cause)
