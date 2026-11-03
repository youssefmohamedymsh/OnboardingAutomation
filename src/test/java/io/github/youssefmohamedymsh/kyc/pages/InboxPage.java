package io.github.youssefmohamedymsh.kyc.pages;

import io.github.youssefmohamedymsh.kyc.support.TestEnvironment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** The demo mailbox that stands in for a real inbox (the original suite used a hosted test mailbox). */
public class InboxPage extends BasePage {

    public InboxPage openFor(String email) {
        driver.get(TestEnvironment.url("inbox.html?to=" + URLEncoder.encode(email, StandardCharsets.UTF_8)));
        return this;
    }

    public String latestSubject() {
        return textOf("email-subject");
    }

    public void openLatestVerificationLink() {
        visible("email-item");
        click("verify-link");
    }
}
