package com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects;

import java.util.Locale;
import java.util.Objects;

/** Value object representing the postal address of a warehouse. */
public record StreetAddress(String street, String district, String city, String country) {

    public StreetAddress {
        street = required(street, "warehouse.error.address.street.required", 160);
        district = required(district, "warehouse.error.address.district.required", 100);
        city = required(city, "warehouse.error.address.city.required", 100);
        country = required(country, "warehouse.error.address.country.required", 2).toUpperCase(Locale.ROOT);
        if (country.length() != 2) {
            throw new IllegalArgumentException("warehouse.error.address.country.invalid");
        }
    }

    private static String required(String value, String messageKey, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(messageKey);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("warehouse.error.address.tooLong");
        }
        return normalized;
    }

    public String formatted() {
        return String.join(", ", street, district, city, country);
    }
}
