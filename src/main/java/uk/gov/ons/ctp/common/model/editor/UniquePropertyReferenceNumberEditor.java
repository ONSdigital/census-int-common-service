package uk.gov.ons.ctp.common.model.editor;

import java.beans.PropertyEditorSupport;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;

public class UniquePropertyReferenceNumberEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    try {
      Long uprn = Long.parseLong(text);
      if (uprn.longValue() >= UniquePropertyReferenceNumber.UPRN_MIN
          && uprn.longValue() <= UniquePropertyReferenceNumber.UPRN_MAX) {
        setValue(new UniquePropertyReferenceNumber(uprn));
      } else {
        throw new IllegalArgumentException();
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public String getAsText() {
    return ((UniquePropertyReferenceNumber) this.getValue()).toString();
  }
}
