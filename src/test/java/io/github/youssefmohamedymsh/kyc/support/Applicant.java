package io.github.youssefmohamedymsh.kyc.support;

import java.util.UUID;

/** The person going through onboarding in the current scenario. Every applicant gets a unique work email. */
public final class Applicant {

    public static final String COMPANY_DOMAIN = "acme-demo.co.uk";
    public static final String DEFAULT_PASSWORD = "Ledger!line2026x";

    public final String firstName;
    public final String lastName;
    public final String companyName;
    public final String email;
    public String password = DEFAULT_PASSWORD;
    public String session;
    public String verificationLink;

    public Applicant(String firstName, String lastName, String companyName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.companyName = companyName;
        this.email = uniqueWorkEmail(firstName);
    }

    public static Applicant defaultApplicant() {
        return new Applicant("Maya", "Brook", "Acme Demo Ltd");
    }

    public static String uniqueWorkEmail(String firstName) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return firstName.toLowerCase().replaceAll("[^a-z]", "") + "." + suffix + "@" + COMPANY_DOMAIN;
    }
}
