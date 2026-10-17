package com.globalatlas.region;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/regions")
public class RegionSearchController {
    private final RegionRepository repository;

    public RegionSearchController(RegionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/search")
    public List<RegionSearchResult> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit
    ) {
        String query = q == null ? "" : q.strip();
        if (query.length() < 2 || query.length() > 100 || limit < 1 || limit > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "q must contain 2..100 characters and limit must be 1..100"
            );
        }
        return repository.search(query, limit);
    }
}
