package com.team14.amazon.order.controller;

import com.team14.amazon.order.dto.ProductRecommendationDTO;
import com.team14.amazon.order.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S3-F12 — GET /api/orders/recommendations?productId={id}&limit={n}
 *
 * Auth (§10.3.3 a): protected by JwtAuthenticationFilter via SecurityConfig
 * (anyRequest().authenticated()) — missing/invalid token → 401 before reaching
 * this controller, so no inline auth check is needed here.
 *
 * 404 (§10.3.3 b) is raised by the service via ResponseStatusException.
 */
@RestController
@RequestMapping("/api/orders")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<ProductRecommendationDTO>> getRecommendations(
            @RequestParam Long productId,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        return ResponseEntity.ok(recommendationService.getRecommendations(productId, limit));
    }
}
