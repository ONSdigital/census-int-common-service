package uk.gov.ons.ctp.common.domain;

public enum Language {
  GAELIC("ga"),
  WELSH("cy"),
  ULSTERSCOTCH("eo"),
  ENGLISH("en");

  private String isoLikeCode;

  private Language(String isoLikeCode) {
    this.isoLikeCode = isoLikeCode;
  }

  public String getIsoLikeCode() {
    return isoLikeCode;
  }
}
