package com.example.volley_reservations.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ReservedSpot {


    private WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();

        driver.get("http://localhost:8080/login");

        WebElement usernameInput = driver.findElement(By.name("username"));
        usernameInput.sendKeys("Denisa");

        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys("denisa1234");

        WebElement loginButton = driver.findElement(By.cssSelector("button[type='submit']"));
        loginButton.click();
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    @Test
    void testClickingReservedSlotDoesNotOpenPopup() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("http://localhost:8080/field/1");

        WebElement reservedCell = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("td.reserved"))
        );
        reservedCell.click();
        WebElement popup = driver.findElement(By.id("reservationPopup"));
        org.junit.jupiter.api.Assertions.assertFalse(popup.isDisplayed(),
                "Popup should not be visible when clicking a reserved slot.");

        WebElement toast = driver.findElement(By.id("toast"));
        wait.until(d -> toast.isDisplayed() || toast.getCssValue("visibility").equals("visible"));

        String toastText = toast.getText();
        org.junit.jupiter.api.Assertions.assertEquals("This slot is already reserved!", toastText,
                "Toast message should inform the user the slot is reserved.");
    }
}
