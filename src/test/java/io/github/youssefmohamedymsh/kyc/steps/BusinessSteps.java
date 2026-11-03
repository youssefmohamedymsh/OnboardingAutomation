package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.java.en.When;
import io.github.youssefmohamedymsh.kyc.pages.BusinessDetailsPage;

public class BusinessSteps {

    @When("I enter my business details for {string} with registration number {string}")
    public void enterBusiness(String country, String registrationNumber) {
        new BusinessDetailsPage().enter("Acme Demo Ltd", country, registrationNumber, "software", "under_1m");
    }
}
