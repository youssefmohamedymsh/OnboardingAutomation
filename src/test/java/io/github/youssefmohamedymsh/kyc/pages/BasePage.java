package io.github.youssefmohamedymsh.kyc.pages;

import io.github.youssefmohamedymsh.kyc.support.Hooks;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Shared page behaviour. Elements are found by data-testid, which the app keeps stable on purpose,
 * and every interaction waits explicitly for the element it needs.
 */
public abstract class BasePage {

    protected final WebDriver driver = Hooks.driver();
    public final WebDriverWait wait = waitFor(driver);

    /** Pages re-render after API calls, so a wait retries on stale elements instead of failing. */
    private static WebDriverWait waitFor(WebDriver driver) {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(10));
        w.ignoring(StaleElementReferenceException.class);
        return w;
    }

    protected static By testId(String id) {
        return By.cssSelector("[data-testid=\"" + id + "\"]");
    }

    protected WebElement visible(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(testId(id)));
    }

    protected void type(String id, String text) {
        WebElement field = visible(id);
        field.clear();
        field.sendKeys(text);
    }

    protected void choose(String id, String value) {
        new Select(visible(id)).selectByValue(value);
    }

    protected void click(String id) {
        wait.until(ExpectedConditions.elementToBeClickable(testId(id))).click();
    }

    /** File inputs are often visually hidden, so only their presence is required. */
    protected void upload(String id, Path file) {
        wait.until(ExpectedConditions.presenceOfElementLocated(testId(id))).sendKeys(file.toAbsolutePath().toString());
    }

    /** Waits until the element has text (messages are filled in after an API call) and returns it. */
    protected String textOf(String id) {
        wait.until(d -> !d.findElement(testId(id)).getText().isBlank());
        return driver.findElement(testId(id)).getText().trim();
    }

    protected List<WebElement> all(String id) {
        return driver.findElements(testId(id));
    }

    public String fieldError(String field) {
        return textOf("error-" + field);
    }

    public String formError() {
        return textOf("form-error");
    }

    public void waitForPage(String path) {
        wait.until(ExpectedConditions.urlContains(path));
    }

    public String heading() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1"))).getText();
    }
}
