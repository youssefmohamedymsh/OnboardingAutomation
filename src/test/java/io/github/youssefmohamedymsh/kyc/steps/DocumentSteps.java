package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.java.en.When;
import io.github.youssefmohamedymsh.kyc.pages.IdentityDocumentPage;
import io.github.youssefmohamedymsh.kyc.support.TestFiles;

public class DocumentSteps {

    @When("I upload {string} as my {string}")
    public void uploadDocument(String fileName, String documentType) {
        new IdentityDocumentPage().uploadDocument(documentType, TestFiles.document(fileName));
    }
}
