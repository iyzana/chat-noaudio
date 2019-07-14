package de.randomerror.chatnoaudio

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
    val qrCode = api.getAuthQr()
    for (y in 0 until qrCode.height) {
        for (x in 0 until qrCode.width) {
            print(if (qrCode[x, y]) "  " else "██")
        }
        println()
    }
    api.waitForAuth()
}
