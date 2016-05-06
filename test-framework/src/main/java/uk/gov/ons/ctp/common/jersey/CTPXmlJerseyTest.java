package uk.gov.ons.ctp.common.jersey;


import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.impl.ConfigurableMapper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.springframework.http.HttpStatus;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.jackson.JacksonConfigurator;
import uk.gov.ons.ctp.common.jaxrs.CTPExceptionMapper;
import uk.gov.ons.ctp.common.jaxrs.GeneralExceptionMapper;
import uk.gov.ons.ctp.common.jaxrs.NotFoundExceptionMapper;
import uk.gov.ons.ctp.common.jaxrs.QueryParamExceptionMapper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;

/**
 * An abstract base class for CTP Unit Tests with endpoints expecting XML. This class attempts to distill
 * into util methods the repetitive drudgery of cookie cutter, mechanical
 * jersey unit test code. It provides a DSL or fluent API. Start
 * with(etc).assertThis(etc).assertOther(etc etc).assertEtc
 * */
public abstract class CTPXmlJerseyTest extends JerseyTest {

  private static final String ERROR_CODE = "$.error.code";
  private static final String ERROR_TIMESTAMP = "$.error.timestamp";
  private static final String ERROR_MESSAGE = "$.error.message";

  /**
   * To initiailise the test
   *
   * @param endpointClass the Controller/Endpoint class being tested
   * @param serviceClass the Service class used in the Controller/Endpoint class
   * @param factoryClass the Factory class providing the mocked Service
   * @param mapper the bean mapper that maps to/from DTOs and JPA entity types
   * @param extraObjectsToRegister test-specific objects that need to be registered in the ResourceConfig
   * @return a JAX-RS Application
   *
   */
  @SuppressWarnings("rawtypes")
  public final Application init(final Class endpointClass, final Class serviceClass, final Class factoryClass,
                                final ConfigurableMapper mapper, final Object... extraObjectsToRegister) {
    ResourceConfig config = new ResourceConfig(endpointClass);

    AbstractBinder binder = new AbstractBinder() {
      @SuppressWarnings("unchecked")
      @Override
      protected void configure() {
        if (serviceClass != null && factoryClass != null) {
          bindFactory(factoryClass).to(serviceClass);
        }
        if (mapper != null) {
          bind(mapper).to(MapperFacade.class);
        }
      }
    };
    config.register(binder);

    config.register(CTPExceptionMapper.class);
    config.register(GeneralExceptionMapper.class);
    config.register(JacksonConfigurator.class);
    config.register(NotFoundExceptionMapper.class);
    config.register(QueryParamExceptionMapper.class);

    if (extraObjectsToRegister != null) {
      for (int i = 0; i < extraObjectsToRegister.length; i++) {
        config.register(extraObjectsToRegister[i]);
      }
    }

    return config;
  }

  /**
   * This method sets the url/endpoint under test.
   * @param url the url of the endpoint under test
   * @param args the substitutes in the url string
   * @return the TestableResponse object
   */
  protected final CTPXmlJerseyTest.TestableResponse with(final String url, final Object... args) {
    return new CTPXmlJerseyTest.TestableResponse(String.format(url, args));
  }

  /**
   * The TestableResponse class
   */
  @RequiredArgsConstructor
  protected static class TestableResponse {
    @NonNull
    private String url;

    private Client client;
    private Response response;
    private String responseStr;
    private String bodyStr;
    private CTPXmlJerseyTest.TestableResponse.Operation operation = CTPXmlJerseyTest.TestableResponse.Operation.GET;

    /**
     * The list of supported HTTP methods
     */
    private enum Operation {
      GET, PUT, POST
    }

    /**
     * This method sets the operation to GET
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse get() {
      operation = CTPXmlJerseyTest.TestableResponse.Operation.GET;
      return this;
    }

    /**
     * This method sets the operation to PUT and the body to the provided String
     * @param body the provided String
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse put(final String body) {
      bodyStr = body;
      operation = CTPXmlJerseyTest.TestableResponse.Operation.PUT;
      return this;
    }

    /**
     * This method sets the operation to POST and the body to the provided String
     * @param body the provided String
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse post(final String body) {
      bodyStr = body;
      operation = CTPXmlJerseyTest.TestableResponse.Operation.POST;
      return this;
    }

    /**
     * This method compares the Response Http Status to the expected one.
     * @param expectedStatus the expected Http Status
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertResponseCodeIs(final HttpStatus expectedStatus) {
      Assert.assertEquals(expectedStatus.value(), getResponse().getStatus());
      return this;
    }

    /**
     * This method compares the Response Length to the expected one.
     * @param value the expected Length
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertResponseLengthIs(final int value) {
      Assert.assertEquals(value, getResponse().getLength());
      return this;
    }

    /**
     * This method compares the Fault contained in the Response to the expected one.
     * @param fault the expected Fault
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertFaultIs(final CTPException.Fault fault) {
      Assert.assertEquals(fault.toString(), JsonPath.read(getResponseString(), ERROR_CODE));
      return this;
    }

    /**
     * This method compares the Response String to an empty String.
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertEmptyResponse() {
      Assert.assertEquals("Response should not contain anything", getResponseString(), "");
      return this;
    }

    /**
     * This method verifies that an Error Timestamp is present in the Response.
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertTimestampExists() {
      Assert.assertNotNull(JsonPath.read(getResponseString(), ERROR_TIMESTAMP));
      return this;
    }

    /**
     * This method verifies that an Error Message is present in the Response. And compares it to the provided message.
     * @param message the expected error message
     * @param args the substitutes in the error message
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertMessageEquals(final String message, final Object... args) {
      Assert.assertEquals(String.format(message, args), JsonPath.read(getResponseString(), ERROR_MESSAGE));
      return this;
    }

    /**
     * This method verifies that the Response contains an array of the given length
     * @param value the expected length of the array present in the Response
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertArrayLengthInBodyIs(final int value) {
      Assert.assertEquals(new Integer(value), JsonPath.parse(getResponseString()).read("$.length()", Integer.class));
      return this;
    }

    /**
     * This method verifies that the provided json path contains an integer value.
     * And it matches it to the provided integer.
     * @param path the expected json path
     * @param value the integer value for the expected json path
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertIntegerInBody(final String path, final int value) {
      Assert.assertEquals(new Integer(value), JsonPath.parse(getResponseString()).read(path, Integer.class));
      return this;
    }

    /**
     * This method verifies that the provided json path contains a list of integer values.
     * And it matches the list with the provided one.
     * @param path the expected json path
     * @param integers the list of integers for the expected json path
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertIntegerListInBody(final String path, final Integer... integers) {
      List<Integer> integersList = JsonPath.parse(getResponseString()).read(path);
      Assert.assertThat(integersList, containsInAnyOrder(integers));
      return this;
    }

    /**
     * This method verifies that the provided json path contains a double value.
     * And it matches it to the provided double.
     * @param path the expected json path
     * @param value the double value for the expected json path
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertDoubleInBody(final String path, final double value) {
      Assert.assertEquals(new Double(value), JsonPath.parse(getResponseString()).read(path, Double.class));
      return this;
    }

    /**
     * This method verifies that the provided json path contains a list of double values.
     * And it matches the list with the provided one.
     * @param path the expected json path
     * @param doubles the list of doubles for the expected json path
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertDoubleListInBody(final String path, final Double... doubles) {
      List<Double> doublesList = JsonPath.parse(getResponseString()).read(path);
      Assert.assertThat(doublesList, containsInAnyOrder(doubles));
      return this;
    }

    /**
     * This method verifies that the provided json path contains a string value.
     * And it matches it to the provided string.
     * @param path the expected json path
     * @param value the string value for the expected json path
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertStringInBody(final String path, final String value) {
      Assert.assertEquals(value, JsonPath.parse(getResponseString()).read(path, String.class));
      return this;
    }

    /**
     * This method verifies that the provided json path contains a list of string values.
     * And it matches the list with the provided one.
     * @param path the expected json path
     * @param strs the list of strings for the expected json path
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertStringListInBody(final String path, final String... strs) {
      List<String> strList = JsonPath.parse(getResponseString()).read(path);
      Assert.assertThat(strList, containsInAnyOrder(strs));
      return this;
    }

    /**
     * This method verifies that the provided json path contains a list of boolean values.
     * And it matches the list with the provided one.
     * @param path the expected json path
     * @param bools the list of boolean values for the expected json path
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertBooleanListInBody(final String path, final Boolean... bools) {
      List<Boolean> booleanList = JsonPath.parse(getResponseString()).read(path);
      Assert.assertThat(booleanList, containsInAnyOrder(bools));
      return this;
    }

    /**
     * This method verifies that the provided json path contains a list of integer values.
     * And it verifies that all values in the list are equal to the provided integer.
     * @param path the expected json path
     * @param value the integer value to match against
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertIntegerOccursThroughoutListInBody(final String path, final int value) {
      List<Integer> integerList = JsonPath.parse(getResponseString()).read(path);
      Assert.assertThat(integerList, everyItem(equalTo(value)));
      return this;
    }

    /**
     * This method verifies that the provided json path contains a list of string values.
     * And it verifies that all values in the list are equal to the provided string.
     * @param path the expected json path
     * @param value the string value to match against
     * @return the TestableResponse object
     */
    public final CTPXmlJerseyTest.TestableResponse assertStringOccursThroughoutListInBody(final String path, final String value) {
      List<String> stringList = JsonPath.parse(getResponseString()).read(path);
      Assert.assertThat(stringList, everyItem(equalTo(value)));
      return this;
    }

    /**
     * This method closes the JAX-RS response and the JAX-RS client
     * as per the recommendations by the JAX-RS Test Framework.
     */
    public final void andClose() {
      response.close();
      client.close();
    }

    /**
     * Client, Response and ResponseStr are chickens and eggs, The
     * TestableResponse is constructed with url only. Before we can get
     * ResponseStr, we need to get Response, Before we can get Response, we need
     * to get Client. Nested lazy loading!
     * @return the JAX-RS Client
     */
    private Client getClient() {
      if (client == null) {
        client = ClientBuilder.newClient();
      }
      return client;
    }

    /**
     *
     * @return the JAX-RS response
     */
    private Response getResponse() {
      if (response == null) {
        Invocation.Builder builder = getClient().target(url).request();
        switch (operation) {
          case GET:
            response = builder.get();
            break;
          case PUT:
            response = builder.put(Entity.xml(bodyStr));
            break;
          case POST:
            response = builder.post(Entity.xml(bodyStr));
            break;
          default:
            response = builder.get();
        }
      }
      return response;
    }

    /**
     *
     * @return the json response String
     */
    private String getResponseString() {
      if (responseStr == null) {
        responseStr = getResponse().readEntity(String.class);
      }
      return responseStr;
    }
  }
}