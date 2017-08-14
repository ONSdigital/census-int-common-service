package uk.gov.ons.ctp.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.oxm.Marshaller;
import uk.gov.ons.ctp.common.error.CTPException;

import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A class which allows for the execution of a provided CheckedFunction lambda.
 * If the lambda throws any Runtime exception the command will serialise the
 * object to be consumed to XML and log it as an error. Effectively using the
 * log as a DeadLetterQueue. To be used by RabbitMQ message consuming methods
 * ie @ServiceActivator annotated methods
 *
 * @param <X> the type that the lambda will consume
 */
@Slf4j
public class DeadLetterLogCommand<X> {

  private X thingToMarshal;
  private Marshaller marshaller;

  /**
   * Checked function executor interface
   * @param <X>
   */
  @FunctionalInterface
  public interface CheckedFunction<X> {
    /**
     * Executor
     * @param x X
     * @throws CTPException CTPException
     */
    void execute(X x) throws CTPException;
  }

  /**
   * DeadLetterLogCommand Constructor
   * @param marshaller mashaller to be used
   * @param thingToMarshal thing to marshall
   */
  public DeadLetterLogCommand(Marshaller marshaller, X thingToMarshal) {
    this.thingToMarshal = thingToMarshal;
    this.marshaller = marshaller;
  }

  /**
   * Run the lambda and turn the consumed object in to xml for logging
   *
   * @param function the lambda that is the doing the work we wish to log on failure
   */
  public void run(CheckedFunction<X> function) {
    try {
      function.execute(thingToMarshal);
    } catch (Throwable t) {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        marshaller.marshal(thingToMarshal, new StreamResult(baos));
        log.error("Dead Letter Log Exception: {}", t);
        log.error("Dead Letter Log Content: {}", baos.toString());
      } catch (IOException ioe) {
        // we cannot marshal it to xml, so last ditch .. toString()
        log.error("Tried but failed to Dead Letter Log : {}", thingToMarshal);
      }
    }
  }
}
