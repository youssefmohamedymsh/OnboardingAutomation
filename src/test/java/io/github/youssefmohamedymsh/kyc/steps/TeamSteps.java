package io.github.youssefmohamedymsh.kyc.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.youssefmohamedymsh.kyc.pages.TeamInvitePage;
import io.github.youssefmohamedymsh.kyc.support.Applicant;
import io.github.youssefmohamedymsh.kyc.support.TestFiles;
import org.testng.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamSteps {

    /** Feature files write {domain} so the data stays readable; it becomes the applicant's company domain. */
    private static String withDomain(String text) {
        return text.replace("{domain}", Applicant.COMPANY_DOMAIN);
    }

    @When("I upload a team CSV with these rows:")
    public void uploadTeamCsv(DataTable rows) {
        String csv = rows.asLists().stream()
                .map(cells -> String.join(",", cells))
                .collect(Collectors.joining("\n"));
        new TeamInvitePage().uploadCsv(TestFiles.csv("team.csv", withDomain(csv)));
    }

    @When("I upload a CSV file containing:")
    public void uploadRawCsv(String content) {
        new TeamInvitePage().uploadCsv(TestFiles.csv("team.csv", content));
    }

    @Then("I see {string}")
    public void inviteSummary(String summary) {
        Assert.assertEquals(new TeamInvitePage().summary(), summary);
    }

    @Then("no rows are rejected")
    public void noRowsRejected() {
        Assert.assertTrue(new TeamInvitePage().rejectedRows().isEmpty());
    }

    @Then("these rows are rejected:")
    public void rowsRejected(DataTable expected) {
        List<Map<String, String>> want = expected.asMaps().stream()
                .map(row -> row.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> withDomain(e.getValue()))))
                .toList();
        List<Map<String, String>> actual = new TeamInvitePage().rejectedRows();
        Assert.assertEquals(actual.size(), want.size(), "Number of rejected rows");
        for (int i = 0; i < want.size(); i++) {
            for (String key : List.of("row", "email", "reason")) {
                Assert.assertEquals(actual.get(i).get(key), want.get(i).get(key), "Rejected row " + (i + 1) + " " + key);
            }
        }
    }
}
