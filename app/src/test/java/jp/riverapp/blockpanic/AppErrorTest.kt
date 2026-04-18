package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.network.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AppErrorTest {

    @Test
    fun `AppError passthrough`() {
        val original = AppError(AppErrorKind.ROOM_NOT_FOUND)
        assertEquals(AppErrorKind.ROOM_NOT_FOUND, classifyError(original).kind)
    }

    @Test
    fun `HTTP status code mapping`() {
        assertEquals(AppErrorKind.ROOM_NOT_FOUND, appErrorFromStatus(404).kind)
        assertEquals(AppErrorKind.ROOM_FULL, appErrorFromStatus(409).kind)
        assertEquals(AppErrorKind.SERVER_ERROR, appErrorFromStatus(500).kind)
        assertEquals(AppErrorKind.SERVER_ERROR, appErrorFromStatus(503).kind)
        assertEquals(AppErrorKind.GENERIC, appErrorFromStatus(400).kind)
    }

    @Test
    fun `SignalingError mapping`() {
        assertEquals(AppErrorKind.ROOM_NOT_FOUND, classifyError(SignalingError(404, "nf")).kind)
        assertEquals(AppErrorKind.ROOM_FULL, classifyError(SignalingError(409, "full")).kind)
        assertEquals(AppErrorKind.SERVER_ERROR, classifyError(SignalingError(502, "bad")).kind)
        assertEquals(AppErrorKind.GENERIC, classifyError(SignalingError(400, "bad req")).kind)
    }

    @Test
    fun `UnknownHost is offline`() {
        assertEquals(AppErrorKind.OFFLINE, classifyError(UnknownHostException("no dns")).kind)
    }

    @Test
    fun `ConnectException is offline`() {
        assertEquals(AppErrorKind.OFFLINE, classifyError(ConnectException("refused")).kind)
    }

    @Test
    fun `SocketTimeout is timeout`() {
        assertEquals(AppErrorKind.TIMEOUT, classifyError(SocketTimeoutException("slow")).kind)
    }

    @Test
    fun `Other IOException is offline`() {
        assertEquals(AppErrorKind.OFFLINE, classifyError(IOException("io")).kind)
    }

    @Test
    fun `Unknown error falls back to generic`() {
        assertEquals(AppErrorKind.GENERIC, classifyError(RuntimeException("?")).kind)
    }
}
