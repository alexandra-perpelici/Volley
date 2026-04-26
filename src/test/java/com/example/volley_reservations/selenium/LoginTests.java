package com.example.volley_reservations.selenium;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginTests {

    private WebDriver driver;

    @BeforeEach
    void setUp() {

        driver = new ChromeDriver();
        driver.manage().window().maximize();
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    @Test
    void testValidLogin() {

        driver.get("http://localhost:8080/login");

        WebElement usernameInput = driver.findElement(By.name("username"));
        usernameInput.sendKeys("Denisa");

        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys("denisa1234");

        WebElement loginButton = driver.findElement(By.id("loginButton"));
        loginButton.click();

        WebElement welcomeText = driver.findElement(By.tagName("h1"));
        assertTrue(welcomeText.getText().contains("Welcome!"), "Login failed or home page not loaded");
    }

    @Test
    void testInvalidLoginShowsError() {
        driver.get("http://localhost:8080/login");

        WebElement usernameInput = driver.findElement(By.name("username"));
        usernameInput.sendKeys("wronguser");

        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys("wrongpass");

        WebElement loginButton = driver.findElement(By.cssSelector("button[type='submit']"));
        loginButton.click();

        WebElement errorAlert = driver.findElement(By.className("alert-danger"));
        assertTrue(errorAlert.isDisplayed(), "Error message not shown for invalid login");
    }
}