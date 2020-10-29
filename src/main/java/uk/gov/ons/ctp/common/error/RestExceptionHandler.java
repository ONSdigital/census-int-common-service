package uk.gov.ons.ctp.common.error;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

/** Rest Exception Handler */
@ControllerAdvice
public class RestExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

  public static final String INVALID_JSON = "Provided json fails validation.";
  public static final String PROVIDED_JSON_INCORRECT = "Provided json is incorrect.";

  // Regex patterns used to produce a summary of the root cause of errors
  List<Pattern> exceptionScrapers =
      Arrays.asList(
          // Multiple error catch all
          Pattern.compile("Validation failed .* with ([0-9]* errors: .*)"),
          // For enums
          Pattern.compile("(JSON parse error: .*); nested exception is .*"),
          // For @NotNull
          Pattern.compile(
              ".*(Field error in object .* rejected value \\[null]; ).*(default message .*)"),
          // For @Pattern
          Pattern.compile(
              ".*(field.*rejected value.*?;).*(constraints.Pattern).*( ).*(must match.*)]]"),
          // For @Size & @NotBlank
          Pattern.compile("error .* on (field.*?; ).*default message \\[(.*)]]"));

  /**
   * CTPException Handler
   *
   * @param exception CTPException
   * @return ResponseEntity containing exception and associated HttpStatus
   */
  @ExceptionHandler(CTPException.class)
  public ResponseEntity<?> handleCTPException(CTPException exception) {

    HttpStatus status = mapFaultToHttpStatus(exception.getFault());

    switch (status) {
      case NOT_FOUND:
        log.with("fault", exception.getFault())
            .with("message", exception.getMessage())
            .warn("Handling CTPException - Resource not found");
        break;
      case BAD_REQUEST:
        log.with("fault", exception.getFault())
            .with("message", exception.getMessage())
            .warn("Handling CTPException - Bad request");
        break;
      case ACCEPTED:
        log.with("fault", exception.getFault())
            .with("message", exception.getMessage())
            .warn("Handling CTPException - The request is accepted but unable to process");
        break;

      default:
        log.with("fault", exception.getFault())
            .with("message", exception.getMessage())
            .error("Handling CTPException - System error", exception);
        break;
    }

    return new ResponseEntity<>(exception, status);
  }

  private HttpStatus mapFaultToHttpStatus(Fault fault) {
    HttpStatus status;
    switch (fault) {
      case RESOURCE_NOT_FOUND:
        status = HttpStatus.NOT_FOUND;
        break;
      case RESOURCE_VERSION_CONFLICT:
        status = HttpStatus.CONFLICT;
        break;
      case ACCESS_DENIED:
        status = HttpStatus.UNAUTHORIZED;
        break;
      case BAD_REQUEST:
      case VALIDATION_FAILED:
        status = HttpStatus.BAD_REQUEST;
        break;
      case TOO_MANY_REQUESTS:
        status = HttpStatus.TOO_MANY_REQUESTS;
        break;
      case SYSTEM_ERROR:
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        break;
      case ACCEPTED_UNABLE_TO_PROCESS:
        status = HttpStatus.ACCEPTED;
        break;
      default:
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        break;
    }
    return status;
  }

  private Fault mapHttpStatusToFault(HttpStatus status) {
    Fault fault;
    switch (status) {
      case NOT_FOUND:
        fault = Fault.RESOURCE_NOT_FOUND;
        break;
      case CONFLICT:
        fault = Fault.RESOURCE_VERSION_CONFLICT;
        break;
      case UNAUTHORIZED:
        fault = Fault.ACCESS_DENIED;
        break;
      case BAD_REQUEST:
        fault = Fault.BAD_REQUEST;
        break;
      case TOO_MANY_REQUESTS:
        fault = Fault.TOO_MANY_REQUESTS;
        break;
      case INTERNAL_SERVER_ERROR:
        fault = Fault.SYSTEM_ERROR;
        break;
      case ACCEPTED:
        fault = Fault.ACCEPTED_UNABLE_TO_PROCESS;
        break;
      default:
        fault = Fault.SYSTEM_ERROR;
        break;
    }
    return fault;
  }

  /**
   * Handler for ResponseStatusException.
   *
   * @param exception is the underlying exception.
   * @return ResponseEntity containing exception details.
   */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> handleResponseStatusException(ResponseStatusException exception) {
    log.with("fault", exception.getStatus())
        .with("exception_message", exception.getMessage())
        .warn("RestExceptionHandler is handling ResponseStatusException");

    Fault fault = mapHttpStatusToFault(exception.getStatus());
    CTPException ourException = new CTPException(fault, exception.getMessage());
    return new ResponseEntity<>(ourException, exception.getStatus());
  }

  /**
   * Handler for Invalid Request Exceptions
   *
   * @param t Throwable
   * @return ResponseEntity containing CTP Exception
   */
  @ExceptionHandler(Throwable.class)
  public ResponseEntity<?> handleGeneralException(Throwable t) {
    log.error("Uncaught Throwable", t);
    return new ResponseEntity<>(
        new CTPException(CTPException.Fault.SYSTEM_ERROR, t, t.getMessage()),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Handler for MethodArgumentTypeMismatchExceptions Thrown when spring attempts to convert path
   * param values into declared endpoint method params
   *
   * @param ex the exception we are handling
   * @return ResponseEntity containing CTPException
   */
  @ResponseBody
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<?> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException ex) {

    String errors =
        String.format("field=%s value=%s message=%s", ex.getName(), ex.getValue(), ex.getMessage());

    log.with("validation_errors", errors)
        .with("source_message", ex.getRootCause())
        .warn("Unhandled MethodArgumentTypeMismatchException");
    CTPException ourException =
        new CTPException(CTPException.Fault.VALIDATION_FAILED, PROVIDED_JSON_INCORRECT);
    return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handler for Invalid Request Exceptions
   *
   * @param ex the exception we are handling
   * @return ResponseEntity containing CTPException
   */
  @ResponseBody
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<?> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException ex) {

    String errors = String.format("field=%s message=%s", ex.getParameterName(), ex.getMessage());

    log.with("validation_errors", errors)
        .with("source_message", ex.getMessage())
        .warn("Unhandled MethodArgumentTypeMismatchException");
    CTPException ourException =
        new CTPException(CTPException.Fault.VALIDATION_FAILED, PROVIDED_JSON_INCORRECT);
    return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handler for Invalid Request Exceptions
   *
   * @param ex the exception we are handling
   * @return ResponseEntity containing CTPException
   */
  @ResponseBody
  @ExceptionHandler(BindException.class)
  public ResponseEntity<?> handleBindException(BindException ex) {

    String errors =
        String.format("field=%s message=%s", ex.getFieldError().getField(), ex.getMessage());

    log.with("validation_errors", errors)
        .with("source_message", ex.getMessage())
        .warn("Unhandled BindException");
    CTPException ourException =
        new CTPException(CTPException.Fault.VALIDATION_FAILED, ex.getMessage());
    return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handler for HttpMessageConversionException
   *
   * @param ex the exception we are handling
   * @return ResponseEntity containing CTPException
   */
  @ResponseBody
  @ExceptionHandler(HttpMessageConversionException.class)
  public ResponseEntity<?> handleHttpMessageConversionException(HttpMessageConversionException ex) {

    String errors =
        String.format("field=%s message=%s", ex.getMostSpecificCause(), ex.getMessage());

    log.with("validation_errors", errors)
        .with("source_message", ex.getMessage())
        .warn("Unhandled HttpMessageConversionException");
    CTPException ourException =
        new CTPException(CTPException.Fault.VALIDATION_FAILED, PROVIDED_JSON_INCORRECT);
    return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handler for Invalid Request Exceptions
   *
   * @param ex the exception we are handling
   * @return ResponseEntity containing CTPException
   */
  @ResponseBody
  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<?> handleInvalidRequestException(InvalidRequestException ex) {

    String errors =
        ex.getErrors().getFieldErrors().stream()
            .map(e -> String.format("field=%s message=%s", e.getField(), e.getDefaultMessage()))
            .collect(Collectors.joining(","));

    log.with("validation_errors", errors)
        .with("source_message", ex.getSourceMessage())
        .warn("Unhandled InvalidRequestException");
    CTPException ourException =
        new CTPException(CTPException.Fault.VALIDATION_FAILED, INVALID_JSON);
    return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles Http Message Not Readable Exception
   *
   * @param ex exception
   * @return ResponseEntity containing exception and BAD_REQUEST http status
   */
  @ResponseBody
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<?> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {
    String message = createCleanedUpErrorMessage(ex);
    log.warn("Uncaught HttpMessageNotReadableException. {}", message);

    CTPException ourException = new CTPException(CTPException.Fault.VALIDATION_FAILED, message);

    return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles Method Argument not valid Exception, which is generated for validation on the fields in
   * the request body.
   *
   * @param ex exception
   * @return ResponseEntity containing exception and BAD_REQUEST http status
   */
  @ResponseBody
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    String message = createCleanedUpErrorMessage(ex);
    log.with("parameter", ex.getParameter().getParameterName())
        .warn("Uncaught MethodArgumentNotValidException. {}", message);

    CTPException ourException = new CTPException(CTPException.Fault.VALIDATION_FAILED, message);
    return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
  }

  /**
   * This method creates explanatory response text for an exception. It cleans up the exception
   * message and attempts to provide a concise reason for the error without internal class names or
   * error repetition.
   *
   * @param ex is the exception to create a message for.
   * @return a String with simplified error text, or if it doesn't fit an existing pattern an error
   *     string with the exceptions message text.
   */
  private String createCleanedUpErrorMessage(Exception ex) {
    // By default we return the message text, unless we can scrape out a better explanation
    String messageDetail = ex.getMessage();

    // Attempt to extract key content of exception message
    for (Pattern pattern : exceptionScrapers) {
      Matcher matcher = pattern.matcher(ex.getMessage());
      if (matcher.find()) {
        StringBuilder messageExtracts = new StringBuilder();
        for (int i = 1; i <= matcher.groupCount(); i++) {
          messageExtracts.append(matcher.group(i));
        }
        messageDetail = messageExtracts.toString();
        break;
      }
    }

    return PROVIDED_JSON_INCORRECT + " Caused by: " + messageDetail.trim();
  }
}
