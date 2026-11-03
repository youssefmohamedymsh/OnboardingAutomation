@ui @ubo
Feature: Beneficial owners (UBOs)
  Every person who owns 25% or more must be listed. Owners must be adults, and ownership
  can't add up to more than 100%.

  Background:
    Given I am a verified applicant
    And my business details are complete
    And I have opened the "owners.html" page

  Scenario: Two owners who share the business
    When I add these beneficial owners:
      | fullName   | dateOfBirth | nationality | ownership |
      | Maya Brook | 1988-03-14  | GB          | 60        |
      | Sam Ortiz  | 1990-07-01  | IE          | 40        |
    Then I am on the "Verify your identity" step

  Scenario: Ownership can't add up to more than 100%
    When I add these beneficial owners:
      | fullName   | dateOfBirth | nationality | ownership |
      | Maya Brook | 1988-03-14  | GB          | 60        |
      | Sam Ortiz  | 1990-07-01  | IE          | 50        |
    Then the owners section shows "Ownership adds up to 110%. It can't be more than 100%."

  Scenario Outline: Each owner must meet the KYC rules (<rule>)
    When I add these beneficial owners:
      | fullName   | dateOfBirth   | nationality   | ownership   |
      | <fullName> | <dateOfBirth> | <nationality> | <ownership> |
    Then owner 1's "<field>" field shows "<message>"

    Examples:
      | rule              | fullName   | dateOfBirth | nationality    | ownership | field       | message                                         |
      | adult             | Maya Brook | 2015-01-01  | GB             | 100       | dateOfBirth | Beneficial owners must be 18 or older           |
      | 25% threshold     | Maya Brook | 1988-03-14  | GB             | 10        | ownership   | Only list owners with 25% to 100% ownership     |
      | country code      | Maya Brook | 1988-03-14  | 12             | 100       | nationality | Enter a 2-letter country code, like GB          |
      | full name         | Maya       | 1988-03-14  | GB             | 100       | fullName    | Enter the owner's full name as shown on their ID |
