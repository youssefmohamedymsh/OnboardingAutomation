@ui @screening
Feature: Screening, risk score and decision
  On submission every owner is screened against the sanctions and PEP (politically exposed person)
  test lists, and the application gets a risk score from its country, industry, turnover, PEP
  status and document type. Sanctions matches are rejected; PEPs and scores of 50+ go to a human.

  Scenario Outline: Low-risk applications are approved instantly (<country>, <industry>)
    Given I am a verified applicant
    And my business is registered in "<country>" in the "<industry>" industry
    And "Maya Brook" is the only beneficial owner
    And my "<document>" is uploaded
    When I review and submit my application
    Then the decision is "Approved"
    And the risk score is <score>
    And no decision reasons are listed
    And I can invite my team

    Examples:
      | country | industry   | document    | score |
      | GB      | software   | passport    | 10    |
      | IE      | retail     | national_id | 15    |
      | DE      | consulting | passport    | 15    |

  Scenario Outline: Screening hits and high risk are stopped (<case>)
    Given I am a verified applicant
    And my business is registered in "<country>" in the "<industry>" industry
    And "<owner>" is the only beneficial owner
    And my "<document>" is uploaded
    When I review and submit my application
    Then the decision is "<decision>"
    And the risk score is <score>
    And the decision reasons include "<reason>"
    And I can't invite my team yet

    Examples:
      | case                | country | industry       | owner        | document        | decision         | score | reason                                  |
      | sanctions match     | GB      | software       | Viktor Kraal | passport        | Rejected         | 10    | Sanctions match: Viktor Kraal           |
      | PEP                 | GB      | software       | Elena Marsh  | passport        | In manual review | 40    | Politically exposed person: Elena Marsh |
      | high-risk industry  | AE      | crypto         | Maya Brook   | passport        | In manual review | 65    | Risk score 65 is at or above 50         |
      | money services + DL | AE      | money_services | Maya Brook   | driving_licence | In manual review | 65    | Risk score 65 is at or above 50         |

  Scenario: An incomplete application can't be submitted
    Given I am a verified applicant
    And my business details are complete
    When I review and submit my application
    Then I see the error "Still to do: Beneficial owners, ID document"
