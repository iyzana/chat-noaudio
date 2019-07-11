package de.randomerror.chatnoaudio

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.GeckoDriverService
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Duration
import java.util.*
import javax.imageio.ImageIO

fun main() {
    val exe = GeckoDriverService.Builder()
        .usingDriverExecutable(File(System.getenv("HOME") + "/Downloads/geckodriver"))
        .build()
    val driver: WebDriver = FirefoxDriver(exe)

    val fluentWait = FluentWait(driver)
        .withTimeout(Duration.ofSeconds(30))
        .pollingEvery(Duration.ofMillis(200))
        .ignoring(NoSuchElementException::class.java)

    try {
        getQr(driver)
        listChats(driver)
    } finally {
//        driver.quit()
    }
}

fun getQr(driver: WebDriver) {
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
    val bitmatrix = image
        .let(::BufferedImageLuminanceSource)
        .let(::HybridBinarizer)
        .let(::BinaryBitmap)
        .let(QRCodeReader()::decode)
        .let { QRCodeWriter().encode(it.text, BarcodeFormat.QR_CODE, 0, 0) }
    for (y in 0 until bitmatrix.height) {
        for (x in 0 until bitmatrix.width) {
            print(if (bitmatrix[x, y]) "  " else "██")
        }
        println()
    }
}

fun listChats(driver: WebDriver) {
    val chats = driver.waitForElement(By.id("pane-side"))
        .findElements(By.xpath("./div/div/div/div"))

    println("chats")
    for (chat in chats) {
        val name = chat.findElement(By.xpath("./div[1]/div[1]/div[2]/div/div/span")).text
        println("chat: '$name'")
    }
}

fun WebDriver.waitForElement(by: By): WebElement {
    val wait = WebDriverWait(this, 15)
    wait.until(ExpectedConditions.presenceOfElementLocated(by))
    return findElement(by)
}

fun WebDriver.waitForElements(by: By): List<WebElement> {
    val wait = WebDriverWait(this, 15)
    wait.until(ExpectedConditions.presenceOfElementLocated(by))
    return findElements(by)
}

fun WebElement.waitForElement(by: By, driver: WebDriver): WebElement {
    val wait = WebDriverWait(driver, 15)
    wait.until(ExpectedConditions.presenceOfElementLocated(by))
    return findElement(by)
}

fun WebElement.waitForElements(by: By, driver: WebDriver): List<WebElement> {
    val wait = WebDriverWait(driver, 15)
    wait.until(ExpectedConditions.presenceOfElementLocated(by))
    return findElements(by)
}
