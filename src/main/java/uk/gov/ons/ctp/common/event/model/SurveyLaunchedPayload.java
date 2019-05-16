package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyLaunchedPayload {

  private SurveyLaunchedResponse response = new SurveyLaunchedResponse();
}
