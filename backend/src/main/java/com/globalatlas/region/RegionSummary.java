package com.globalatlas.region;

public record RegionSummary(
        long id,
        long countryId,
        String code,
        String nameEn,
        String nameFa,
        int adminLevel
) {
}
