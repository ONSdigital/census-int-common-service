package uk.gov.ons.ctp.common.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;

/**
 * Test of teh CTP Exception
 *
 */
public class CTPExceptionTest {

  private ObjectMapper mapper;

  /**
   * setup test
   * @throws Exception oh dear
   */
  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  /**
   * a test
   * @throws Exception oh dear
   */
  @Test
  public void testFaultOnly() throws Exception {

    CTPException ctpe = new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND);
    String result = write(ctpe);
    System.out.println(result);

    assertEquals("RESOURCE_NOT_FOUND", (String) JsonPath.read(result,  "$.error.code"));
    assertEquals("Non Specific Error", (String) JsonPath.read(result,  "$.error.message"));
  }

  /**
   * a test
   * @throws Exception oh dear
   */
  @Test
  public void testFaultAndThrowable() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
    String nowStr = sdf.format(System.currentTimeMillis());
    NullPointerException npe = new NullPointerException("Testing is great");
    CTPException ctpe = new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, npe);
    String result = write(ctpe);
    System.out.println(result);

    assertEquals("RESOURCE_NOT_FOUND", (String) JsonPath.read(result,  "$.error.code"));
    assertEquals("Testing is great", (String) JsonPath.read(result,  "$.error.message"));
    assertTrue(((String) JsonPath.read(result,  "$.error.timestamp")).startsWith(nowStr));
  }

  /**
   * a test
   * @throws Exception oh dear
   */
  @Test
  public void testFaultAndMessage() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
    String nowStr = sdf.format(System.currentTimeMillis());
    CTPException ctpe = new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Testing %d %d %s", 1, 2, "three");
    String result = write(ctpe);
    System.out.println(result);

    assertEquals("RESOURCE_NOT_FOUND", (String) JsonPath.read(result,  "$.error.code"));
    assertEquals("Testing 1 2 three", (String) JsonPath.read(result,  "$.error.message"));
    assertTrue(((String) JsonPath.read(result,  "$.error.timestamp")).startsWith(nowStr));
  }

  /**
   * a test
   * @throws Exception oh dear
   */
  @Test
  public void testFaultAndMessageAndCause() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
    String nowStr = sdf.format(System.currentTimeMillis());
    NullPointerException npe = new NullPointerException("Testing is great");
    CTPException ctpe = new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, npe, "Testing %d %d %s", 1, 2, "three");
    String result = write(ctpe);
    System.out.println(result);

    assertEquals("RESOURCE_NOT_FOUND", (String) JsonPath.read(result,  "$.error.code"));
    assertEquals("Testing 1 2 three", (String) JsonPath.read(result,  "$.error.message"));
    assertTrue(((String) JsonPath.read(result,  "$.error.timestamp")).startsWith(nowStr));
  }

  /**
   * write an object for test
   * @param obj to be written
   * @return the string representation
   * @throws Exception oh dear
   */
  protected String write(Object obj) throws Exception {
    Writer writer = new StringWriter();
    mapper.writeValue(writer, obj);
    return writer.toString();
  }


}
