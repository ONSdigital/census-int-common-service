package uk.gov.ons.ctp.common.error;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

/** Test of the Controller Advice for Spring MVC exception handling */
@RunWith(MockitoJUnitRunner.class)
public class RestExceptionHandlerTest {

  @RestController
  @RequestMapping(value = "/test")
  private interface TestController {

    @RequestMapping(value = "/run", method = RequestMethod.GET)
    ResponseEntity<String> runTest() throws CTPException;
  }

  private MockMvc mockMvc;

  @Mock private TestController testController;

  @Before
  public void setup() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(testController)
            .setControllerAdvice(new RestExceptionHandler())
            .build();
  }

  @Test
  public void handleCTPException_RESOURCE_NOT_FOUND() throws Exception {
    Mockito.doThrow(new CTPException(Fault.RESOURCE_NOT_FOUND)).when(testController).runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.RESOURCE_NOT_FOUND.toString()));
  }

  @Test
  public void handleCTPException_RESOURCE_VERSION_CONFLICT() throws Exception {
    Mockito.doThrow(new CTPException(Fault.RESOURCE_VERSION_CONFLICT))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isConflict())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.RESOURCE_VERSION_CONFLICT.toString()));
  }

  @Test
  public void handleCTPException_ACCESS_DENIED() throws Exception {
    Mockito.doThrow(new CTPException(Fault.ACCESS_DENIED)).when(testController).runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.ACCESS_DENIED.toString()));
  }

  @Test
  public void handleCTPException_BAD_REQUEST() throws Exception {
    Mockito.doThrow(new CTPException(Fault.BAD_REQUEST)).when(testController).runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.BAD_REQUEST.toString()));
  }

  @Test
  public void handleCTPException_VALIDATION_FAILED() throws Exception {
    Mockito.doThrow(new CTPException(Fault.VALIDATION_FAILED)).when(testController).runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.VALIDATION_FAILED.toString()));
  }

  @Test
  public void handleCTPException_TOO_MANY_REQUESTS() throws Exception {
    Mockito.doThrow(new CTPException(Fault.TOO_MANY_REQUESTS)).when(testController).runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isTooManyRequests())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.TOO_MANY_REQUESTS.toString()));
  }

  @Test
  public void handleCTPException_SYSTEM_ERROR() throws Exception {
    Mockito.doThrow(new CTPException(Fault.SYSTEM_ERROR)).when(testController).runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isInternalServerError())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.SYSTEM_ERROR.toString()));
  }

  @Test
  public void handleCTPException_ACCEPTED_UNABLE_TO_PROCESS() throws Exception {
    Mockito.doThrow(new CTPException(Fault.ACCEPTED_UNABLE_TO_PROCESS))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.ACCEPTED_UNABLE_TO_PROCESS.toString()));
  }

  @Test
  public void handleResponseStatusException_NOT_FOUND() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.RESOURCE_NOT_FOUND.toString()));
  }

  @Test
  public void handleResponseStatusException_CONFLICT() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isConflict())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.RESOURCE_VERSION_CONFLICT.toString()));
  }

  @Test
  public void handleResponseStatusException_UNAUTHORIZED() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.ACCESS_DENIED.toString()));
  }

  @Test
  public void handleResponseStatusException_BAD_REQUEST() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.BAD_REQUEST.toString()));
  }

  @Test
  public void handleResponseStatusException_TOO_MANY_REQUESTS() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isTooManyRequests())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.TOO_MANY_REQUESTS.toString()));
  }

  @Test
  public void handleResponseStatusException_INTERNAL_SERVER_ERROR() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isInternalServerError())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.SYSTEM_ERROR.toString()));
  }

  @Test
  public void handleResponseStatusException_ACCEPTED() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.ACCEPTED))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.ACCEPTED_UNABLE_TO_PROCESS.toString()));
  }

  @Test
  public void handleResponseStatusException_I_AM_A_TEAPOT() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(testController)
        .runTest();

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/test/run"))
            .andExpect(status().isIAmATeapot())
            .andReturn()
            .getResponse();
    assertTrue(response.getContentAsString().contains(Fault.SYSTEM_ERROR.toString()));
  }
}
