package de.randomerror.chatnoaudio

import com.google.zxing.common.BitMatrix

fun main() {
    try {
        val api = ChatApi()
        printQrCode(api.getAuthQr())
        api.waitForAuth()
        val chats = api.getChats()
        printChats(chats)
    } finally {
//        driver.quit()
    }
}

fun printQrCode(bitMatrix: BitMatrix) {
    for (y in 0 until bitMatrix.height) {
        for (x in 0 until bitMatrix.width) {
            print(if (bitMatrix[x, y]) "  " else "██")
        }
        println()
    }
}

fun printChats(chats: Collection<Chat>) {
    println("chats")
    for (chat in chats) {
        println("chat: $chat")
    }
}
