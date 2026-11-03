package io.github.youssefmohamedymsh.kyc.pages;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

public class BeneficialOwnersPage extends BasePage {

    /** Fills one owner card per row, adding cards as needed, then saves. */
    public void enterOwners(List<Map<String, String>> owners) {
        visible("owner-0");
        for (int i = 0; i < owners.size(); i++) {
            if (i > 0) {
                click("add-owner");
            }
            Map<String, String> o = owners.get(i);
            type("owner-" + i + "-fullName", o.get("fullName"));
            setDate("owner-" + i + "-dateOfBirth", o.get("dateOfBirth"));
            type("owner-" + i + "-nationality", o.get("nationality"));
            type("owner-" + i + "-ownership", o.get("ownership"));
        }
        click("owners-submit");
    }

    public String ownerFieldError(int ownerNumber, String field) {
        return fieldError("owners[" + (ownerNumber - 1) + "]." + field);
    }

    public String sectionError() {
        return fieldError("owners");
    }

    /** Date inputs ignore typed text in some locales; setting the value directly is locale-proof. */
    private void setDate(String id, String isoDate) {
        WebElement input = visible(id);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
                input, isoDate);
    }
}
