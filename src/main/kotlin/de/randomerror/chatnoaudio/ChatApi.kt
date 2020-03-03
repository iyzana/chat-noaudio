package de.randomerror.chatnoaudio

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.GeckoDriverService
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO

data class Chat(val name: String, val elementIndex: Int, val hasUnread: Boolean, val lastMessageType: MessageType)

enum class MessageType {
    TEXT,
    AUDIO,
    OTHER
}

class ChatApi {
    private val driver = GeckoDriverService.Builder()
        .usingDriverExecutable(File(System.getenv("HOME") + "/Downloads/geckodriver"))
        .build()
        .let(::FirefoxDriver)

    fun getAuthQr(): BitMatrix {
        return QRCodeWriter().encode(getAuthCode(), BarcodeFormat.QR_CODE, 0, 0)
    }

    fun getAuthCode(): String {
        val url = Base64.getDecoder().decode("aHR0cHM6Ly93ZWIud2hhdHNhcHAuY29tLwo=")
        driver.navigate().to(String(url))

        Thread.sleep(2000)
        val base64Auth = driver.executeAsyncScript("""
            const callback = arguments[arguments.length - 1];
            callback(document.getElementsByClassName("landing-main")[0].getElementsByTagName("canvas")[0].toDataURL("image/png"));
        """.trimIndent()) as String
        val base64qr = base64Auth
            .split(';')[1]
            .split(',')[1]
        val image = base64qr
            .let(Base64.getDecoder()::decode)
            .let(::ByteArrayInputStream)
            .let(ImageIO::read)
        return image
            .let(::BufferedImageLuminanceSource)
            .let(::HybridBinarizer)
            .let(::BinaryBitmap)
            .let(QRCodeReader()::decode)
            .text
    }

    fun waitForAuth() {
        driver.waitForElement(By.id("pane-side"))
    }

    // todo: handle other web session opening
    // possibly wait 10 minutes then reopen session

    fun getChats(): Set<Chat> {
        Thread.sleep(2000)
        return driver.findElement(By.id("pane-side"))
            .findElements(By.xpath("./div[1]/div[1]/div[1]/div"))
            .mapIndexed { index, chatElement ->
                val name = chatElement.findElement(By.xpath("./div[1]/div[1]/div[2]/div[1]/div[1]//span")).text
                val hasUnread = hasUnreadMessages(chatElement)
                val messageType = getMessageType(chatElement)
                Chat(name, index + 1, hasUnread, messageType)
            }
            .toSet()
    }

    fun hasNewAudioMessage(chat: Chat): Boolean {
        return chat.lastMessageType == MessageType.AUDIO
    }

    fun getAudioMessage(chat: Chat): ByteArray {
        getChat(chat).click()
        Thread.sleep(1000)

        val audioSource = driver.findElement(By.id("main"))
            .waitForElement(By.xpath("./div[3]/div[1]/div[1]/div[2]/div[last()]"), driver)
            .waitForElement(By.xpath("./div[1]/div[1]/div[1]/div[1]/div[1]/div[1]/div[2]/div[2]/audio"), driver)
            .getAttribute("src")

        val base64Audio = driver.executeAsyncScript("""
            const callback = arguments[arguments.length - 1];
            fetch("$audioSource")
              .then(body => body.arrayBuffer())
              .then(arrayBuffer => {
                const bytes = new Uint8Array(arrayBuffer);
                const data = Array.from(bytes);
                const binary = data.map(i => String.fromCharCode(i)).join('');
                return window.btoa(binary);
              })
              .then(data => callback(data))
        """.trimIndent()) as String

        return Base64.getDecoder().decode(base64Audio)
    }

    fun sendMessage(chat: Chat, text: String) {
        val chatElement = getChat(chat)
        chatElement.click()
        chatElement.sendKeys(text)
        // todo: find text input and send keys
    }

    private fun getChat(chat: Chat): WebElement {
        return driver.findElement(By.id("pane-side"))
            .findElement(By.xpath("./div[1]/div[1]/div[1]/div[${chat.elementIndex}]"))
    }

    private fun getMessageType(chatElement: WebElement): MessageType {
        return try {
            val attribute =
                chatElement.findElement(By.xpath("./div[1]/div[1]/div[2]/div[2]/div[1]/span[1]/div/span"))
                    .getAttribute("data-icon")
            when (attribute) {
                "status-ptt-green" -> MessageType.AUDIO
                "status-ptt-blue" -> MessageType.AUDIO
                else -> MessageType.OTHER
            }
        } catch (e: NoSuchElementException) {
            MessageType.TEXT
        }
    }

    private fun hasUnreadMessages(chatElement: WebElement): Boolean {
        val unreadMessages =
            chatElement.findElements(By.xpath("./div[1]/div[1]/div[2]/div[2]/div[2]/span[1]/div/span"))
                .firstOrNull()
        return unreadMessages != null
    }

    fun close() {
        driver.close()
    }
}
