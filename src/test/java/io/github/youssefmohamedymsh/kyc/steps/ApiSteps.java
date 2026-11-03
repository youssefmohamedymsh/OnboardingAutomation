package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.youssefmohamedymsh.kyc.support.Applicant;
import io.github.youssefmohamedymsh.kyc.support.LedgerlineApi;
import io.github.youssefmohamedymsh.kyc.support.ScenarioContext;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class ApiSteps {

    private static Applicant applicant() {
        return ScenarioContext.applicant();
    }

    private static void remember(Response response) {
        ScenarioContext.setLastResponse(response);
    }

    private static Response last() {
        return ScenarioContext.lastResponse();
    }

    @Given("a verified applicant exists")
    public void verifiedApplicantExists() {
        LedgerlineApi.verifiedApplicant(applicant());
    }

    @Given("an approved applicant exists")
    public void approvedApplicantExists() {
        ScenarioContext.setApplicant(LedgerlineApi.decidedApplicant("GB", "software", "APPROVED"));
    }

    @Given("an applicant in manual review exists")
    public void manualReviewApplicantExists() {
        ScenarioContext.setApplicant(LedgerlineApi.decidedApplicant("AE", "crypto", "MANUAL_REVIEW"));
    }

    @When("I sign up through the API with a new work email")
    public void signUpNew() {
        Applicant a = applicant();
        remember(LedgerlineApi.signUp(a.email, a.firstName, a.lastName, a.companyName));
    }

    @When("I sign up through the API with {string} set to {string}")
    public void signUpWithField(String field, String value) {
        Applicant a = applicant();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", a.email);
        body.put("firstName", a.firstName);
        body.put("lastName", a.lastName);
        body.put("companyName", a.companyName);
        body.put(field, value);
        remember(LedgerlineApi.signUp(body.get("email"), body.get("firstName"), body.get("lastName"),
                body.get("companyName")));
    }

    @When("I request the application without a session")
    public void requestWithoutSession() {
        remember(LedgerlineApi.application(null));
    }

    @When("I log in through the API with the correct password")
    public void logInCorrect() {
        remember(LedgerlineApi.logIn(applicant().email, applicant().password));
    }

    @When("I log in through the API with the password {string}")
    public void logInWith(String password) {
        remember(LedgerlineApi.logIn(applicant().email, password));
    }

    @When("I submit the application through the API")
    public void submitThroughApi() {
        remember(LedgerlineApi.submit(applicant().session));
    }

    @When("I change the business details through the API")
    public void changeBusiness() {
        remember(LedgerlineApi.putBusiness(applicant().session, LedgerlineApi.business("IE", "654321", "retail")));
    }

    @When("I invite a team member through the API")
    public void inviteThroughApi() {
        remember(LedgerlineApi.inviteTeam(applicant().session,
                "first_name,last_name,email\nSara,Nolan,sara.nolan@" + Applicant.COMPANY_DOMAIN));
    }

    @Then("the response status is {int}")
    public void statusIs(int status) {
        Assert.assertEquals(last().statusCode(), status, "HTTP status. Body: " + last().asString());
    }

    @Then("the response message is {string}")
    public void messageIs(String message) {
        Assert.assertEquals(last().jsonPath().getString("message"), message);
    }

    @Then("the error for {string} is {string}")
    public void fieldErrorIs(String field, String message) {
        Assert.assertEquals(last().jsonPath().getString("errors." + field), message);
    }

    @Then("the missing steps are {string}")
    public void missingSteps(String steps) {
        List<String> missing = last().jsonPath().getList("missing", String.class);
        Assert.assertEquals(String.join(", ", missing), steps);
    }

    @Then("the applicant's inbox has {int} email(s)")
    public void inboxCount(int count) {
        int actual = given().queryParam("to", applicant().email).get("api/inbox")
                .then().statusCode(200).extract().jsonPath().getList("emails").size();
        Assert.assertEquals(actual, count);
    }
}
