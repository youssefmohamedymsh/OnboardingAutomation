package io.github.youssefmohamedymsh.kyc.pages;

public class ReviewPage extends BasePage {

    public String ownersSummary() {
        return textOf("summary-owners");
    }

    public void submit() {
        click("submit-application");
    }
}
