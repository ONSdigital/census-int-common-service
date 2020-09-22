package uk.gov.ons.ctp.common.domain;

import java.util.Optional;

public enum EstabType {
  // code for OTHER deliberate - if AIMS code is actually "" or "Floating Caravan Palace"
  // either way forCode() will return OTHER
  OTHER("OTHER", null, SecurityType.NOT_SECURE),
  HALL_OF_RESIDENCE("HALL OF RESIDENCE", AddressType.CE, SecurityType.NOT_SECURE),
  CARE_HOME("CARE HOME", AddressType.CE, SecurityType.NOT_SECURE),
  HOSPITAL("HOSPITAL", AddressType.CE, SecurityType.NOT_SECURE),
  HOSPICE("HOSPICE", AddressType.CE, SecurityType.NOT_SECURE),
  BOARDING_SCHOOL("BOARDING SCHOOL", AddressType.CE, SecurityType.NOT_SECURE),
  LOW_OR_MEDIUM_SECURE_MENTAL_HEALTH(
      "LOW/MEDIUM SECURE MENTAL HEALTH", AddressType.CE, SecurityType.SECURE),
  HIGH_SECURE_MENTAL_HEALTH("HIGH SECURE MENTAL HEALTH", AddressType.CE, SecurityType.SECURE),
  HOTEL("HOTEL", AddressType.CE, SecurityType.NOT_SECURE),
  YOUTH_HOSTEL("YOUTH HOSTEL", AddressType.CE, SecurityType.NOT_SECURE),
  HOSTEL("HOSTEL", AddressType.CE, SecurityType.NOT_SECURE),
  MILITARY_SLA("MILITARY SLA", AddressType.CE, SecurityType.SECURE),
  MILITARY_US_SLA("MILITARY US SLA", AddressType.CE, SecurityType.SECURE),
  RELIGIOUS_COMMUNITY("RELIGIOUS COMMUNITY", AddressType.CE, SecurityType.NOT_SECURE),
  RESIDENTIAL_CHILDRENS_HOME("RESIDENTIAL CHILDRENS HOME", AddressType.CE, SecurityType.NOT_SECURE),
  EDUCATION_OTHER("EDUCATION OTHER", AddressType.CE, SecurityType.NOT_SECURE),
  PRISON("PRISON", AddressType.CE, SecurityType.SECURE),
  IMMIGRATION_REMOVAL_CENTRE("IMMIGRATION REMOVAL CENTRE", AddressType.CE, SecurityType.SECURE),
  APPROVED_PREMISES("APPROVED PREMISES", AddressType.CE, SecurityType.SECURE),
  ROUGH_SLEEPER("ROUGH SLEEPER", AddressType.CE, SecurityType.NOT_SECURE),
  STAFF_ACCOMMODATION("STAFF ACCOMMODATION", AddressType.CE, SecurityType.NOT_SECURE),
  HOUSEHOLD("HOUSEHOLD", AddressType.HH, SecurityType.NOT_SECURE),
  SHELTERED_ACCOMMODATION("SHELTERED ACCOMMODATION", AddressType.HH, SecurityType.NOT_SECURE),
  RESIDENTIAL_CARAVAN("RESIDENTIAL CARAVAN", AddressType.HH, SecurityType.NOT_SECURE),
  RESIDENTIAL_BOAT("RESIDENTIAL BOAT", AddressType.HH, SecurityType.NOT_SECURE),
  MILITARY_SFA("MILITARY SFA", AddressType.SPG, SecurityType.SECURE),
  EMBASSY("EMBASSY", AddressType.SPG, SecurityType.SECURE),
  ROYAL_HOUSEHOLD("ROYAL HOUSEHOLD", AddressType.SPG, SecurityType.SECURE),
  CARAVAN("CARAVAN", AddressType.SPG, SecurityType.NOT_SECURE),
  MARINA("MARINA", AddressType.SPG, SecurityType.NOT_SECURE),
  TRAVELLING_PERSONS("TRAVELLING PERSONS", AddressType.SPG, SecurityType.NOT_SECURE),
  TRANSIENT_PERSONS("TRANSIENT PERSONS", AddressType.SPG, SecurityType.NOT_SECURE),
  MILITARY_US_SFA("MILITARY US SFA", AddressType.SPG, SecurityType.SECURE);

  private static enum SecurityType {
    SECURE,
    NOT_SECURE
  }

  private String code;
  private AddressType addressType;
  private SecurityType securityType;

  private EstabType(String code, AddressType addressType, SecurityType securityType) {
    this.code = code;
    this.addressType = addressType;
    this.securityType = securityType;
  }

  public String getCode() {
    return code;
  }

  public boolean isSecure() {
    return securityType == SecurityType.SECURE;
  }

  public Optional<AddressType> getAddressType() {
    return addressType == null ? Optional.empty() : Optional.of(addressType);
  }

  public static EstabType forCode(String code) {
    for (EstabType estabType : EstabType.values()) {
      if (estabType.code.equals(code.toUpperCase())) {
        return estabType;
      }
    }
    return EstabType.OTHER;
  }
}
