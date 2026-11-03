package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.youssefmohamedymsh.kyc.pages.InboxPage;
import io.github.youssefmohamedymsh.kyc.pages.SignUpPage;
import io.github.youssefmohamedymsh.kyc.support.Applicant;
import io.github.youssefmohamedymsh.kyc.support.LedgerlineApi;
import io.github.youssefmohamedymsh.kyc.support.ScenarioContext;
import org.testng.Assert;

public class SignUpSteps {

    @Given("I am on the sign-up page")
    public void openSignUp() {
        new SignUpPage().open();
    }

    @When("I sign up as {string} from {string} with a new work email")
    public void signUpWithNewEmail(String fullName, String company) {
        Applicant a = applicantNamed(fullName, company);
        new SignUpPage().signUp(a.email, a.firstName, a.lastName, a.companyName);
    }

    @When("I sign up as {string} from {string} with the email {string}")
    public void signUpWithEmail(String fullName, String company, String email) {
        Applicant a = applicantNamed(fullName, company);
        new SignUpPage().signUp(email, a.firstName, a.lastName, a.companyName);
    }

    @Given("someone has already signed up with my work email")
    public void alreadySignedUp() {
        Applicant a = ScenarioContext.applicant();
        LedgerlineApi.signUp(a.email, a.firstName, a.lastName, a.companyName).then().statusCode(201);
    }

    @When("I sign up as {string} from {string} with that same email")
    public void signUpWithSameEmail(String fullName, String company) {
        String email = ScenarioContext.applicant().email;
        String[] names = fullName.split(" ", 2);
        new SignUpPage().signUp(email, names[0], names[1], company);
    }

    @Then("I am asked to check my inbox")
    public void askedToCheckInbox() {
        Assert.assertEquals(new SignUpPage().checkInboxAddress(), ScenarioContext.applicant().email);
    }

    @Then("my inbox has an email titled {string}")
    public void inboxHasEmail(String subject) {
        new SignUpPage().openInbox();
        Assert.assertEquals(new InboxPage().latestSubject(), subject);
    }

    private static Applicant applicantNamed(String fullName, String company) {
        String[] names = fullName.split(" ", 2);
        Applicant a = new Applicant(names[0], names[1], company);
        ScenarioContext.setApplicant(a);
        return a;
    }
}
