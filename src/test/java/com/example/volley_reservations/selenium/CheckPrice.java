package com.example.volley_reservations.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class CheckPrice {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {

        driver = new ChromeDriver();
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get("http://localhost:8080/login");

        WebElement usernameInput = driver.findElement(By.name("username"));
        usernameInput.sendKeys("Denisa");

        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys("denisa1234");

        WebElement loginButton = driver.findElement(By.cssSelector("button[type='submit']"));
        loginButton.click();

        wait.until(ExpectedConditions.urlContains("/home"));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void checkPriceTest() {
        driver.get("http://localhost:8080/field/1");
        WebElement availableCell = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("td.available"))
        );
        availableCell.click();
        WebElement confirmBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("popupYesBtn"))
        );
        confirmBtn.click();
        wait.until(ExpectedConditions.urlContains("/cart/view"));

        List<WebElement> rows = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("table tbody tr"))
        );

        int itemCount = rows.size();
        int expectedTotal = itemCount * 20;
        WebElement priceSpan = driver.findElement(By.cssSelector(".mb-4 h4 span"));
        int actualTotal = Integer.parseInt(priceSpan.getText().trim());
        Assertions.assertEquals(expectedTotal, actualTotal,
                "Total price mismatch! With " + itemCount + " items, price should be " + expectedTotal);
    }
}