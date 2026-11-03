@ui @document
Feature: Identity document upload
  Applicants upload one ID document. Only PDF, PNG or JPG files up to 5 MB are accepted.

  Background:
    Given I am a verified applicant
    And my business details are complete
    And my beneficial owners are complete
    And I have opened the "identity.html" page

  Scenario Outline: Supported files are accepted (<file>)
    When I upload "<file>" as my "<type>"
    Then I am on the "Review and submit" step

    Examples:
      | file         | type            |
      | passport.pdf | passport        |
      | id-card.png  | national_id     |
      | licence.jpg  | driving_licence |

  Scenario Outline: Unsupported files are refused (<file>)
    When I upload "<file>" as my "passport"
    Then the "file" field shows "<message>"

    Examples:
      | file              | message                        |
      | notes.txt         | Upload a PDF, PNG or JPG file  |
      | empty-scan.pdf    | This file is empty             |
      | large-scan.pdf    | Files must be 5 MB or smaller  |
