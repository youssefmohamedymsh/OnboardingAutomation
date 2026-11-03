package io.github.youssefmohamedymsh.ledgerline;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Business rules of the Ledgerline demo onboarding: sign-up, email verification, KYB/KYC data capture,
 * sanctions/PEP screening, risk scoring and team invites. All state is in memory.
 *
 * Every rule here is fictional and deliberately simple, but shaped like the checks a real business-banking
 * onboarding performs, so the test suite has realistic behaviour to exercise.
 */
public final class Onboarding {

    // ---------- Reference data ----------

    static final Set<String> FREE_EMAIL_DOMAINS = Set.of(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "icloud.com", "proton.me", "aol.com");

    /** Company registration number formats by country (ISO 3166-1 alpha-2). */
    static final Map<String, Pattern> REGISTRATION_FORMATS = Map.of(
            "GB", Pattern.compile("^(\\d{8}|(SC|NI|OC)\\d{6})$"),
            "IE", Pattern.compile("^\\d{6}$"),
            "DE", Pattern.compile("^HR[AB] \\d{1,6}$"),
            "AE", Pattern.compile("^CN-\\d{7}$"));

    static final Map<String, String> REGISTRATION_HINTS = Map.of(
            "GB", "8 digits, or SC/NI/OC followed by 6 digits",
            "IE", "6 digits",
            "DE", "HRA or HRB, a space, then up to 6 digits",
            "AE", "CN- followed by 7 digits");

    static final Map<String, Integer> COUNTRY_RISK = Map.of("GB", 10, "IE", 10, "DE", 10, "AE", 25);

    static final Map<String, Integer> INDUSTRY_RISK = Map.of(
            "software", 0, "retail", 5, "consulting", 5,
            "money_services", 35, "crypto", 40, "gambling", 40);

    static final Map<String, Integer> TURNOVER_RISK = Map.of("under_1m", 0, "1m_10m", 5, "over_10m", 10);

    static final Set<String> DOCUMENT_TYPES = Set.of("passport", "national_id", "driving_licence");
    static final Set<String> DOCUMENT_MIME_TYPES = Set.of("application/pdf", "image/png", "image/jpeg");
    static final int MAX_DOCUMENT_BYTES = 5 * 1024 * 1024;

    /** Fictional test watchlists. Names are matched case-insensitively after trimming. */
    static final Set<String> SANCTIONS_LIST = Set.of("viktor kraal", "anika voss-reyes");
    static final Set<String> PEP_LIST = Set.of("elena marsh", "tomas idris");

    static final int MANUAL_REVIEW_THRESHOLD = 50;
    static final Duration VERIFICATION_LINK_TTL = Duration.ofMinutes(30);
    static final int MAX_INVITES_PER_FILE = 200;

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PERSON_NAME = Pattern.compile("^[\\p{L}][\\p{L} '\\-]{0,59}$");
    private static final Pattern COUNTRY_CODE = Pattern.compile("^[A-Z]{2}$");

    // ---------- State ----------

    private final Clock clock;
    private final Map<String, Application> applicationsById = new ConcurrentHashMap<>();
    private final Map<String, String> applicationIdByEmail = new ConcurrentHashMap<>();
    private final Map<String, VerificationLink> verificationLinks = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final List<Email> outbox = new ArrayList<>();

    public Onboarding(Clock clock) {
        this.clock = clock;
    }

    // ---------- Model ----------

    public enum Status { DRAFT, APPROVED, MANUAL_REVIEW, REJECTED }

    public static final class Application {
        public final String id = UUID.randomUUID().toString();
        public String email;
        public String firstName;
        public String lastName;
        public String companyName;
        public boolean emailVerified;
        public String passwordHash;
        public Map<String, Object> business;
        public List<Map<String, Object>> owners;
        public Map<String, Object> document;
        public Status status = Status.DRAFT;
        public Integer riskScore;
        public List<String> decisionReasons = new ArrayList<>();
        public final List<Map<String, String>> team = new ArrayList<>();

        public String emailDomain() {
            return email.substring(email.indexOf('@') + 1);
        }
    }

    public record Email(String to, String subject, String body, String link, String sentAt) {
    }

    private record VerificationLink(String applicationId, Instant expiresAt, boolean used) {
    }

    /** A rule violation, mapped to an HTTP status by the server. */
    public static final class OnboardingException extends RuntimeException {
        public final int status;
        public final Map<String, Object> body;

        OnboardingException(int status, String message, Map<String, ?> details) {
            super(message);
            this.status = status;
            this.body = new LinkedHashMap<>();
            this.body.put("message", message);
            if (details != null) {
                this.body.putAll(details);
            }
        }

        static OnboardingException of(int status, String message) {
            return new OnboardingException(status, message, null);
        }

        static OnboardingException validation(Map<String, String> fieldErrors) {
            return new OnboardingException(422, "Some details need fixing", Map.of("errors", fieldErrors));
        }
    }

    // ---------- 1. Sign-up ----------

    public synchronized Application signUp(String email, String firstName, String lastName, String companyName) {
        Map<String, String> errors = new LinkedHashMap<>();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalizedEmail).matches()) {
            errors.put("email", "Enter a valid email address");
        } else if (FREE_EMAIL_DOMAINS.contains(normalizedEmail.substring(normalizedEmail.indexOf('@') + 1))) {
            errors.put("email", "Use your work email address, not a personal one");
        }
        requireName(errors, "firstName", firstName, "first name");
        requireName(errors, "lastName", lastName, "last name");
        if (isBlank(companyName)) {
            errors.put("companyName", "Enter your company name");
        }
        if (!errors.isEmpty()) {
            throw OnboardingException.validation(errors);
        }
        if (applicationIdByEmail.containsKey(normalizedEmail)) {
            throw OnboardingException.of(409, "An application already exists for this email. Log in instead.");
        }

        Application app = new Application();
        app.email = normalizedEmail;
        app.firstName = firstName.trim();
        app.lastName = lastName.trim();
        app.companyName = companyName.trim();
        applicationsById.put(app.id, app);
        applicationIdByEmail.put(normalizedEmail, app.id);
        sendVerificationEmail(app);
        return app;
    }

    private void sendVerificationEmail(Application app) {
        String token = UUID.randomUUID().toString().replace("-", "");
        verificationLinks.put(token, new VerificationLink(app.id, clock.instant().plus(VERIFICATION_LINK_TTL), false));
        String link = "/verify.html?token=" + token;
        outbox.add(new Email(app.email, "Verify your email to continue your Ledgerline application",
                "Hi " + app.firstName + ", confirm your email to continue setting up " + app.companyName
                        + ". This link expires in 30 minutes.",
                link, clock.instant().toString()));
    }

    public synchronized List<Email> inbox(String to) {
        String normalized = to == null ? "" : to.trim().toLowerCase(Locale.ROOT);
        List<Email> mine = new ArrayList<>(outbox.stream().filter(e -> e.to().equals(normalized)).toList());
        java.util.Collections.reverse(mine); // newest first
        return mine;
    }

    // ---------- 2. Email verification and password ----------

    public synchronized String verifyEmail(String token, String password) {
        VerificationLink link = verificationLinks.get(token == null ? "" : token);
        if (link == null) {
            throw OnboardingException.of(404, "This verification link is not valid");
        }
        if (link.used()) {
            throw OnboardingException.of(410, "This link has already been used. Log in instead.");
        }
        if (clock.instant().isAfter(link.expiresAt())) {
            throw OnboardingException.of(410, "This link has expired. Request a new one.");
        }
        Application app = applicationsById.get(link.applicationId());
        List<String> failed = passwordRuleFailures(password, app.email);
        if (!failed.isEmpty()) {
            throw new OnboardingException(422, "Choose a stronger password", Map.of("failedRules", failed));
        }
        verificationLinks.put(token, new VerificationLink(link.applicationId(), link.expiresAt(), true));
        app.emailVerified = true;
        app.passwordHash = sha256(password);
        return openSession(app);
    }

    static List<String> passwordRuleFailures(String password, String email) {
        List<String> failed = new ArrayList<>();
        String p = password == null ? "" : password;
        if (p.length() < 12) failed.add("At least 12 characters");
        if (!p.matches(".*[A-Z].*")) failed.add("An uppercase letter");
        if (!p.matches(".*[a-z].*")) failed.add("A lowercase letter");
        if (!p.matches(".*\\d.*")) failed.add("A number");
        if (!p.matches(".*[^A-Za-z0-9].*")) failed.add("A symbol");
        String localPart = email.substring(0, email.indexOf('@'));
        if (localPart.length() >= 3 && p.toLowerCase(Locale.ROOT).contains(localPart)) {
            failed.add("Must not contain your email name");
        }
        return failed;
    }

    public synchronized String logIn(String email, String password) {
        String id = applicationIdByEmail.get(email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
        Application app = id == null ? null : applicationsById.get(id);
        if (app == null || !app.emailVerified || !sha256(password == null ? "" : password).equals(app.passwordHash)) {
            throw OnboardingException.of(401, "Email or password is incorrect");
        }
        return openSession(app);
    }

    private String openSession(Application app) {
        String session = UUID.randomUUID().toString();
        sessions.put(session, app.id);
        return session;
    }

    public Application forSession(String session) {
        String id = session == null ? null : sessions.get(session);
        if (id == null) {
            throw OnboardingException.of(401, "Your session has ended. Log in again.");
        }
        return applicationsById.get(id);
    }

    // ---------- 3. Business details (KYB) ----------

    public synchronized void saveBusiness(Application app, Map<String, Object> input) {
        requireDraft(app);
        Map<String, String> errors = new LinkedHashMap<>();
        String legalName = str(input, "legalName");
        String country = str(input, "country").toUpperCase(Locale.ROOT);
        String registrationNumber = str(input, "registrationNumber").toUpperCase(Locale.ROOT);
        String industry = str(input, "industry");
        String turnover = str(input, "annualTurnover");

        if (isBlank(legalName)) errors.put("legalName", "Enter the registered legal name");
        if (!REGISTRATION_FORMATS.containsKey(country)) {
            errors.put("country", "Ledgerline can onboard businesses in GB, IE, DE and AE");
        } else if (!REGISTRATION_FORMATS.get(country).matcher(registrationNumber).matches()) {
            errors.put("registrationNumber", "Registration numbers in " + country + " are "
                    + REGISTRATION_HINTS.get(country));
        }
        if (!INDUSTRY_RISK.containsKey(industry)) errors.put("industry", "Choose an industry");
        if (!TURNOVER_RISK.containsKey(turnover)) errors.put("annualTurnover", "Choose an annual turnover");
        if (!errors.isEmpty()) throw OnboardingException.validation(errors);

        Map<String, Object> business = new LinkedHashMap<>();
        business.put("legalName", legalName.trim());
        business.put("country", country);
        business.put("registrationNumber", registrationNumber);
        business.put("industry", industry);
        business.put("annualTurnover", turnover);
        app.business = business;
    }

    // ---------- 4. Beneficial owners (UBOs) ----------

    public synchronized void saveOwners(Application app, List<Map<String, Object>> owners) {
        requireDraft(app);
        Map<String, String> errors = new LinkedHashMap<>();
        if (owners == null || owners.isEmpty()) {
            throw OnboardingException.validation(Map.of("owners", "Add at least one beneficial owner"));
        }
        if (owners.size() > 4) {
            throw OnboardingException.validation(Map.of("owners", "A business can have at most 4 owners with 25% or more"));
        }
        int total = 0;
        LocalDate today = LocalDate.now(clock);
        List<Map<String, Object>> cleaned = new ArrayList<>();
        for (int i = 0; i < owners.size(); i++) {
            Map<String, Object> o = owners.get(i);
            String prefix = "owners[" + i + "].";
            String fullName = str(o, "fullName");
            String nationality = str(o, "nationality").toUpperCase(Locale.ROOT);
            String dob = str(o, "dateOfBirth");
            int ownership;
            try {
                ownership = Integer.parseInt(str(o, "ownership"));
            } catch (NumberFormatException e) {
                ownership = -1;
            }

            if (!PERSON_NAME.matcher(fullName.trim()).matches() || !fullName.trim().contains(" ")) {
                errors.put(prefix + "fullName", "Enter the owner's full name as shown on their ID");
            }
            if (!COUNTRY_CODE.matcher(nationality).matches()) {
                errors.put(prefix + "nationality", "Enter a 2-letter country code, like GB");
            }
            try {
                LocalDate birth = LocalDate.parse(dob);
                if (Period.between(birth, today).getYears() < 18) {
                    errors.put(prefix + "dateOfBirth", "Beneficial owners must be 18 or older");
                }
            } catch (DateTimeParseException e) {
                errors.put(prefix + "dateOfBirth", "Enter a date like 1985-04-23");
            }
            if (ownership < 25 || ownership > 100) {
                errors.put(prefix + "ownership", "Only list owners with 25% to 100% ownership");
            } else {
                total += ownership;
            }

            Map<String, Object> clean = new LinkedHashMap<>();
            clean.put("fullName", fullName.trim());
            clean.put("nationality", nationality);
            clean.put("dateOfBirth", dob);
            clean.put("ownership", ownership);
            cleaned.add(clean);
        }
        if (total > 100) {
            errors.put("owners", "Ownership adds up to " + total + "%. It can't be more than 100%.");
        }
        if (!errors.isEmpty()) throw OnboardingException.validation(errors);
        app.owners = cleaned;
    }

    // ---------- 5. Identity document ----------

    public synchronized void saveDocument(Application app, Map<String, Object> input) {
        requireDraft(app);
        String type = str(input, "documentType");
        String mimeType = str(input, "mimeType");
        String fileName = str(input, "fileName");
        byte[] content;
        try {
            content = Base64.getDecoder().decode(str(input, "contentBase64"));
        } catch (IllegalArgumentException e) {
            content = new byte[0];
        }

        if (!DOCUMENT_TYPES.contains(type)) {
            throw OnboardingException.validation(Map.of("documentType", "Choose the type of ID"));
        }
        if (!DOCUMENT_MIME_TYPES.contains(mimeType)) {
            throw OnboardingException.validation(Map.of("file", "Upload a PDF, PNG or JPG file"));
        }
        if (content.length == 0) {
            throw OnboardingException.validation(Map.of("file", "This file is empty"));
        }
        if (content.length > MAX_DOCUMENT_BYTES) {
            throw OnboardingException.validation(Map.of("file", "Files must be 5 MB or smaller"));
        }
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("documentType", type);
        document.put("fileName", fileName);
        document.put("mimeType", mimeType);
        document.put("sizeBytes", content.length);
        app.document = document;
    }

    // ---------- 6. Submit: screening, risk score, decision ----------

    public synchronized Application submit(Application app) {
        if (app.status != Status.DRAFT) {
            throw OnboardingException.of(409, "This application has already been submitted");
        }
        List<String> missing = new ArrayList<>();
        if (app.business == null) missing.add("business");
        if (app.owners == null) missing.add("owners");
        if (app.document == null) missing.add("document");
        if (!missing.isEmpty()) {
            throw new OnboardingException(409, "Finish every step before submitting", Map.of("missing", missing));
        }

        List<String> reasons = new ArrayList<>();
        boolean sanctionsHit = false;
        boolean pepHit = false;
        for (Map<String, Object> owner : app.owners) {
            String name = owner.get("fullName").toString().trim().toLowerCase(Locale.ROOT);
            if (SANCTIONS_LIST.contains(name)) {
                sanctionsHit = true;
                reasons.add("Sanctions match: " + owner.get("fullName"));
            }
            if (PEP_LIST.contains(name)) {
                pepHit = true;
                reasons.add("Politically exposed person: " + owner.get("fullName"));
            }
        }

        int score = COUNTRY_RISK.get(app.business.get("country").toString())
                + INDUSTRY_RISK.get(app.business.get("industry").toString())
                + TURNOVER_RISK.get(app.business.get("annualTurnover").toString())
                + (pepHit ? 30 : 0)
                + ("driving_licence".equals(app.document.get("documentType")) ? 5 : 0);
        app.riskScore = Math.min(score, 100);

        if (sanctionsHit) {
            app.status = Status.REJECTED;
        } else if (pepHit) {
            app.status = Status.MANUAL_REVIEW;
        } else if (app.riskScore >= MANUAL_REVIEW_THRESHOLD) {
            app.status = Status.MANUAL_REVIEW;
            reasons.add("Risk score " + app.riskScore + " is at or above " + MANUAL_REVIEW_THRESHOLD);
        } else {
            app.status = Status.APPROVED;
        }
        app.decisionReasons = reasons;
        return app;
    }

    // ---------- 7. Team invites (CSV) ----------

    public synchronized Map<String, Object> inviteTeam(Application app, String csv) {
        if (app.status != Status.APPROVED) {
            throw OnboardingException.of(403, "You can invite your team once your business is approved");
        }
        List<String> lines = (csv == null ? "" : csv).lines().map(String::trim).filter(l -> !l.isEmpty()).toList();
        if (lines.isEmpty() || !lines.get(0).equalsIgnoreCase("first_name,last_name,email")) {
            throw OnboardingException.validation(Map.of("file",
                    "The first row must be exactly: first_name,last_name,email"));
        }
        if (lines.size() - 1 > MAX_INVITES_PER_FILE) {
            throw OnboardingException.validation(Map.of("file", "Upload at most " + MAX_INVITES_PER_FILE + " people at a time"));
        }

        Set<String> seen = new HashSet<>();
        app.team.forEach(member -> seen.add(member.get("email")));
        List<Map<String, Object>> rejected = new ArrayList<>();
        int invited = 0;
        for (int i = 1; i < lines.size(); i++) {
            int row = i + 1;
            String[] cells = lines.get(i).split(",", -1);
            String first = cells.length > 0 ? cells[0].trim() : "";
            String last = cells.length > 1 ? cells[1].trim() : "";
            String email = cells.length > 2 ? cells[2].trim().toLowerCase(Locale.ROOT) : "";

            String reason = null;
            if (cells.length != 3) {
                reason = "Expected 3 columns";
            } else if (!PERSON_NAME.matcher(first).matches() || !PERSON_NAME.matcher(last).matches()) {
                reason = "Names can only contain letters, spaces, hyphens and apostrophes";
            } else if (!EMAIL.matcher(email).matches()) {
                reason = "Not a valid email address";
            } else if (!email.endsWith("@" + app.emailDomain())) {
                reason = "Must be a work email at @" + app.emailDomain();
            } else if (!seen.add(email)) {
                reason = "Duplicate email";
            }

            if (reason != null) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("row", row);
                r.put("email", email);
                r.put("reason", reason);
                rejected.add(r);
            } else {
                app.team.add(Map.of("firstName", first, "lastName", last, "email", email));
                invited++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invited", invited);
        result.put("rejected", rejected);
        return result;
    }

    // ---------- Views ----------

    public Map<String, Object> view(Application app) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", app.id);
        v.put("email", app.email);
        v.put("firstName", app.firstName);
        v.put("lastName", app.lastName);
        v.put("companyName", app.companyName);
        v.put("emailVerified", app.emailVerified);
        v.put("status", app.status);
        Map<String, Boolean> steps = new LinkedHashMap<>();
        steps.put("business", app.business != null);
        steps.put("owners", app.owners != null);
        steps.put("document", app.document != null);
        v.put("completedSteps", steps);
        v.put("business", app.business);
        v.put("owners", app.owners);
        v.put("document", app.document);
        v.put("riskScore", app.riskScore);
        v.put("decisionReasons", app.decisionReasons);
        v.put("teamSize", app.team.size());
        return v;
    }

    // ---------- Helpers ----------

    private static void requireDraft(Application app) {
        if (app.status != Status.DRAFT) {
            throw OnboardingException.of(409, "This application has been submitted and can no longer be changed");
        }
    }

    private static void requireName(Map<String, String> errors, String field, String value, String label) {
        if (value == null || !PERSON_NAME.matcher(value.trim()).matches()) {
            errors.put(field, "Enter your " + label + " using letters only");
        }
    }

    private static String str(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        return v == null ? "" : v.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
