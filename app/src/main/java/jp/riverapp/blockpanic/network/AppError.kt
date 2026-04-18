package jp.riverapp.blockpanic.network

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.ConnectException

/**
 * アプリ全体のエラー分類。ネットワーク、ルーム、汎用の3系統で8カテゴリ。
 * 下位層 (SignalingClient, PeerHost, PeerClient) は throw、上位層 (GameCoordinator)
 * が classifyError() で AppError に変換し、ErrorDialog を表示する。
 */
enum class AppErrorKind {
    OFFLINE,           // 端末がオフライン
    TIMEOUT,           // 接続/応答タイムアウト
    ROOM_NOT_FOUND,    // 指定ルームが存在しない
    ROOM_EXPIRED,      // ルームハートビートが古い
    ROOM_FULL,         // 満員のため参加不可
    HOST_UNAVAILABLE,  // ホストが応答しない (WebRTC 接続失敗等)
    SERVER_ERROR,      // サーバー 5xx
    GENERIC            // 分類不能
}

class AppError(
    val kind: AppErrorKind,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message ?: kind.name, cause)

/** HTTP ステータスコードから AppError を生成 */
fun appErrorFromStatus(code: Int): AppError = when {
    code == 404 -> AppError(AppErrorKind.ROOM_NOT_FOUND, "HTTP 404")
    code == 409 -> AppError(AppErrorKind.ROOM_FULL, "HTTP 409")
    code in 500..599 -> AppError(AppErrorKind.SERVER_ERROR, "HTTP $code")
    else -> AppError(AppErrorKind.GENERIC, "HTTP $code")
}

/** 任意の例外を AppError に分類 */
fun classifyError(err: Throwable): AppError {
    if (err is AppError) return err
    if (err is SignalingError) return appErrorFromStatus(err.code)
    return when (err) {
        is UnknownHostException,
        is ConnectException -> AppError(AppErrorKind.OFFLINE, err.message, err)
        is SocketTimeoutException -> AppError(AppErrorKind.TIMEOUT, err.message, err)
        is IOException -> AppError(AppErrorKind.OFFLINE, err.message, err)
        else -> AppError(AppErrorKind.GENERIC, err.message, err)
    }
}
