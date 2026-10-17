package com.globalatlas.country;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CountrySummary(
        long id,
        String iso2,
        String iso3,
        String nameEn,
        String nameFa,
        String capital,
        Long population,
        BigDecimal areaKm2,
        long regionCount,
        String dataSource,
        String dataLicense,
        String datasetVersion,
        LocalDate referenceDate
) {
}
