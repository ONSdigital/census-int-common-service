package uk.gov.ons.ctp.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UniquePropertyReferenceNumber {
  public static final String UPRN_RE = "^\\d{1,13}$";
  public static final long UPRN_MIN = 0L;
  public static final long UPRN_MAX = 9999999999999L;

  @JsonCreator
  public static UniquePropertyReferenceNumber create(String uprn) {
    return new UniquePropertyReferenceNumber(uprn);
  }

  public UniquePropertyReferenceNumber(String str) {
    if (!StringUtils.isBlank(str)) {
      try {
        Long uprn = Long.parseLong(str);
        if (uprn.longValue() >= UPRN_MIN && uprn.longValue() <= UPRN_MAX) {
          this.value = uprn;
        } else {
          throw new IllegalArgumentException("String '" + uprn + "' is not a valid UPRN");
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException();
      }
    }
  }

  @JsonValue private long value;
}
