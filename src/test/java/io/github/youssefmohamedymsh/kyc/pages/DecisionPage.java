package io.github.youssefmohamedymsh.kyc.pages;

import org.openqa.selenium.WebElement;

import java.util.List;

public class DecisionPage extends BasePage {

    public String status() {
        return textOf("decision-status");
    }

    public int riskScore() {
        wait.until(d -> textOf("risk-score").contains("/"));
        return Integer.parseInt(textOf("risk-score").split(" ")[0]);
    }

    public List<String> reasons() {
        status(); // the page has rendered once the status is shown
        return all("decision-reason").stream().map(WebElement::getText).toList();
    }

    public boolean canInviteTeam() {
        status();
        return driver.findElement(testId("invite-team-link")).isDisplayed();
    }
}
