package uk.gov.ons.ctp.common.domain;

import java.util.Optional;

public enum EstabType {
  // code for OTHER deliberate - if AIMS code is actually "" or "Floating Caravan Palace"
  // either way forCode() will return OTHER
  OTHER("", null),
  HALL_OF_RESIDENCE("HALL OF RESIDENCE", AddressType.CE),
  CARE_HOME("CARE HOME", AddressType.CE),
  HOSPITAL("HOSPITAL", AddressType.CE),
  HOSPICE("HOSPICE", AddressType.CE),
  MENTAL_HEALTH_HOSPITAL("MENTAL HEALTH HOSPITAL", AddressType.CE),
  MEDICAL_CARE_OTHER("MEDICAL CARE OTHER", AddressType.CE),
  BOARDING_SCHOOL("BOARDING SCHOOL", AddressType.CE),
  LOW_OR_MEDIUM_SECURE_MENTAL_HEALTH(
      "LOW/MEDIUM SECURE MENTAL HEALTH", AddressType.CE, SecurityType.SECURE),
  HIGH_SECURE_MENTAL_HEALTH("HIGH SECURE MENTAL HEALTH", AddressType.CE, SecurityType.SECURE),
  HOTEL("HOTEL", AddressType.CE),
  YOUTH_HOSTEL("YOUTH HOSTEL", AddressType.CE),
  HOSTEL("HOSTEL", AddressType.CE),
  MILITARY_SLA("MILITARY SLA", AddressType.CE, SecurityType.SECURE),
  MILITARY_US("MILITARY US", AddressType.CE, SecurityType.SECURE),
  RELIGIOUS_COMMUNITY("RELIGIOUS COMMUNITY", AddressType.CE),
  RESIDENTIAL_CHILDRENS_HOME("RESIDENTIAL CHILDRENS HOME", AddressType.CE),
  EDUCATION_OTHER("EDUCATION OTHER", AddressType.CE),
  PRISON("PRISON", AddressType.CE, SecurityType.SECURE),
  IMMIGRATION_REMOVAL_CENTRE("IMMIGRATION REMOVAL CENTRE", AddressType.CE, SecurityType.SECURE),
  APPROVED_PREMISES("APPROVED PREMISES", AddressType.CE, SecurityType.SECURE),
  ROUGH_SLEEPER("ROUGH SLEEPER", AddressType.CE),
  STAFF_ACCOMMODATION("STAFF ACCOMMODATION", AddressType.CE),
  CAMPHILL("CAMPHILL", AddressType.CE),
  HOLIDAY_PARK("HOLIDAY PARK", AddressType.CE),
  HOUSEHOLD("HOUSEHOLD", AddressType.HH),
  SHELTERED_ACCOMMODATION("SHELTERED ACCOMMODATION", AddressType.HH),
  RESIDENTIAL_CARAVAN("RESIDENTIAL CARAVAN", AddressType.HH),
  RESIDENTIAL_BOAT("RESIDENTIAL BOAT", AddressType.HH),
  GATED_APARTMENTS("GATED APARTMENTS", AddressType.HH),
  MOD_HOUSEHOLDS("MOD HOUSEHOLDS", AddressType.HH),
  FOREIGN_OFFICES("FOREIGN OFFICES", AddressType.HH),
  CASTLES("CASTLES", AddressType.HH),
  GRT_SITE("GRT SITE", AddressType.HH),
  MILITARY_SFA("MILITARY SFA", AddressType.SPG, SecurityType.SECURE),
  EMBASSY("EMBASSY", AddressType.SPG, SecurityType.SECURE),
  ROYAL_HOUSEHOLD("ROYAL HOUSEHOLD", AddressType.SPG, SecurityType.SECURE),
  CARAVAN_SITE("CARAVAN SITE", AddressType.SPG),
  MARINA("MARINA", AddressType.SPG),
  TRAVELLING_PERSONS("TRAVELLING PERSONS", AddressType.SPG),
  TRANSIENT_PERSONS("TRANSIENT PERSONS", AddressType.SPG);

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

  private EstabType(String code, AddressType addressType) {
    this(code, addressType, SecurityType.NOT_SECURE);
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
