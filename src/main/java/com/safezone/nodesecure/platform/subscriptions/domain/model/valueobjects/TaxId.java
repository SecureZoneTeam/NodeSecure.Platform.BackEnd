package com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects;

import java.util.Set;

/**
 * Peruvian RUC (Registro Único de Contribuyentes): 11 digits, a valid taxpayer prefix
 * (10, 15, 16, 17 or 20) and the SUNAT modulo-11 check digit.
 */
public record TaxId(String value) {

    private static final int[] WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final Set<String> PREFIXES = Set.of("10", "15", "16", "17", "20");

    public TaxId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("companies.error.taxId.notBlank");
        }
        value = value.trim();
        if (!isValid(value)) {
            throw new IllegalArgumentException("companies.error.taxId.invalid");
        }
    }

    public static boolean isValid(String ruc) {
        if (ruc == null || !ruc.matches("\\d{11}") || !PREFIXES.contains(ruc.substring(0, 2))) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += Character.getNumericValue(ruc.charAt(i)) * WEIGHTS[i];
        }
        int check = 11 - (sum % 11);
        if (check == 10) check = 0;
        if (check == 11) check = 1;
        return check == Character.getNumericValue(ruc.charAt(10));
    }
}
