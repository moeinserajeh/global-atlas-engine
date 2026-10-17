package com.globalatlas.country;

public record CountrySummary(
        long id,
        String iso2,
        String iso3,
        String nameEn,
        String nameFa
) {
}
