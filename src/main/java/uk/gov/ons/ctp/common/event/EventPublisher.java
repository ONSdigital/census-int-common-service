package uk.gov.ons.ctp.common.event;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalEvent;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalPayload;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;

/** Service responsible for the publication of events. */
public class EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

  private RabbitTemplate template;

  @Getter
  public enum EventType {
    SURVEY_LAUNCHED("RESPONDENT_HOME", "RH"),
    RESPONDENT_AUTHENTICATED("RESPONDENT_HOME", "RH"),
    FULFILMENT_REQUESTED("CONTACT_CENTRE_API", "CC"),
    REFUSAL_RECEIVED("CONTACT_CENTRE_API", "CC");

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
   * @throws CTPException if a failure was detected.
   */
  public String sendEvent(String routingKey, EventPayload payload) throws CTPException {

    GenericEvent genericEvent = null;
    if (payload instanceof SurveyLaunchedResponse) {
      SurveyLaunchedEvent event = new SurveyLaunchedEvent();
      event.setEvent(buildHeader(EventType.SURVEY_LAUNCHED));
      event.getPayload().setResponse((SurveyLaunchedResponse) payload);
      genericEvent = event;

    } else if (payload instanceof RespondentAuthenticatedResponse) {
      RespondentAuthenticatedEvent event = new RespondentAuthenticatedEvent();
      event.setEvent(buildHeader(EventType.RESPONDENT_AUTHENTICATED));
      event.getPayload().setResponse((RespondentAuthenticatedResponse) payload);
      genericEvent = event;

    } else if (payload instanceof FulfilmentRequest) {
      FulfilmentRequestedEvent event = new FulfilmentRequestedEvent();
      event.setEvent(buildHeader(EventType.FULFILMENT_REQUESTED));
      FulfilmentPayload fulfilmentPayload = new FulfilmentPayload((FulfilmentRequest) payload);
      event.setPayload(fulfilmentPayload);
      genericEvent = event;

    } else if (payload instanceof RespondentRefusalDetails) {
      RespondentRefusalEvent event = new RespondentRefusalEvent();
      event.setEvent(buildHeader(EventType.REFUSAL_RECEIVED));
      RespondentRefusalPayload respondentRefusalPayload =
          new RespondentRefusalPayload((RespondentRefusalDetails) payload);
      event.setPayload(respondentRefusalPayload);
      genericEvent = event;

    } else {
      log.error(payload.getClass().getName() + " not supported");
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, payload.getClass().getName() + " not supported");
    }

    template.convertAndSend(routingKey, genericEvent);
    return genericEvent.getEvent().getTransactionId();
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
