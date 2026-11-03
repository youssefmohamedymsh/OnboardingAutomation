package io.github.youssefmohamedymsh.kyc.support;

import io.restassured.response.Response;

/**
 * State shared between step classes during one scenario. Scenarios run one at a time,
 * and Hooks resets this before each one.
 */
public final class ScenarioContext {

    private static Applicant applicant;
    private static Response lastResponse;

    private ScenarioContext() {
    }

    public static Applicant applicant() {
        if (applicant == null) {
            applicant = Applicant.defaultApplicant();
        }
        return applicant;
    }

    public static void setApplicant(Applicant a) {
        applicant = a;
    }

    public static Response lastResponse() {
        return lastResponse;
    }

    public static void setLastResponse(Response response) {
        lastResponse = response;
    }

    public static void reset() {
        applicant = null;
        lastResponse = null;
    }
}
