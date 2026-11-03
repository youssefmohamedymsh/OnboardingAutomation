package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.github.youssefmohamedymsh.kyc.pages.BasePage;
import io.github.youssefmohamedymsh.kyc.support.Hooks;
import io.github.youssefmohamedymsh.kyc.support.ScenarioContext;
import org.testng.Assert;

/** Navigation and message checks that apply to every screen. */
public class CommonSteps {

    /** Any page: the shared helpers are all these steps need. */
    private static final class AnyPage extends BasePage {
    }

    @Given("I have opened the {string} page")
    public void openPage(String page) {
        Hooks.openAsLoggedIn(ScenarioContext.applicant(), page);
    }

    @Then("I am on the {string} step")
    public void onStep(String heading) {
        AnyPage page = new AnyPage();
        page.wait.until(d -> page.heading().equals(heading));
    }

    @Then("the {string} field shows {string}")
    public void fieldShows(String field, String message) {
        Assert.assertEquals(new AnyPage().fieldError(field), message);
    }

    @Then("I see the error {string}")
    public void formErrorShows(String message) {
        Assert.assertEquals(new AnyPage().formError(), message);
    }
}
