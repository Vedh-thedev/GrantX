package com.grantx.controller;

import com.grantx.dto.ApiResponse;
import com.grantx.dto.ReviewDto;
import com.grantx.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
@RequiredArgsConstructor
public class FacultyController {

    private final ReviewService reviewService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<List<ReviewDto.AssignmentResponse>>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ReviewDto.AssignmentResponse> assignments = reviewService.getFacultyAssignments(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Faculty dashboard loaded", assignments));
    }

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<ReviewDto.AssignmentResponse>>> getAssignments(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ReviewDto.AssignmentResponse> assignments = reviewService.getFacultyAssignments(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Assignments fetched", assignments));
    }

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewDto.ReviewResponse>> submitReview(
            @Valid @RequestBody ReviewDto.SubmitReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReviewDto.ReviewResponse review = reviewService.submitReview(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", review));
    }

    @GetMapping("/reviews/{assignmentId}")
    public ResponseEntity<ApiResponse<ReviewDto.ReviewResponse>> getMyReview(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReviewDto.ReviewResponse review = reviewService.getReviewForAssignment(assignmentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Review fetched", review));
    }
}
