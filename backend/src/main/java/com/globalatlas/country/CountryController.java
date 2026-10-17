package com.globalatlas.country;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final CountryRepository countryRepository;

    public CountryController(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @GetMapping
    public List<CountrySummary> listCountries(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        if (limit < 1 || limit > MAX_PAGE_SIZE || offset < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "limit must be 1..100 and offset must be non-negative"
            );
        }
        return countryRepository.findAll(Math.min(limit, DEFAULT_PAGE_SIZE), offset);
    }

    @GetMapping("/{iso2}")
    public CountrySummary getCountry(@PathVariable String iso2) {
        if (!iso2.matches("(?i)[a-z]{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ISO-3166 alpha-2 code required");
        }
        return countryRepository.findByIso2(iso2)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found"));
    }
}
