package de.randomerror.chatnoaudio

import com.google.zxing.common.BitMatrix

fun main() {
    val api = ChatApi()

    try {
        authenticate(api)

        while (true) {
            println("::chats::")
            api.getChats()
                .onEach { println("chat: $it") }
                .filter { chat -> api.hasNewAudioMessage(chat) }
                .map { chat -> chat to api.getAudioMessage(chat) }
                .map { (chat, audio) -> chat to /* todo: map audio to text using aws */ String(audio) }
                .forEach { (chat, text) -> api.sendMessage(chat, text) }
            Thread.sleep(10 * 1000)
            break
        }
    } finally {
//        api.close()
    }
}

fun authenticate(api: ChatApi) {
    printQrCode(api.getAuthQr())
    api.waitForAuth()
}

fun printQrCode(qrCode: BitMatrix) {
    println()
    println()
    for (y in 0 until qrCode.height step 2) {
        for (x in 0 until qrCode.width) {
            print(
                when {
                    !qrCode[x, y] && (y + 1 >= qrCode.height || !qrCode[x, y + 1]) -> "█"
                    !qrCode[x, y] && qrCode[x, y + 1] -> "▀"
                    qrCode[x, y] && (y + 1 >= qrCode.height || !qrCode[x, y + 1]) -> "▄"
                    else -> " "
                }
            )
        }
        println()
    }
    println()
}
