package uk.gov.ons.ctp.common.model;

import java.util.Optional;

public enum EstabType {
  OTHER("", AddressType.HH), // not sure about the address type for this one - depends how OTHER gets used if at all
  HALL_OF_RESIDENCE("HALL OF RESIDENCE", AddressType.CE),
  CARE_HOME("CARE HOME", AddressType.CE),
  HOSPITAL("HOSPITAL", AddressType.CE),
  HOSPICE("HOSPICE", AddressType.CE),
  MENTAL_HEALTH_HOSPITAL("MENTAL HEALTH HOSPITAL", AddressType.CE),
  MEDICAL_CARE_OTHER("MEDICAL CARE OTHER", AddressType.CE),
  BOARDING_SCHOOL("BOARDING SCHOOL", AddressType.CE),
  LOW_OR_MEDIUM_SECURE_MENTAL_HEALTH("LOW/MEDIUM SECURE MENTAL HEALTH", AddressType.CE),
  HIGH_SECURE_MENTAL_HEALTH("HIGH SECURE MENTAL HEALTH", AddressType.CE),
  HOTEL("HOTEL", AddressType.CE),
  YOUTH_HOSTEL("YOUTH HOSTEL", AddressType.CE),
  HOSTEL("HOSTEL", AddressType.CE),
  MILITARY_SLA("MILITARY SLA", AddressType.CE),
  MILITARY_US("MILITARY US", AddressType.CE),
  RELIGIOUS_COMMUNITY("RELIGIOUS COMMUNITY", AddressType.CE),
  RESIDENTIAL_CHILDRENS_HOME("RESIDENTIAL CHILDRENS HOME", AddressType.CE),
  EDUCATION_OTHER("EDUCATION OTHER", AddressType.CE),
  PRISON("PRISON", AddressType.CE),
  IMMIGRATION_REMOVAL_CENTRE("IMMIGRATION REMOVAL CENTRE", AddressType.CE),
  APPROVED_PREMISES("APPROVED PREMISES", AddressType.CE),
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
  MILITARY_SFA("MILITARY SFA", AddressType.SPG),
  EMBASSY("EMBASSY", AddressType.SPG),
  ROYAL_HOUSEHOLD("ROYAL HOUSEHOLD", AddressType.SPG),
  CARAVAN_SITE("CARAVAN SITE", AddressType.SPG),
  MARINA("MARINA", AddressType.SPG),
  TRAVELLING_PERSONS("TRAVELLING PERSONS", AddressType.SPG),
  TRANSIENT_PERSONS("TRANSIENT PERSONS", AddressType.SPG);

  private String code;
  private AddressType addressType;

  private EstabType(String code, AddressType addressType) {
    this.code = code;
    this.addressType = addressType;
  }

  public String getCode() {
    return code;
  }

  public AddressType getAddressType() {
    return addressType;
  }

  public static Optional<EstabType> forCode(String code) {
    for (EstabType estabType : EstabType.values()) {
      if (estabType.code.equals(code.toUpperCase())) {
        return Optional.of(estabType);
      }
    }
    return Optional.empty();
  }
}
