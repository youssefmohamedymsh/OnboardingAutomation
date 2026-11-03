package io.github.youssefmohamedymsh.kyc.pages;

import java.nio.file.Path;

public class IdentityDocumentPage extends BasePage {

    public void uploadDocument(String documentType, Path file) {
        choose("documentType", documentType);
        upload("document-file", file);
        click("document-submit");
    }
}
