package uk.gov.ons.ctp.common.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestClientConfig {
  @Builder.Default private String scheme = "http";
  @Builder.Default private String host = "localhost";
  @Builder.Default private String port = "8080";
  private String username;
  private String password;

  @Builder.Default private int connectionManagerDefaultMaxPerRoute = 20;
  @Builder.Default private int connectionManagerMaxTotal = 50;

  // Timeout value for establishing a connection with the destination server
  @Builder.Default private int connectTimeoutMillis = 0;
  // Timeout for getting a connection from the connection manager
  @Builder.Default private int connectionRequestTimeoutMillis = 0;
  // Maximum time to wait between data packets
  @Builder.Default private int socketTimeoutMillis = 0;
}
