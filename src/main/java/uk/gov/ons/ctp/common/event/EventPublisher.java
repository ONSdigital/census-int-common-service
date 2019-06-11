package uk.gov.ons.ctp.common.event;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;

/** Service responsible for the publication of events. */
public class EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

  private RabbitTemplate template;

  @Getter
  public enum EventType {
    SURVEY_LAUNCHED("RESPONDENT_HOME", "RH"),
    RESPONDENT_AUTHENTICATED("RESPONDENT_HOME", "RH");

    private String source;
    private String channel;

    EventType(String source, String channel) {
      this.source = source;
      this.channel = channel;
    }
  }

  /**
   * Constructor taking publishing helper class
   *
   * @param template Helper class for asynchronous publishing
   */
  public EventPublisher(RabbitTemplate template) {
    this.template = template;
  }

  /**
   * Method to publish a respondent Event.
   *
   * @param routingKey message routing key for event
   * @param payload message payload for event
   * @return String UUID transaction Id for event
   * @throws CTPException
   */
  public String sendEvent(String routingKey, EventPayload payload) throws CTPException {

    if (payload instanceof SurveyLaunchedResponse) {
      Header header = buildHeader(EventType.SURVEY_LAUNCHED);
      SurveyLaunchedEvent event = new SurveyLaunchedEvent();
      event.setEvent(header);
      event.getPayload().setResponse((SurveyLaunchedResponse) payload);
      template.convertAndSend(routingKey, event);
      return event.getEvent().getTransactionId();
    } else if (payload instanceof RespondentAuthenticatedResponse) {
      Header header = buildHeader(EventType.RESPONDENT_AUTHENTICATED);
      RespondentAuthenticatedEvent event = (new RespondentAuthenticatedEvent());
      event.setEvent(header);
      event.getPayload().setResponse((RespondentAuthenticatedResponse) payload);
      template.convertAndSend(routingKey, event);
      return event.getEvent().getTransactionId();
    } else {
      log.error(payload.getClass().getName() + " not supported");
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, payload.getClass().getName() + " not supported");
    }
  }

  private static Header buildHeader(EventType type) {
    return Header.builder()
        .type(type.toString())
        .source(type.getSource())
        .channel(type.getChannel())
        .dateTime(new Date())
        .transactionId(UUID.randomUUID().toString())
        .build();
  }
}
