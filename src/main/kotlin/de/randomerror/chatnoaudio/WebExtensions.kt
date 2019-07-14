package de.randomerror.chatnoaudio

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait

fun WebDriver.waitForElement(by: By): WebElement {
    val wait = WebDriverWait(this, 15)
    wait.until { findElement(by) }
    return findElement(by)
}

fun WebDriver.waitForElements(by: By): List<WebElement> {
    val wait = WebDriverWait(this, 15)
    wait.until { findElement(by) }
    return findElements(by)
}

fun WebElement.waitForElement(by: By, driver: WebDriver): WebElement {
    val wait = WebDriverWait(driver, 15)
    wait.until { findElement(by) }
    return findElement(by)
}

fun WebElement.waitForElements(by: By, driver: WebDriver): List<WebElement> {
    val wait = WebDriverWait(driver, 15)
    wait.until { findElement(by) }
    return findElements(by)
}
