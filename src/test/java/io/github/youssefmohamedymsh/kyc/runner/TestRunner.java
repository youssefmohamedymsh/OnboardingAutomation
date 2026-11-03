package io.github.youssefmohamedymsh.kyc.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Runs every feature. Narrow it from the command line, for example:
 *   mvn verify -Dcucumber.filter.tags="@api"
 *   mvn verify -Dcucumber.filter.tags="@screening and not @slow"
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {
                "io.github.youssefmohamedymsh.kyc.steps",
                "io.github.youssefmohamedymsh.kyc.support"
        },
        plugin = {
                "pretty",
                "summary",
                "html:target/cucumber.html",
                "json:target/cucumber.json",
                "junit:target/cucumber.xml",
                "rerun:target/rerun.txt"
        }
)
public class TestRunner extends AbstractTestNGCucumberTests {
}
