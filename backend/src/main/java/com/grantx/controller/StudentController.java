package com.grantx.controller;

import com.grantx.dto.ApiResponse;
import com.grantx.dto.ProposalDto;
import com.grantx.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final ProposalService proposalService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> stats = proposalService.getStudentDashboardStats(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Dashboard loaded", stats));
    }

    @PostMapping("/proposals")
    public ResponseEntity<ApiResponse<ProposalDto.ProposalResponse>> createProposal(
            @Valid @RequestBody ProposalDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProposalDto.ProposalResponse response = proposalService.createProposal(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(request.isSaveDraft() ? "Draft saved successfully" : "Proposal submitted successfully", response));
    }

    @GetMapping("/proposals")
    public ResponseEntity<ApiResponse<List<ProposalDto.ProposalResponse>>> getMyProposals(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProposalDto.ProposalResponse> proposals = proposalService.getStudentProposals(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Proposals fetched", proposals));
    }

    @GetMapping("/proposals/{id}")
    public ResponseEntity<ApiResponse<ProposalDto.ProposalDetailResponse>> getProposal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProposalDto.ProposalDetailResponse detail = proposalService.getProposalDetail(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Proposal fetched", detail));
    }

    @PostMapping("/proposals/{id}/submit")
    public ResponseEntity<ApiResponse<ProposalDto.ProposalResponse>> submitDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProposalDto.ProposalResponse response = proposalService.submitDraft(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Proposal submitted successfully", response));
    }
}
