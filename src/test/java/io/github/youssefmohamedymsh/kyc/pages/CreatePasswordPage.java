package io.github.youssefmohamedymsh.kyc.pages;

import org.openqa.selenium.WebElement;

import java.util.List;

public class CreatePasswordPage extends BasePage {

    public void createPassword(String password, String confirmation) {
        type("password", password);
        type("confirmPassword", confirmation);
        click("verify-submit");
    }

    public List<String> failedRules() {
        visible("failed-rule");
        return all("failed-rule").stream().map(WebElement::getText).toList();
    }
}
