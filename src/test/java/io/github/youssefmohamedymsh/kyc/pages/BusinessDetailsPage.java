package io.github.youssefmohamedymsh.kyc.pages;

public class BusinessDetailsPage extends BasePage {

    public void enter(String legalName, String country, String registrationNumber, String industry, String turnover) {
        type("legalName", legalName);
        choose("country", country);
        type("registrationNumber", registrationNumber);
        choose("industry", industry);
        choose("annualTurnover", turnover);
        click("business-submit");
    }
}
