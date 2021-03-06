package uk.gov.ons.ctp.common.error;

// import net.sourceforge.cobertura.CoverageIgnore;
import org.springframework.validation.Errors;

/** Invalid Requestion Exception */
// @CoverageIgnore
public class InvalidRequestException extends Exception {
  private static final long serialVersionUID = -3708136528897081539L;

  private Errors errors;
  private String sourceMessage = "Invalid Request ";

  /**
   * InvalidRequestException Constructor
   *
   * @param message message to be displayed
   * @param errors errors
   */
  public InvalidRequestException(String message, Errors errors) {
    super(message);
    this.errors = errors;
  }

  /**
   * Errors getter
   *
   * @return Errors errors
   */
  public Errors getErrors() {
    return errors;
  }

  /**
   * Source Message getter
   *
   * @return String source message
   */
  public String getSourceMessage() {
    return sourceMessage;
  }
}
