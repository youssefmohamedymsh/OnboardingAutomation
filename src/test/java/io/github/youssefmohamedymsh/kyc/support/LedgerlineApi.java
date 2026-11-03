package io.github.youssefmohamedymsh.kyc.support;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Thin Rest-Assured client for the Ledgerline API. UI scenarios use it to set up state quickly
 * (sign-up, verification, earlier steps), so each scenario only drives the screen it is testing.
 */
public final class LedgerlineApi {

    private LedgerlineApi() {
    }

    // ---------- Raw calls (API scenarios assert on these) ----------

    public static Response signUp(String email, String firstName, String lastName, String companyName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("companyName", companyName);
        return json().body(body).post("api/signup");
    }

    public static Response verify(String token, String password) {
        return json().body(Map.of("token", token, "password", password)).post("api/verify");
    }

    public static Response logIn(String email, String password) {
        return json().body(Map.of("email", email, "password", password)).post("api/login");
    }

    public static Response application(String session) {
        return auth(session).get("api/application");
    }

    public static Response putBusiness(String session, Map<String, Object> business) {
        return auth(session).body(business).put("api/application/business");
    }

    public static Response putOwners(String session, List<Map<String, Object>> owners) {
        return auth(session).body(Map.of("owners", owners)).put("api/application/owners");
    }

    public static Response putDocument(String session, String type, String fileName, String mimeType, byte[] content) {
        return auth(session).body(Map.of(
                "documentType", type,
                "fileName", fileName,
                "mimeType", mimeType,
                "contentBase64", Base64.getEncoder().encodeToString(content))).put("api/application/document");
    }

    public static Response submit(String session) {
        return auth(session).post("api/application/submit");
    }

    public static Response inviteTeam(String session, String csv) {
        return auth(session).body(Map.of("csv", csv)).post("api/team/invite");
    }

    // ---------- Setup shortcuts ----------

    /** Signs the applicant up and returns the verification link from their inbox. */
    public static String signUpAndGetLink(Applicant a) {
        signUp(a.email, a.firstName, a.lastName, a.companyName).then().statusCode(201);
        a.verificationLink = latestVerificationLink(a.email);
        return a.verificationLink;
    }

    public static String latestVerificationLink(String email) {
        return given().queryParam("to", email).get("api/inbox")
                .then().statusCode(200)
                .extract().path("emails[0].link");
    }

    public static String tokenFrom(String link) {
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    /** Signs up and verifies the applicant; stores and returns their session. */
    public static String verifiedApplicant(Applicant a) {
        String link = signUpAndGetLink(a);
        a.session = verify(tokenFrom(link), a.password).then().statusCode(200).extract().path("session");
        return a.session;
    }

    public static Map<String, Object> business(String country, String registrationNumber, String industry) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("legalName", "Acme Demo Ltd");
        b.put("country", country);
        b.put("registrationNumber", registrationNumber);
        b.put("industry", industry);
        b.put("annualTurnover", "under_1m");
        return b;
    }

    public static Map<String, Object> owner(String fullName, String dateOfBirth, String nationality, int ownership) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("fullName", fullName);
        o.put("dateOfBirth", dateOfBirth);
        o.put("nationality", nationality);
        o.put("ownership", ownership);
        return o;
    }

    /** Completes every step through the API. Nothing is submitted, so callers decide when to submit. */
    public static void completeApplication(Applicant a, String country, String industry, String ownerName,
                                           String documentType) {
        putBusiness(a.session, business(country, sampleRegistrationNumber(country), industry))
                .then().statusCode(200);
        putOwners(a.session, List.of(owner(ownerName, "1988-03-14", "GB", 100))).then().statusCode(200);
        putDocument(a.session, documentType, "id.pdf", "application/pdf",
                "%PDF-1.4 test".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).then().statusCode(200);
    }

    /** Creates a verified applicant whose submitted application got the expected decision. */
    public static Applicant decidedApplicant(String country, String industry, String expectedStatus) {
        Applicant a = Applicant.defaultApplicant();
        verifiedApplicant(a);
        completeApplication(a, country, industry, "Maya Brook", "passport");
        submit(a.session).then().statusCode(200).body("status", org.hamcrest.Matchers.equalTo(expectedStatus));
        return a;
    }

    /** A valid registration number for each supported country. */
    public static String sampleRegistrationNumber(String country) {
        return switch (country) {
            case "GB" -> "12345678";
            case "IE" -> "654321";
            case "DE" -> "HRB 12345";
            case "AE" -> "CN-1234567";
            default -> throw new IllegalArgumentException("Unsupported country " + country);
        };
    }

    private static RequestSpecification json() {
        return given().contentType(ContentType.JSON);
    }

    private static RequestSpecification auth(String session) {
        RequestSpecification spec = json();
        return session == null ? spec : spec.header("Authorization", "Bearer " + session);
    }
}
