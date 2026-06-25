package com.ikuai.inetspeed.core.iperf3.runner

import com.ikuai.inetspeed.core.data.error.ErrorCode
import com.ikuai.inetspeed.core.iperf3.binary.BinaryInstaller
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessRunnerTest {

    private lateinit var runner: ProcessRunner

    @Before
    fun setup() {
        val installer = mockk<BinaryInstaller>(relaxed = true)
        runner = ProcessRunner(installer)
    }

    @Test
    fun classifyError_streamCreationFails_returnsBinaryUnsupported() {
        assertEquals(
            ErrorCode.BINARY_UNSUPPORTED,
            runner.classifyError("unable to create a new stream", 1),
        )
    }

    @Test
    fun classifyError_permissionDenied_returnsPermissionDenied() {
        assertEquals(
            ErrorCode.PERMISSION_DENIED,
            runner.classifyError("connect failed: Permission denied", 1),
        )
    }

    @Test
    fun classifyError_connectionRefused_returnsServerUnreachable() {
        assertEquals(
            ErrorCode.SERVER_UNREACHABLE,
            runner.classifyError("connect to 10.0.0.1 failed: Connection refused", 1),
        )
    }

    @Test
    fun classifyError_connectFailed_returnsServerUnreachable() {
        assertEquals(
            ErrorCode.SERVER_UNREACHABLE,
            runner.classifyError("error - connect failed: Connection timed out", 1),
        )
    }

    @Test
    fun classifyError_unableToConnect_returnsServerUnreachable() {
        assertEquals(
            ErrorCode.SERVER_UNREACHABLE,
            runner.classifyError("unable to connect to server", 1),
        )
    }

    @Test
    fun classifyError_noRoute_returnsServerUnreachable() {
        assertEquals(
            ErrorCode.SERVER_UNREACHABLE,
            runner.classifyError("error - no route to host", 1),
        )
    }

    @Test
    fun classifyError_serverBusy_returnsServerUnreachable() {
        assertEquals(
            ErrorCode.SERVER_UNREACHABLE,
            runner.classifyError("error - the server is busy", 1),
        )
    }

    @Test
    fun classifyError_protocolNotSupported_returnsProtocolUnsupported() {
        assertEquals(
            ErrorCode.PROTOCOL_UNSUPPORTED,
            runner.classifyError("error - protocol is not supported", 1),
        )
    }

    @Test
    fun classifyError_sctpNotSupported_returnsProtocolUnsupported() {
        assertEquals(
            ErrorCode.PROTOCOL_UNSUPPORTED,
            runner.classifyError("error - sctp is not supported on this platform", 1),
        )
    }

    @Test
    fun classifyError_timeout_returnsTestTimeout() {
        assertEquals(
            ErrorCode.TEST_TIMEOUT,
            runner.classifyError("error - test timed out", 1),
        )
    }

    @Test
    fun classifyError_timedOut_returnsTestTimeout() {
        assertEquals(
            ErrorCode.TEST_TIMEOUT,
            runner.classifyError("connection timed out", 1),
        )
    }

    @Test
    fun classifyError_unableToReadFromStream_returnsServerUnreachable() {
        assertEquals(
            ErrorCode.SERVER_UNREACHABLE,
            runner.classifyError("error - unable to read from stream socket: Try again", 1),
        )
    }

    @Test
    fun classifyError_genericError_returnsUnknown() {
        assertEquals(
            ErrorCode.UNKNOWN,
            runner.classifyError("some unexpected error occurred", 1),
        )
    }

    @Test
    fun classifyError_nonZeroExitNoMatch_returnsUnknown() {
        assertEquals(
            ErrorCode.UNKNOWN,
            runner.classifyError("something went wrong", 2),
        )
    }

    @Test
    fun classifyError_zeroExitNoError_returnsUnknown() {
        assertEquals(
            ErrorCode.UNKNOWN,
            runner.classifyError("normal output with no errors", 0),
        )
    }

    @Test
    fun classifyError_udpTimeout_returnsServerUnreachable() {
        assertEquals(
            ErrorCode.SERVER_UNREACHABLE,
            runner.classifyError("UDP 连接超时，请检查服务器是否支持 UDP 或网络是否阻止 UDP 流量", 1),
        )
    }

    @Test
    fun classifyError_udpUnreachable_returnsServerUnreachable() {
        assertEquals(
            ErrorCode.SERVER_UNREACHABLE,
            runner.classifyError("UDP 不可达，请检查服务器是否支持 UDP", 1),
        )
    }
}
