package com.example.volley_reservations.selenium;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;


public class HomePageTests {


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
    void testField1ButtonNavigatesCorrectly() {
        WebElement field1Button = driver.findElement(By.id("field1Button"));
        field1Button.click();
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/field/1"));
        String currentUrl = driver.getCurrentUrl();
        org.junit.jupiter.api.Assertions.assertTrue(currentUrl.contains("/field/1"),
                "Clicking Field 1 should navigate to /field/1");
    }

    @Test
    void testField2ButtonNavigatesCorrectly() {
        WebElement field1Button = driver.findElement(By.id("field2Button"));
        field1Button.click();
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/field/2"));
        String currentUrl = driver.getCurrentUrl();
        org.junit.jupiter.api.Assertions.assertTrue(currentUrl.contains("/field/2"),
                "Clicking Field 1 should navigate to /field/2");
    }

    @Test
    void testLogoutButtonNavigatesCorrectly() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement logoutButton = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("logoutButton"))
        );
        logoutButton.click();
        wait.until(ExpectedConditions.urlContains("/login"));
        String currentUrl = driver.getCurrentUrl();
        org.junit.jupiter.api.Assertions.assertTrue(currentUrl.contains("/login"),
                "URL should contain /login after logout. Found: " + currentUrl);
    }
}