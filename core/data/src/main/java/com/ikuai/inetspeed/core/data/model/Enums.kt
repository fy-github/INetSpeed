package com.ikuai.inetspeed.core.data.model

enum class Protocol(val value: String) {
    TCP("tcp"),
    UDP("udp"),
    SCTP("sctp");

    companion object {
        fun from(value: String): Protocol = entries.firstOrNull { it.value == value } ?: TCP
    }
}

enum class Direction(val value: String) {
    FORWARD("forward"),
    REVERSE("reverse");

    companion object {
        fun from(value: String): Direction = entries.firstOrNull { it.value == value } ?: FORWARD
    }
}

enum class IpVersion(val value: String) {
    IPV4("ipv4"),
    IPV6("ipv6");

    companion object {
        fun from(value: String): IpVersion = entries.firstOrNull { it.value == value } ?: IPV4
    }
}

enum class TestStatus(val value: String) {
    IDLE("idle"),
    PREPARING("preparing"),
    RUNNING("running"),
    CANCELLING("cancelling"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    FAILED("failed");

    companion object {
        fun from(value: String): TestStatus = entries.firstOrNull { it.value == value } ?: IDLE
    }
}

enum class ToolType(val value: String) {
    PING("ping"),
    TRACEROUTE("traceroute"),
    NETWORK_INFO("network_info");

    companion object {
        fun from(value: String): ToolType = entries.firstOrNull { it.value == value } ?: PING
    }
}
