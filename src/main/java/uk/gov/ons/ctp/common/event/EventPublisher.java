package uk.gov.ons.ctp.common.event;

import uk.gov.ons.ctp.common.error.CTPException;

public interface EventPublisher {

  /**
   * Method to publish an asynchronous event message.
   *
   * @param routingKey message routing key
   * @param payload message payload to send
   * @return String of transaction Id
   * @throws CTPException if cannot deal with payload
   */
  <T> String sendEvent(String routingKey, T payload) throws CTPException;
}
