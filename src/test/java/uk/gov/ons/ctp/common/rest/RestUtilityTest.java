package uk.gov.ons.ctp.common.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.http.HttpEntity;

public class RestUtilityTest {

  private RestUtility restUtility =
      new RestUtility(RestClientConfig.builder().username("user").password("password").build());

  @Test
  public void shouldAddAuthorizationHeaderWhenCreateHttpEntityWithAuthHeader() {
    HttpEntity<Object> httpEntity = restUtility.createHttpEntityWithAuthHeader();

    assertThat(httpEntity.getHeaders(), hasKey("Authorization"));
    String authorizationHeader = httpEntity.getHeaders().getFirst("Authorization");
    String base64UserPassword = StringUtils.substringAfter(authorizationHeader, "Basic ");
    String usernamePassword =
        new String(
            Base64.getDecoder().decode(base64UserPassword.getBytes()), StandardCharsets.US_ASCII);
    assertEquals(usernamePassword, "user:password");
  }

  @Test
  public void shouldAddJsonContentTypeHeaderWhenCreateHttpEntityWithAuthHeader() {
    HttpEntity<Object> httpEntity = restUtility.createHttpEntityWithAuthHeader();

    assertThat(httpEntity.getHeaders(), hasKey("Content-Type"));
    assertEquals(httpEntity.getHeaders().getFirst("Content-Type"), "application/json");
  }

  @Test
  public void shouldAddJsonAcceptHeaderWhenCreateHttpEntityWithAuthHeader() {
    HttpEntity<Object> httpEntity = restUtility.createHttpEntityWithAuthHeader();

    assertThat(httpEntity.getHeaders(), hasKey("Accept"));
    assertEquals(httpEntity.getHeaders().getFirst("Accept"), "application/json");
  }

  @Test
  public void shouldAddAuthorizationHeaderWhenCreateHttpEntity() {
    HttpEntity<Object> httpEntity = restUtility.createHttpEntity(null);

    assertThat(httpEntity.getHeaders(), hasKey("Authorization"));
    String authorizationHeader = httpEntity.getHeaders().getFirst("Authorization");
    String base64UserPassword = StringUtils.substringAfter(authorizationHeader, "Basic ");
    String usernamePassword =
        new String(
            Base64.getDecoder().decode(base64UserPassword.getBytes()), StandardCharsets.US_ASCII);
    assertEquals(usernamePassword, "user:password");
  }

  @Test
  public void shouldAddJsonContentTypeHeaderWhenCreateHttpEntity() {
    HttpEntity<Object> httpEntity = restUtility.createHttpEntity(null);

    assertThat(httpEntity.getHeaders(), hasKey("Content-Type"));
    assertEquals(httpEntity.getHeaders().getFirst("Content-Type"), "application/json");
  }

  @Test
  public void shouldAddJsonAcceptHeaderWhenCreateHttpEntity() {
    HttpEntity<Object> httpEntity = restUtility.createHttpEntity(null);

    assertThat(httpEntity.getHeaders(), hasKey("Accept"));
    assertEquals(httpEntity.getHeaders().getFirst("Accept"), "application/json");
  }
}
