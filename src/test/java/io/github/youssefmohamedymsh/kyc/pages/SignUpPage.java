package io.github.youssefmohamedymsh.kyc.pages;

import io.github.youssefmohamedymsh.kyc.support.TestEnvironment;

public class SignUpPage extends BasePage {

    public SignUpPage open() {
        driver.get(TestEnvironment.url("index.html"));
        return this;
    }

    public void signUp(String email, String firstName, String lastName, String companyName) {
        type("email", email);
        type("firstName", firstName);
        type("lastName", lastName);
        type("companyName", companyName);
        click("signup-submit");
    }

    public String checkInboxAddress() {
        visible("check-inbox");
        return textOf("sent-to");
    }

    public void openInbox() {
        click("open-inbox");
    }
}
