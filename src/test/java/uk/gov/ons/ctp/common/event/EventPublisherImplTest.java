package uk.gov.ons.ctp.common.event;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;

@RunWith(MockitoJUnitRunner.class)
public class EventPublisherImplTest {

  private static final String ROUTING_KEY = "whereAreWeRoutingThis";
  private static final UUID CASE_ID = UUID.fromString("dc4477d1-dd3f-4c69-b181-7ff725dc9fa4");
  private static final String QUESTIONNAIRE_ID = "1110000009";

  @InjectMocks private EventPublisherImpl eventPublisher;
  @Mock private RabbitTemplate template;

  /** Test event message with SurveyLaunchedResponse pay load */
  @Test
  public void sendEventSurveyLaunchedPayload() throws Exception {

    SurveyLaunchedResponse surveyLaunchedResponse =
        SurveyLaunchedResponse.builder()
            .questionnaireId(QUESTIONNAIRE_ID)
            .caseId(CASE_ID)
            .agentId(null)
            .build();

    ArgumentCaptor<SurveyLaunchedEvent> eventCapture =
        ArgumentCaptor.forClass(SurveyLaunchedEvent.class);

    String trandactionId = eventPublisher.sendEvent(ROUTING_KEY, surveyLaunchedResponse);

    verify(template, times(1)).convertAndSend(eq(ROUTING_KEY), eventCapture.capture());
    SurveyLaunchedEvent event = eventCapture.getValue();

    assertEquals(event.getEvent().getTransactionId(), trandactionId);
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(
        EventPublisherImpl.EventType.SURVEY_LAUNCHED.toString(), event.getEvent().getType());
    assertEquals(
        EventPublisherImpl.EventType.SURVEY_LAUNCHED.getSource(), event.getEvent().getSource());
    assertEquals(
        EventPublisherImpl.EventType.SURVEY_LAUNCHED.getChannel(), event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
    assertEquals(CASE_ID, event.getPayload().getResponse().getCaseId());
    assertEquals(QUESTIONNAIRE_ID, event.getPayload().getResponse().getQuestionnaireId());
  }

  /** Test event message with RespondentAuthenticatedResponse pay load */
  @Test
  public void buildEventRespondentAuthenticatedPayload() throws Exception {

    RespondentAuthenticatedResponse respondentAuthenticatedResponse =
        RespondentAuthenticatedResponse.builder()
            .questionnaireId(QUESTIONNAIRE_ID)
            .caseId(CASE_ID)
            .build();

    ArgumentCaptor<RespondentAuthenticatedEvent> eventCapture =
        ArgumentCaptor.forClass(RespondentAuthenticatedEvent.class);

    String trandactionId = eventPublisher.sendEvent(ROUTING_KEY, respondentAuthenticatedResponse);

    verify(template, times(1)).convertAndSend(eq(ROUTING_KEY), eventCapture.capture());
    RespondentAuthenticatedEvent event = eventCapture.getValue();

    assertEquals(event.getEvent().getTransactionId(), trandactionId);
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(
        EventPublisherImpl.EventType.RESPONDENT_AUTHENTICATED.toString(),
        event.getEvent().getType());
    assertEquals(
        EventPublisherImpl.EventType.RESPONDENT_AUTHENTICATED.getSource(),
        event.getEvent().getSource());
    assertEquals(
        EventPublisherImpl.EventType.RESPONDENT_AUTHENTICATED.getChannel(),
        event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
    assertEquals(CASE_ID, event.getPayload().getResponse().getCaseId());
    assertEquals(QUESTIONNAIRE_ID, event.getPayload().getResponse().getQuestionnaireId());
  }

  /** Test build of Respondent Authenticated event message with wrong pay load */
  @Test
  public void buildRespondentAuthenticatedWrongPayload() {

    boolean exceptionThrown = false;

    try {
      eventPublisher.sendEvent(ROUTING_KEY, new Object());
    } catch (CTPException e) {
      exceptionThrown = true;
      assertThat(e.getMessage(), containsString("not supported"));
    }

    assertTrue(exceptionThrown);
  }
}
