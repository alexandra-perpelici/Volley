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


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RegistrationTests {


    private WebDriver driver;
    private WebDriverWait wait;


    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    @Test
    void testPasswordMismatchShowsError() {
        driver.get("http://localhost:8080/register");

        driver.findElement(By.name("username")).sendKeys("newuser3");
        driver.findElement(By.name("password")).sendKeys("Password123!");
        driver.findElement(By.name("confirmPassword")).sendKeys("Password12345!");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        WebElement errorMessage = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("registerErrorMessage"))
        );
        assertTrue(errorMessage.isDisplayed(), "Error message should appear for password mismatch");
    }

    @Test
    void testRegistrationEmptyUsernameShowsError() {
        driver.get("http://localhost:8080/register");


        WebElement usernameInput = driver.findElement(By.id("usernameInput"));
        WebElement registerButton = driver.findElement(By.id("registerButton"));

        usernameInput.clear();

        registerButton.click();

        String validationMessage = usernameInput.getAttribute("validationMessage");
        assertFalse(validationMessage.isEmpty(), "Browser should show required field message");
    }
    }