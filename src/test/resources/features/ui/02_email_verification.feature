@ui @verification
Feature: Confirm the email and create a password
  The link in the verification email confirms the address and lets the applicant set a password
  that meets the security policy. Each link works once.

  Background:
    Given I have signed up with a new work email
    And I open the verification link from my inbox

  Scenario: A strong password confirms the email and opens the application
    When I create the password "Ledger!line2026x"
    Then I am on the "About your business" step

  Scenario Outline: Weak passwords are refused with the rule they break (<rule>)
    When I create the password "<password>"
    Then I am told the password needs "<rule>"

    Examples:
      | password         | rule                   |
      | Sh0rt!pass       | At least 12 characters |
      | alllowercase123! | An uppercase letter    |
      | NoNumbersHere!!  | A number               |
      | NoSymbols123456  | A symbol               |

  Scenario: The confirmation must match
    When I create the password "Ledger!line2026x" but confirm it as "Ledger!line2026y"
    Then the "confirmPassword" field shows "Passwords don't match"

  Scenario: A verification link can only be used once
    Given I have already created my password with this link
    When I open the verification link from my inbox
    And I create the password "Another!Pass2026"
    Then I see the error "This link has already been used. Log in instead."
