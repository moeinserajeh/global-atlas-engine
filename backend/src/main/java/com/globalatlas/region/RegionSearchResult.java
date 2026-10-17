package com.globalatlas.region;

public record RegionSearchResult(
        long id,
        String countryIso2,
        String countryNameEn,
        String countryNameFa,
        String code,
        String nameEn,
        String nameFa,
        int adminLevel
) {}
