package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.youssefmohamedymsh.kyc.pages.BeneficialOwnersPage;
import org.testng.Assert;

public class OwnersSteps {

    @When("I add these beneficial owners:")
    public void addOwners(DataTable owners) {
        new BeneficialOwnersPage().enterOwners(owners.asMaps());
    }

    @Then("the owners section shows {string}")
    public void ownersSectionShows(String message) {
        Assert.assertEquals(new BeneficialOwnersPage().sectionError(), message);
    }

    @Then("owner {int}'s {string} field shows {string}")
    public void ownerFieldShows(int ownerNumber, String field, String message) {
        Assert.assertEquals(new BeneficialOwnersPage().ownerFieldError(ownerNumber, field), message);
    }
}
