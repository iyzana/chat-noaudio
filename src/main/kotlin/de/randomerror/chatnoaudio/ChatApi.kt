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
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.GeckoDriverService
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.ByteArrayInputStream
import java.io.File
import java.lang.RuntimeException
import java.util.*
import javax.imageio.ImageIO

data class Chat(val name: String)

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

        val base64qr = driver.waitForElement(By.className("landing-main"))
            .waitForElement(By.tagName("img"), driver)
            .getAttribute("src")
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
        val wait = WebDriverWait(driver, 15)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pane-side")))
    }

    fun getChats(): Set<Chat> {
        return driver.findElement(By.id("pane-side"))
            .findElements(By.xpath("./div/div/div/div"))
            .map { chat ->
                val name = chat.findElement(By.xpath("./div[1]/div[1]/div[2]/div/div/span")).text
                Chat(name)
            }
            .toSet()
    }
}