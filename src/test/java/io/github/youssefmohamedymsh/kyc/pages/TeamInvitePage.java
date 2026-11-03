package io.github.youssefmohamedymsh.kyc.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TeamInvitePage extends BasePage {

    public void uploadCsv(Path file) {
        upload("team-file", file);
        click("team-submit");
    }

    public String summary() {
        return textOf("invite-summary");
    }

    /** Rejected rows as row/email/reason maps, in the order shown. */
    public List<Map<String, String>> rejectedRows() {
        summary();
        return all("rejected-row").stream().map(tr -> {
            List<WebElement> cells = tr.findElements(By.tagName("td"));
            Map<String, String> row = new LinkedHashMap<>();
            row.put("row", cells.get(0).getText());
            row.put("email", cells.get(1).getText());
            row.put("reason", cells.get(2).getText());
            return row;
        }).toList();
    }
}
