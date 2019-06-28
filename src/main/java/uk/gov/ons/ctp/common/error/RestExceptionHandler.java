package uk.gov.ons.ctp.common.error;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
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

/** Rest Exception Handler */
@ControllerAdvice
public class RestExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

  public static final String INVALID_JSON = "Provided json fails validation.";
  public static final String PROVIDED_JSON_INCORRECT = "Provided json is incorrect.";
  public static final String PROVIDED_XML_INCORRECT = "Provided xml is incorrect.";

  private static final String XML_ERROR_MESSAGE = "Could not unmarshal to";

  /**
   * CTPException Handler
   *
   * @param exception CTPException
   * @return ResponseEntity containing exception and associated HttpStatus
   */
  @ExceptionHandler(CTPException.class)
  public ResponseEntity<?> handleCTPException(CTPException exception) {
    log.with("fault", exception.getFault())
        .with("exception_message", exception.getMessage())
        .error("Uncaught CTPException", exception);

    HttpStatus status;
    switch (exception.getFault()) {
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
      case SYSTEM_ERROR:
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        break;
      default:
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        break;
    }

    return new ResponseEntity<>(exception, status);
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
        .error("Uncaught ResponseStatusException", exception);

    return new ResponseEntity<>(exception, exception.getStatus());
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
        .error("Unhandled MethodArgumentTypeMismatchException", ex);
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
        .error("Unhandled MethodArgumentTypeMismatchException", ex);
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
        .error("Unhandled BindException", ex);
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
        .error("Unhandled HttpMessageConversionException", ex);
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
        ex.getErrors()
            .getFieldErrors()
            .stream()
            .map(e -> String.format("field=%s message=%s", e.getField(), e.getDefaultMessage()))
            .collect(Collectors.joining(","));

    log.with("validation_errors", errors)
        .with("source_message", ex.getSourceMessage())
        .error("Unhandled InvalidRequestException", ex);
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
    log.error("Uncaught HttpMessageNotReadableException", ex);
    String message =
        ex.getMessage().startsWith(XML_ERROR_MESSAGE)
            ? PROVIDED_XML_INCORRECT
            : PROVIDED_JSON_INCORRECT;

    CTPException ourException = new CTPException(CTPException.Fault.VALIDATION_FAILED, message);

    return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles Method Argument not valid Exception
   *
   * @param ex exception
   * @return ResponseEntity containing exception and BAD_REQUEST http status
   */
  @ResponseBody
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    log.with("parameter", ex.getParameter().getParameterName())
        .error("Uncaught MethodArgumentNotValidException", ex);
    CTPException ourException =
        new CTPException(CTPException.Fault.VALIDATION_FAILED, INVALID_JSON);
    return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
  }
}
