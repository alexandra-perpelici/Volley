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
import java.util.List;

public class CartTest {


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

        WebElement loginButton = driver.findElement(By.id("loginButton"));
        loginButton.click();
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }
    @Test
    void testAddToCartFlow() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("http://localhost:8080/field/1");

        WebElement availableCell = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("td.available"))
        );
        availableCell.click();
        WebElement addToCartBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("popupYesBtn")));
        addToCartBtn.click();
        wait.until(ExpectedConditions.urlContains("/cart/view"));
        List<WebElement> cartRows = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("table tbody tr"))
        );
        org.junit.jupiter.api.Assertions.assertFalse(cartRows.isEmpty(), "The cart table should not be empty.");
    }

    @Test
    void testCancelReservationFlow() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("http://localhost:8080/field/1");


        WebElement availableCell = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("td.available"))
        );
        availableCell.click();

        WebElement popup = driver.findElement(By.id("reservationPopup"));
        wait.until(ExpectedConditions.attributeContains(popup, "class", "show"));

        WebElement cancelBtn = driver.findElement(By.id("popupNoBtn"));
        cancelBtn.click();

        wait.until(ExpectedConditions.invisibilityOf(popup));

        String currentUrl = driver.getCurrentUrl();
        org.junit.jupiter.api.Assertions.assertTrue(currentUrl.contains("/field/1"),
                "Should remain on the field page after clicking cancel.");
    }
}
