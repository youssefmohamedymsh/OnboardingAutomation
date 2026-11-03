@ui @kyb
Feature: Business details (KYB)
  The registration number must match the company-register format of the country the
  business is registered in.

  Background:
    Given I am a verified applicant
    And I have opened the "business.html" page

  Scenario Outline: Valid registration numbers are accepted (<country> <number>)
    When I enter my business details for "<country>" with registration number "<number>"
    Then I am on the "Beneficial owners" step

    Examples:
      | country | number     |
      | GB      | 12345678   |
      | GB      | SC123456   |
      | IE      | 654321     |
      | DE      | HRB 12345  |
      | AE      | CN-1234567 |

  Scenario Outline: Registration numbers in the wrong format are refused (<country> <number>)
    When I enter my business details for "<country>" with registration number "<number>"
    Then the "registrationNumber" field shows "<message>"

    Examples:
      | country | number  | message                                                                 |
      | GB      | 1234    | Registration numbers in GB are 8 digits, or SC/NI/OC followed by 6 digits |
      | IE      | ABC123  | Registration numbers in IE are 6 digits                                 |
      | DE      | 12345   | Registration numbers in DE are HRA or HRB, a space, then up to 6 digits |
      | AE      | 1234567 | Registration numbers in AE are CN- followed by 7 digits                 |
