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
}
