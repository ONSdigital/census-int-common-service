package uk.gov.ons.ctp.common.cloud;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("cloud-storage.backoff")
@Data
public class RetryConfig {
  private int initial;
  private String multiplier; // String type to handle floats without rounding
  private int max;
  private int maxAttempts;
}
