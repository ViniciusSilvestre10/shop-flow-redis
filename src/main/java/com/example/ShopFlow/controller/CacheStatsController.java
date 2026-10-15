package com.example.ShopFlow.controller;


import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;



@RestController
@RequestMapping("/admin/cache")
public class CacheStatsController {

    private final MeterRegistry registry;

    public CacheStatsController(MeterRegistry registry) {
        this.registry = registry;

    }

    @GetMapping("/hit-ratio")
    public Map<String, Object> hitRatio(@RequestParam(defaultValue = "produtos") String cache){
        double hits = count(cache, "hit");
        double misses = count(cache, "miss");
        double total = hits + misses;
        return Map.of(
                "cache", cache,
                "hits", hits,
                "misses", misses,
                "hitRatio", total == 0 ? 0 : hits / total
        );
    }

    private double count(String cache, String result) {
        var counter = registry.find("cache.gets")
                .tags("cache", cache, "result", result)
                .functionCounter();
        return counter == null ? 0 : counter.count();
    }



}
