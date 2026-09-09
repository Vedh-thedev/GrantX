package com.grantx.controller;

import com.grantx.dto.ApiResponse;
import com.grantx.dto.GrantDto;
import com.grantx.dto.ProposalDto;
import com.grantx.dto.ReviewDto;
import com.grantx.entity.*;
import com.grantx.repository.FacultyRepository;
import com.grantx.repository.UserRepository;
import com.grantx.service.GrantService;
import com.grantx.service.ProposalService;
import com.grantx.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProposalService proposalService;
    private final ReviewService reviewService;
    private final GrantService grantService;
    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> stats = proposalService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard loaded", stats));
    }

    @GetMapping("/proposals")
    public ResponseEntity<ApiResponse<List<ProposalDto.ProposalResponse>>> getAllProposals() {
        List<ProposalDto.ProposalResponse> proposals = proposalService.getAllProposals();
        return ResponseEntity.ok(ApiResponse.success("Proposals fetched", proposals));
    }

    @GetMapping("/proposals/{id}")
    public ResponseEntity<ApiResponse<ProposalDto.ProposalDetailResponse>> getProposal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProposalDto.ProposalDetailResponse detail = proposalService.getProposalDetail(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Proposal fetched", detail));
    }

    @PostMapping("/assign-evaluators")
    public ResponseEntity<ApiResponse<?>> assignEvaluators(
            @Valid @RequestBody ReviewDto.AssignEvaluatorsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.assignEvaluators(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Evaluators assigned successfully"));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<ReviewDto.ReviewMonitorResponse>>> getAllReviews() {
        List<ReviewDto.ReviewMonitorResponse> reviews = reviewService.getAllReviewsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched", reviews));
    }

    @GetMapping("/proposals/{id}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewDto.ReviewResponse>>> getProposalReviews(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ReviewDto.ReviewResponse> reviews = reviewService.getReviewsForProposal(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched", reviews));
    }

    @GetMapping("/decisions/{proposalId}")
    public ResponseEntity<ApiResponse<GrantDto.DecisionSummaryResponse>> getDecisionSummary(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal UserDetails userDetails) {
        GrantDto.DecisionSummaryResponse summary = grantService.getDecisionSummary(proposalId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Decision summary fetched", summary));
    }

    @PostMapping("/decisions")
    public ResponseEntity<ApiResponse<?>> makeDecision(
            @Valid @RequestBody GrantDto.DecisionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        GrantDecision decision = grantService.makeDecision(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Decision recorded: " + decision.getDecision().name(), null));
    }

    @GetMapping("/grants")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllGrants() {
        List<Map<String, Object>> grants = grantService.getAllGrants().stream().map(g -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", g.getId());
            map.put("requestedAmount", g.getRequestedAmount());
            map.put("approvedAmount", g.getApprovedAmount());
            map.put("grantStatus", g.getGrantStatus() != null ? g.getGrantStatus().name() : null);
            map.put("sanctionDate", g.getSanctionDate() != null ? g.getSanctionDate().toString() : null);
            map.put("grantReference", g.getGrantReference());
            map.put("remarks", g.getRemarks());
            map.put("projectPhase", g.getProjectPhase());
            if (g.getProposal() != null) {
                Map<String, Object> pMap = new java.util.HashMap<>();
                pMap.put("id", g.getProposal().getId());
                pMap.put("proposalNumber", g.getProposal().getProposalNumber());
                pMap.put("title", g.getProposal().getTitle());
                pMap.put("status", g.getProposal().getStatus() != null ? g.getProposal().getStatus().name() : null);
                map.put("proposal", pMap);
            }
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Grants fetched", grants));
    }

    @PostMapping("/milestones")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createMilestone(
            @Valid @RequestBody GrantDto.CreateMilestoneRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Milestone m = grantService.createMilestone(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Milestone created", mapMilestoneToMap(m)));
    }

    @PutMapping("/milestones/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMilestone(
            @PathVariable Long id,
            @RequestBody GrantDto.UpdateMilestoneRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Milestone m = grantService.updateMilestone(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Milestone updated", mapMilestoneToMap(m)));
    }

    @GetMapping("/proposals/{id}/milestones")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMilestones(@PathVariable Long id) {
        List<Map<String, Object>> milestones = grantService.getMilestonesForProposal(id).stream()
                .map(this::mapMilestoneToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Milestones fetched", milestones));
    }

    private Map<String, Object> mapMilestoneToMap(Milestone m) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", m.getId());
        map.put("title", m.getTitle());
        map.put("description", m.getDescription());
        map.put("dueDate", m.getDueDate() != null ? m.getDueDate().toString() : null);
        map.put("status", m.getStatus() != null ? m.getStatus().name() : "NOT_STARTED");
        map.put("completionPercentage", m.getCompletionPercentage());
        map.put("remarks", m.getRemarks());
        if (m.getProposal() != null) {
            Map<String, Object> pMap = new java.util.HashMap<>();
            pMap.put("id", m.getProposal().getId());
            pMap.put("proposalNumber", m.getProposal().getProposalNumber());
            pMap.put("title", m.getProposal().getTitle());
            map.put("proposal", pMap);
        }
        return map;
    }

    @GetMapping("/faculty")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllFaculty() {
        List<Map<String, Object>> facultyList = facultyRepository.findAll().stream()
                .map(f -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", f.getId());
                    map.put("userId", f.getUser().getId());
                    map.put("fullName", f.getUser().getFullName());
                    map.put("email", f.getUser().getEmail());
                    map.put("department", f.getDepartment());
                    map.put("designation", f.getDesignation());
                    map.put("employeeId", f.getEmployeeId());
                    map.put("specialization", f.getSpecialization() != null ? f.getSpecialization() : "");
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Faculty list fetched", facultyList));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", u.getId());
                    map.put("username", u.getUsername());
                    map.put("fullName", u.getFullName());
                    map.put("email", u.getEmail());
                    map.put("role", u.getRole().name());
                    map.put("isActive", u.getIsActive());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Users fetched", users));
    }
}
