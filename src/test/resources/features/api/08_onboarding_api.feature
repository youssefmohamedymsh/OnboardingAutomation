@api
Feature: Onboarding API rules
  The same rules hold when the API is called directly, which is how partner integrations
  and the mobile app use it. These scenarios need no browser.

  Scenario: Sign-up returns 201 and sends exactly one verification email
    When I sign up through the API with a new work email
    Then the response status is 201
    And the applicant's inbox has 1 email

  Scenario Outline: Sign-up validation errors are returned per field (<field>)
    When I sign up through the API with "<field>" set to "<value>"
    Then the response status is 422
    And the error for "<field>" is "<message>"

    Examples:
      | field       | value       | message                                    |
      | email       | not-email   | Enter a valid email address                |
      | firstName   | M4ya        | Enter your first name using letters only   |
      | companyName |             | Enter your company name                    |

  Scenario: The application can't be read without a session
    When I request the application without a session
    Then the response status is 401
    And the response message is "Your session has ended. Log in again."

  Scenario: A verified applicant can log in again; a wrong password is refused
    Given a verified applicant exists
    When I log in through the API with the correct password
    Then the response status is 200
    When I log in through the API with the password "Wrong!Password1"
    Then the response status is 401
    And the response message is "Email or password is incorrect"

  Scenario: Submitting too early lists every missing step
    Given a verified applicant exists
    When I submit the application through the API
    Then the response status is 409
    And the missing steps are "business, owners, document"

  Scenario: A decided application is locked against changes
    Given an approved applicant exists
    When I change the business details through the API
    Then the response status is 409
    And the response message is "This application has been submitted and can no longer be changed"

  Scenario: Team invites stay locked until the business is approved
    Given an applicant in manual review exists
    When I invite a team member through the API
    Then the response status is 403
    And the response message is "You can invite your team once your business is approved"
