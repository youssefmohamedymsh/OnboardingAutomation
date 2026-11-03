@ui @team
Feature: Invite the team by CSV
  Once approved, the business can invite colleagues in bulk. Every row is checked and the
  problem rows are reported, while the valid rows are still invited.

  Background:
    Given my business has been approved
    And I have opened the "team.html" page

  Scenario: Every valid colleague is invited
    When I upload a team CSV with these rows:
      | first_name | last_name | email                 |
      | Sara       | Nolan     | sara.nolan@{domain}   |
      | Omar       | Haddad    | omar.haddad@{domain}  |
      | Li         | Wei       | li.wei@{domain}       |
    Then I see "3 people invited"
    And no rows are rejected

  Scenario: Problem rows are reported and the rest are invited
    When I upload a team CSV with these rows:
      | first_name | last_name | email                 |
      | Sara       | Nolan     | sara.nolan@{domain}   |
      | Omar       | Haddad    | omar@gmail.com        |
      | Li         | Wei       | not-an-email          |
      | Sara       | Nolan     | sara.nolan@{domain}   |
      | Ann-Marie  | O'Neil    | ann.oneil@{domain}    |
    Then I see "2 people invited"
    And these rows are rejected:
      | row | email                | reason                             |
      | 3   | omar@gmail.com       | Must be a work email at @{domain}  |
      | 4   | not-an-email         | Not a valid email address          |
      | 5   | sara.nolan@{domain}  | Duplicate email                    |

  Scenario: A file with the wrong columns is refused
    When I upload a CSV file containing:
      """
      name,email
      Sara Nolan,sara.nolan@acme-demo.co.uk
      """
    Then the "file" field shows "The first row must be exactly: first_name,last_name,email"
