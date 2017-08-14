package uk.gov.ons.ctp.common.error;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Rest Exception Handler
 */
@ControllerAdvice
@Slf4j
public class RestExceptionHandler {

    public static final String INVALID_JSON = "Provided json fails validation.";
    public static final String PROVIDED_JSON_INCORRECT = "Provided json is incorrect.";
    public static final String PROVIDED_XML_INCORRECT = "Provided xml is incorrect.";

    private static final String XML_ERROR_MESSAGE = "Could not unmarshal to";

  /**
   * CTPException Handler
   * @param exception CTPException
   * @return ResponseEntity containing exception and associated HttpStatus
   */
    @ExceptionHandler(CTPException.class)
    public ResponseEntity<?> handleCTPException(CTPException exception) {
        log.error("handleCTPException {}", exception);

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
                log.error("Internal System Error", exception);
                break;
            default:
                status = HttpStatus.I_AM_A_TEAPOT;
                break;
        }

        return new ResponseEntity<>(exception, status);
    }

  /**
   * Handler for Invalid Request Exceptions
   * @param t Throwable
   * @return ResponseEntity containing CTP Exception
   */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handleGeneralException(Throwable t) {
        log.error("handleGeneralException {}", t);
        return new ResponseEntity<>(new CTPException(CTPException.Fault.SYSTEM_ERROR, t, t.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

  /**
   * Handler for Invalid Request Exceptions
   * @param ex Invalid Request Exception
   * @param locale Locale
   * @return ResponseEntity containing CTPException
   */
    @ResponseBody
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<?> handleInvalidRequestException(InvalidRequestException ex, Locale locale) {
        log.error("handleInvalidRequestException {}", ex);
        StringBuilder logMsg = new StringBuilder(ex.getSourceMessage());

        StringBuilder responseMsg = new StringBuilder();
        List<FieldError> fieldErrors = ex.getErrors().getFieldErrors();
        for (Iterator<FieldError> errorsIte = fieldErrors.listIterator(); errorsIte.hasNext();) {
            FieldError fieldError = errorsIte.next();
            responseMsg.append(fieldError.getDefaultMessage());
            if (errorsIte.hasNext()) {
                responseMsg.append(",");
            }
        }

        log.error("logMsg is '{}' - responseMsg is '{}'", logMsg.toString(), responseMsg.toString());
        CTPException ourException = new CTPException(CTPException.Fault.VALIDATION_FAILED, INVALID_JSON);
        return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
    }

  /**
   * Handles Http Message Not Readable Exception
   * @param ex exception
   * @param locale locale to use
   * @return ResponseEntity containing exception and BAD_REQUEST http status
   */
    @ResponseBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, Locale locale) {
        log.error("handleHttpMessageNotReadableException {}", ex);

        String message = ex.getMessage().startsWith(XML_ERROR_MESSAGE)
                ? PROVIDED_XML_INCORRECT : PROVIDED_JSON_INCORRECT;
        CTPException ourException = new CTPException(CTPException.Fault.VALIDATION_FAILED, message);

        return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
    }

  /**
   * Handles Method Argument not valid Exception
   * @param ex exception
   * @param locale locale to use
   * @return ResponseEntity containing exception and BAD_REQUEST http status
   */
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, Locale locale) {
        log.error("handleMethodArgumentNotValidException {}", ex);
        CTPException ourException = new CTPException(CTPException.Fault.VALIDATION_FAILED, INVALID_JSON);
        return new ResponseEntity<>(ourException, HttpStatus.BAD_REQUEST);
    }
}

