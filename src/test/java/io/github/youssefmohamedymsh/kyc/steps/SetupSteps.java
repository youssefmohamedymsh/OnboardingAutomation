package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.java.en.Given;
import io.github.youssefmohamedymsh.kyc.support.Applicant;
import io.github.youssefmohamedymsh.kyc.support.LedgerlineApi;
import io.github.youssefmohamedymsh.kyc.support.ScenarioContext;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Fast preconditions through the API. A scenario about the owners screen shouldn't spend
 * a minute clicking through sign-up first, and shouldn't fail because sign-up broke.
 */
public class SetupSteps {

    private Applicant applicant() {
        return ScenarioContext.applicant();
    }

    @Given("I am a verified applicant")
    public void verifiedApplicant() {
        LedgerlineApi.verifiedApplicant(applicant());
    }

    @Given("my business details are complete")
    public void businessComplete() {
        businessRegisteredIn("GB", "software");
    }

    @Given("my business is registered in {string} in the {string} industry")
    public void businessRegisteredIn(String country, String industry) {
        LedgerlineApi.putBusiness(applicant().session,
                        LedgerlineApi.business(country, LedgerlineApi.sampleRegistrationNumber(country), industry))
                .then().statusCode(200);
    }

    @Given("my beneficial owners are complete")
    public void ownersComplete() {
        onlyOwner("Maya Brook");
    }

    @Given("{string} is the only beneficial owner")
    public void onlyOwner(String fullName) {
        LedgerlineApi.putOwners(applicant().session, List.of(LedgerlineApi.owner(fullName, "1988-03-14", "GB", 100)))
                .then().statusCode(200);
    }

    @Given("my {string} is uploaded")
    public void documentUploaded(String documentType) {
        LedgerlineApi.putDocument(applicant().session, documentType, "id.pdf", "application/pdf",
                "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII)).then().statusCode(200);
    }

    @Given("my business has been approved")
    public void businessApproved() {
        ScenarioContext.setApplicant(LedgerlineApi.decidedApplicant("GB", "software", "APPROVED"));
    }
}
