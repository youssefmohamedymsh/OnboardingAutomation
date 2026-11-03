package io.github.youssefmohamedymsh.kyc.support;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Browser lifecycle for @ui scenarios: a fresh Chrome per scenario, a screenshot on failure.
 *
 * Settings (-D system properties):
 *   headless       true to run without a window (CI sets this)
 *   chrome.binary  path to a specific Chrome/Chromium build (optional)
 *   chrome.args    extra comma-separated Chrome flags, e.g. --no-sandbox in containers (optional)
 */
public class Hooks {

    private static WebDriver driver;

    public static WebDriver driver() {
        if (driver == null) {
            throw new IllegalStateException("No browser: is this scenario tagged @ui?");
        }
        return driver;
    }

    @Before(order = 0)
    public void resetContext() {
        ScenarioContext.reset();
    }

    @Before(value = "@ui", order = 1)
    public void openBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1280,900");
        String extraArgs = System.getProperty("chrome.args", "");
        for (String arg : extraArgs.split(",")) {
            if (!arg.isBlank()) {
                options.addArguments(arg.trim());
            }
        }
        String binary = System.getProperty("chrome.binary");
        if (binary != null && !binary.isBlank()) {
            options.setBinary(binary);
        }
        driver = new ChromeDriver(options);
    }

    @After(value = "@ui")
    public void closeBrowser(Scenario scenario) {
        if (driver == null) {
            return;
        }
        try {
            if (scenario.isFailed()) {
                scenario.attach(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES), "image/png",
                        "Screen at failure");
            }
        } finally {
            driver.quit();
            driver = null;
        }
    }

    /** Opens the app already logged in as the applicant, the way a returning user would be. */
    public static void openAsLoggedIn(Applicant applicant, String page) {
        driver().get(TestEnvironment.url("index.html"));
        ((JavascriptExecutor) driver()).executeScript(
                "sessionStorage.setItem('ledgerline.session', arguments[0])", applicant.session);
        driver().get(TestEnvironment.url(page));
    }
}
