package org.sentinel.reportservice.model;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder(toBuilder = true)
@Jacksonized
public record Address(
        @JacksonXmlProperty(isAttribute = true) @NonNull String addr,
        @JacksonXmlProperty(localName = "addrtype", isAttribute = true) AddressType addressType,
        @JacksonXmlProperty(isAttribute = true) String vendor
) {
    public Address {
        if (addressType == null) {
            addressType = AddressType.IPV4;
        }
    }
}