@ui @signup
Feature: Sign up with a work email
  A business account starts with the applicant's work email. Personal mailboxes are refused,
  because the email domain is later used to check who can be invited to the account.

  Background:
    Given I am on the sign-up page

  Scenario: A new applicant is asked to confirm their email
    When I sign up as "Maya Brook" from "Acme Demo Ltd" with a new work email
    Then I am asked to check my inbox
    And my inbox has an email titled "Verify your email to continue your Ledgerline application"

  Scenario Outline: Personal email addresses are refused (<email>)
    When I sign up as "Maya Brook" from "Acme Demo Ltd" with the email "<email>"
    Then the "email" field shows "Use your work email address, not a personal one"

    Examples:
      | email                 |
      | maya.brook@gmail.com  |
      | maya.brook@yahoo.com  |
      | maya.brook@outlook.com |

  Scenario: The same email can't start two applications
    Given someone has already signed up with my work email
    When I sign up as "Maya Brook" from "Acme Demo Ltd" with that same email
    Then I see the error "An application already exists for this email. Log in instead."
