package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.youssefmohamedymsh.kyc.pages.CreatePasswordPage;
import io.github.youssefmohamedymsh.kyc.pages.InboxPage;
import io.github.youssefmohamedymsh.kyc.support.Applicant;
import io.github.youssefmohamedymsh.kyc.support.LedgerlineApi;
import io.github.youssefmohamedymsh.kyc.support.ScenarioContext;
import org.testng.Assert;

public class VerificationSteps {

    @Given("I have signed up with a new work email")
    public void signedUp() {
        LedgerlineApi.signUpAndGetLink(ScenarioContext.applicant());
    }

    @When("I open the verification link from my inbox")
    public void openVerificationLink() {
        new InboxPage().openFor(ScenarioContext.applicant().email).openLatestVerificationLink();
    }

    @When("I create the password {string}")
    public void createPassword(String password) {
        new CreatePasswordPage().createPassword(password, password);
    }

    @When("I create the password {string} but confirm it as {string}")
    public void createMismatchedPassword(String password, String confirmation) {
        new CreatePasswordPage().createPassword(password, confirmation);
    }

    @Then("I am told the password needs {string}")
    public void passwordNeeds(String rule) {
        Assert.assertTrue(new CreatePasswordPage().failedRules().contains(rule),
                "Failed rules should include '" + rule + "'");
    }

    @Given("I have already created my password with this link")
    public void alreadyVerified() {
        Applicant a = ScenarioContext.applicant();
        LedgerlineApi.verify(LedgerlineApi.tokenFrom(a.verificationLink), a.password).then().statusCode(200);
    }
}
