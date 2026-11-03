package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.youssefmohamedymsh.kyc.pages.DecisionPage;
import io.github.youssefmohamedymsh.kyc.pages.ReviewPage;
import io.github.youssefmohamedymsh.kyc.support.Hooks;
import io.github.youssefmohamedymsh.kyc.support.ScenarioContext;
import org.testng.Assert;

public class DecisionSteps {

    @When("I review and submit my application")
    public void reviewAndSubmit() {
        Hooks.openAsLoggedIn(ScenarioContext.applicant(), "review.html");
        new ReviewPage().submit();
    }

    @Then("the decision is {string}")
    public void decisionIs(String status) {
        DecisionPage page = new DecisionPage();
        page.waitForPage("decision.html");
        Assert.assertEquals(page.status(), status);
    }

    @Then("the risk score is {int}")
    public void riskScoreIs(int score) {
        Assert.assertEquals(new DecisionPage().riskScore(), score);
    }

    @Then("no decision reasons are listed")
    public void noReasons() {
        Assert.assertTrue(new DecisionPage().reasons().isEmpty(), "No reasons for an instant approval");
    }

    @Then("the decision reasons include {string}")
    public void reasonsInclude(String reason) {
        Assert.assertTrue(new DecisionPage().reasons().contains(reason),
                "Reasons " + new DecisionPage().reasons() + " should include '" + reason + "'");
    }

    @Then("I can invite my team")
    public void canInvite() {
        Assert.assertTrue(new DecisionPage().canInviteTeam());
    }

    @Then("I can't invite my team yet")
    public void cannotInvite() {
        Assert.assertFalse(new DecisionPage().canInviteTeam());
    }
}
