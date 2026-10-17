package com.globalatlas.region;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/countries/{iso2}/regions")
public class RegionController {
    private static final int MAX_PAGE_SIZE = 100;

    private final RegionRepository regionRepository;

    public RegionController(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @GetMapping
    public List<RegionSummary> listRegions(
            @PathVariable String iso2,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        if (!iso2.matches("(?i)[a-z]{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ISO-3166 alpha-2 code required");
        }
        if (limit < 1 || limit > MAX_PAGE_SIZE || offset < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 1..100 and offset non-negative");
        }
        return regionRepository.findByCountryIso2(iso2, limit, offset);
    }
}
