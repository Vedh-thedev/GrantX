package com.grantx.service;

import com.grantx.entity.*;
import com.grantx.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final ProposalRepository proposalRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewScoreRepository reviewScoreRepository;
    private final GrantDecisionRepository grantDecisionRepository;
    private final GrantFundingRepository grantFundingRepository;
    private final MilestoneRepository milestoneRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void initializeSampleData() {
        if (userRepository.count() > 0) {
            log.info("Database already has data. Skipping sample data initialization.");
            return;
        }
        log.info("Initializing sample data for Grant-X...");

        // === ADMIN USER ===
        User adminUser = userRepository.save(User.builder()
                .username("admin")
                .email("admin@aiht.ac.in")
                .passwordHash(passwordEncoder.encode("admin"))
                .fullName("Dr. R&D Committee Head")
                .role(User.Role.ADMIN)
                .phone("9876543210")
                .isActive(true)
                .build());

        // === FACULTY USERS ===
        User facultyUser1 = userRepository.save(User.builder()
                .username("dr.priya")
                .email("priya.sharma@aiht.ac.in")
                .passwordHash(passwordEncoder.encode("faculty123"))
                .fullName("Dr. Priya Sharma")
                .role(User.Role.FACULTY)
                .phone("9876543211")
                .isActive(true)
                .build());

        Faculty faculty1 = facultyRepository.save(Faculty.builder()
                .user(facultyUser1)
                .employeeId("FAC001")
                .department("Computer Science & Engineering")
                .designation("Associate Professor")
                .specialization("Artificial Intelligence, Machine Learning")
                .build());

        User facultyUser2 = userRepository.save(User.builder()
                .username("dr.rajan")
                .email("rajan.kumar@aiht.ac.in")
                .passwordHash(passwordEncoder.encode("faculty123"))
                .fullName("Dr. Rajan Kumar")
                .role(User.Role.FACULTY)
                .phone("9876543212")
                .isActive(true)
                .build());

        Faculty faculty2 = facultyRepository.save(Faculty.builder()
                .user(facultyUser2)
                .employeeId("FAC002")
                .department("Electronics & Communication")
                .designation("Professor")
                .specialization("IoT, Embedded Systems, Signal Processing")
                .build());

        User facultyUser3 = userRepository.save(User.builder()
                .username("dr.meena")
                .email("meena.rajesh@aiht.ac.in")
                .passwordHash(passwordEncoder.encode("faculty123"))
                .fullName("Dr. Meena Rajesh")
                .role(User.Role.FACULTY)
                .phone("9876543213")
                .isActive(true)
                .build());

        Faculty faculty3 = facultyRepository.save(Faculty.builder()
                .user(facultyUser3)
                .employeeId("FAC003")
                .department("Mechanical Engineering")
                .designation("Assistant Professor")
                .specialization("Robotics, Automation, Manufacturing")
                .build());

        // === STUDENT USERS ===
        User studentUser1 = userRepository.save(User.builder()
                .username("arjun.cs21")
                .email("arjun.kumar@student.aiht.ac.in")
                .passwordHash(passwordEncoder.encode("student123"))
                .fullName("Arjun Kumar")
                .role(User.Role.STUDENT)
                .phone("8765432101")
                .isActive(true)
                .build());

        Student student1 = studentRepository.save(Student.builder()
                .user(studentUser1)
                .registerNumber("21CSBS001")
                .department("Computer Science & Business Systems")
                .yearOfStudy(3)
                .build());

        User studentUser2 = userRepository.save(User.builder()
                .username("divya.ec22")
                .email("divya.patel@student.aiht.ac.in")
                .passwordHash(passwordEncoder.encode("student123"))
                .fullName("Divya Patel")
                .role(User.Role.STUDENT)
                .phone("8765432102")
                .isActive(true)
                .build());

        Student student2 = studentRepository.save(Student.builder()
                .user(studentUser2)
                .registerNumber("22ECE047")
                .department("Electronics & Communication")
                .yearOfStudy(2)
                .build());

        User studentUser3 = userRepository.save(User.builder()
                .username("rahul.mech21")
                .email("rahul.verma@student.aiht.ac.in")
                .passwordHash(passwordEncoder.encode("student123"))
                .fullName("Rahul Verma")
                .role(User.Role.STUDENT)
                .phone("8765432103")
                .isActive(true)
                .build());

        Student student3 = studentRepository.save(Student.builder()
                .user(studentUser3)
                .registerNumber("21MECH015")
                .department("Mechanical Engineering")
                .yearOfStudy(3)
                .build());

        // === PROPOSAL 1 - APPROVED (with milestones) ===
        Proposal proposal1 = proposalRepository.save(Proposal.builder()
                .proposalNumber("GX-2024-001")
                .student(student1)
                .title("AI-Powered Smart Waste Segregation System")
                .problemStatement("Municipal solid waste management is a critical challenge in urban areas. Current manual segregation is inefficient, unhygienic, and costly. There is no automated, intelligent system that can classify waste in real-time.")
                .proposedSolution("An AI-powered conveyor-belt system using computer vision and deep learning models (YOLOv8) to classify waste into biodegradable, recyclable, hazardous, and general categories automatically.")
                .abstrakt("This project proposes an automated waste segregation system leveraging YOLOv8 object detection on a Raspberry Pi 4 with a high-speed camera. The system classifies waste in real-time, controls servo-based diverter gates, and logs waste data to a cloud dashboard for municipal tracking.")
                .objectives("1. Achieve 95%+ classification accuracy for common waste types. 2. Process waste at minimum 100 items/minute. 3. Provide real-time analytics dashboard. 4. Reduce manual segregation cost by 60%.")
                .technologyDomain("Artificial Intelligence, Computer Vision, IoT, Embedded Systems")
                .innovationCategory("Environmental Technology")
                .existingSolutions("Manual sorting by workers, basic sensor-based sorters that classify only by weight/color, expensive industrial robots limited to large facilities.")
                .noveltyExplanation("First implementation of YOLOv8 on edge devices for multi-category waste classification with real-time feedback loop and municipal analytics integration in Indian urban context.")
                .differenceFromExisting("Existing solutions classify only 2-3 categories. Our system classifies 8 categories with 95% accuracy using edge AI without cloud dependency for real-time classification.")
                .innovationSignificance("Addresses Swachh Bharat mission goals, reduces labor costs, improves recycling rates, and generates actionable data for municipal corporations.")
                .patentPotential(Proposal.PatentPotential.HIGH)
                .noveltyDeclaration(true)
                .status(Proposal.ProposalStatus.MILESTONE_TRACKING)
                .aggregatedScore(new BigDecimal("8.35"))
                .submissionDate(LocalDateTime.now().minusDays(60))
                .build());

        ProposalBudget budget1 = new ProposalBudget();
        budget1.setProposal(proposal1);
        budget1.setEquipmentCost(new BigDecimal("25000"));
        budget1.setSoftwareCost(new BigDecimal("5000"));
        budget1.setMaterialsCost(new BigDecimal("8000"));
        budget1.setPrototypeCost(new BigDecimal("12000"));
        budget1.setTestingCost(new BigDecimal("5000"));
        budget1.setOtherCost(new BigDecimal("3000"));
        budget1.calculateTotal();
        proposal1.setBudget(budget1);

        // PROPOSAL 2 - UNDER REVIEW ===
        Proposal proposal2 = proposalRepository.save(Proposal.builder()
                .proposalNumber("GX-2024-002")
                .student(student2)
                .title("Low-Cost IoT Smart Water Quality Monitoring Network")
                .problemStatement("Rural communities lack access to real-time water quality monitoring. Contaminated water causes preventable diseases. Existing solutions are expensive laboratory-based systems unavailable to villages.")
                .proposedSolution("A distributed sensor network using ESP32 microcontrollers with pH, turbidity, TDS, temperature, and dissolved oxygen sensors, transmitting data via LoRa to a central dashboard accessible via mobile.")
                .abstrakt("This project develops an affordable IoT-based water quality monitoring system deployable in rural areas. Sensors measure key water quality parameters and transmit data wirelessly using LoRaWAN technology to a solar-powered gateway, making the data available on a mobile application.")
                .objectives("1. Monitor 6 water quality parameters continuously. 2. Cost per node under Rs. 2000. 3. Battery life of 6+ months. 4. Alert system for parameter threshold violations.")
                .technologyDomain("IoT, Wireless Communication, Embedded Systems, Mobile Computing")
                .innovationCategory("Healthcare & Environment")
                .existingSolutions("Lab-based water testing kits, expensive industrial water monitoring systems, manual testing by government agencies.")
                .noveltyExplanation("Multi-parameter sensing with LoRa for long-range rural deployment at fraction of existing costs, with SMS alerts for immediate community notification.")
                .differenceFromExisting("Cost reduction by 85% over existing solutions while maintaining accuracy. First system designed specifically for Indian rural topology with offline operation capability.")
                .innovationSignificance("Directly impacts public health in rural India, aligns with Jal Jeevan Mission goals, scalable to national level.")
                .patentPotential(Proposal.PatentPotential.HIGH)
                .noveltyDeclaration(true)
                .status(Proposal.ProposalStatus.REVIEW_1_COMPLETED)
                .submissionDate(LocalDateTime.now().minusDays(30))
                .build());

        ProposalBudget budget2 = new ProposalBudget();
        budget2.setProposal(proposal2);
        budget2.setEquipmentCost(new BigDecimal("18000"));
        budget2.setSoftwareCost(new BigDecimal("2000"));
        budget2.setMaterialsCost(new BigDecimal("6000"));
        budget2.setPrototypeCost(new BigDecimal("8000"));
        budget2.setTestingCost(new BigDecimal("4000"));
        budget2.setOtherCost(new BigDecimal("2000"));
        budget2.calculateTotal();
        proposal2.setBudget(budget2);

        // PROPOSAL 3 - SUBMITTED/PENDING ASSIGNMENT ===
        Proposal proposal3 = proposalRepository.save(Proposal.builder()
                .proposalNumber("GX-2024-003")
                .student(student3)
                .title("Autonomous Agricultural Robot for Precision Farming")
                .problemStatement("Indian farmers face labor shortages during critical farming operations. Manual pesticide spraying causes health risks. Precision farming techniques require expensive foreign equipment.")
                .proposedSolution("A GPS-guided autonomous agricultural robot with computer vision for crop disease detection and variable-rate pesticide spraying, controllable via mobile app.")
                .abstrakt("This project designs a compact autonomous farming robot capable of navigating field rows using GPS and ultrasonic sensors, detecting crop diseases using a trained CNN model, and applying pesticides precisely to affected areas only.")
                .objectives("1. Navigate field autonomously with <5cm position error. 2. Detect 15 common crop diseases with 90% accuracy. 3. Reduce pesticide usage by 40%. 4. Complete 1-acre operation on single charge.")
                .technologyDomain("Robotics, Computer Vision, Agriculture Technology, GPS Navigation")
                .innovationCategory("Agriculture & Sustainability")
                .existingSolutions("Manual spraying with backpack sprayers, expensive imported tractors with GPS, drone-based spraying limited by regulations and cost.")
                .noveltyExplanation("First affordable autonomous ground robot designed for Indian small-farm dimensions (<2 acres) with integrated disease detection.")
                .differenceFromExisting("Designed for sub-2-acre Indian farm plots unlike foreign solutions built for large-scale farms. Disease detection + autonomous operation combined at <Rs. 50,000 cost.")
                .innovationSignificance("Addresses agricultural labor crisis, reduces pesticide health hazards, increases crop yield quality, enables precision farming for small farmers.")
                .patentPotential(Proposal.PatentPotential.HIGH)
                .noveltyDeclaration(true)
                .status(Proposal.ProposalStatus.PENDING_EVALUATOR_ASSIGNMENT)
                .submissionDate(LocalDateTime.now().minusDays(5))
                .build());

        ProposalBudget budget3 = new ProposalBudget();
        budget3.setProposal(proposal3);
        budget3.setEquipmentCost(new BigDecimal("30000"));
        budget3.setSoftwareCost(new BigDecimal("5000"));
        budget3.setMaterialsCost(new BigDecimal("10000"));
        budget3.setPrototypeCost(new BigDecimal("15000"));
        budget3.setTestingCost(new BigDecimal("5000"));
        budget3.setOtherCost(new BigDecimal("5000"));
        budget3.calculateTotal();
        proposal3.setBudget(budget3);

        proposalRepository.save(proposal1);
        proposalRepository.save(proposal2);
        proposalRepository.save(proposal3);

        // === REVIEW ASSIGNMENTS FOR PROPOSAL 1 ===
        ReviewAssignment assign1a = reviewAssignmentRepository.save(ReviewAssignment.builder()
                .proposal(proposal1)
                .faculty(faculty1)
                .evaluatorNumber(1)
                .assignedBy(adminUser)
                .deadline(LocalDate.now().minusDays(30))
                .status(ReviewAssignment.AssignmentStatus.COMPLETED)
                .build());

        ReviewAssignment assign1b = reviewAssignmentRepository.save(ReviewAssignment.builder()
                .proposal(proposal1)
                .faculty(faculty2)
                .evaluatorNumber(2)
                .assignedBy(adminUser)
                .deadline(LocalDate.now().minusDays(30))
                .status(ReviewAssignment.AssignmentStatus.COMPLETED)
                .build());

        // === REVIEWS FOR PROPOSAL 1 ===
        Review review1a = Review.builder()
                .assignment(assign1a)
                .proposal(proposal1)
                .faculty(faculty1)
                .technicalComments("The project demonstrates excellent technical depth. The choice of YOLOv8 for edge deployment is well-justified. The proposed conveyor mechanism is feasible.")
                .strengths("Strong AI foundation, clear implementation path, addresses real social problem, has clear metrics for success.")
                .weaknesses("Power consumption of Raspberry Pi 4 may be high for continuous operation. Needs more detail on environmental sensor durability.")
                .recommendation(Review.Recommendation.STRONGLY_RECOMMEND)
                .additionalRemarks("This project has significant potential for commercialization and social impact. Recommend for full grant approval.")
                .isLocked(true)
                .build();

        ReviewScore score1a = ReviewScore.builder()
                .noveltyScore(new BigDecimal("9.0"))
                .feasibilityScore(new BigDecimal("8.0"))
                .commercialImpactScore(new BigDecimal("8.5"))
                .weightedTotal(BigDecimal.ZERO)
                .build();
        score1a.calculateWeightedTotal();

        review1a = reviewRepository.save(review1a);
        score1a.setReview(review1a);
        reviewScoreRepository.save(score1a);
        review1a.setWeightedScore(score1a.getWeightedTotal());
        review1a.setReviewScore(score1a);
        reviewRepository.save(review1a);

        Review review1b = Review.builder()
                .assignment(assign1b)
                .proposal(proposal1)
                .faculty(faculty2)
                .technicalComments("Technically sound project. The LoRa integration for data transmission is novel. Edge AI implementation shows good engineering judgment.")
                .strengths("Innovative use of edge AI, practical approach, good cost analysis, aligns with national missions.")
                .weaknesses("Dataset for Indian waste categories needs to be more comprehensive. Maintenance protocol not fully addressed.")
                .recommendation(Review.Recommendation.RECOMMEND)
                .additionalRemarks("Minor improvements needed in the maintenance and sustainability plan.")
                .isLocked(true)
                .build();

        ReviewScore score1b = ReviewScore.builder()
                .noveltyScore(new BigDecimal("8.0"))
                .feasibilityScore(new BigDecimal("8.5"))
                .commercialImpactScore(new BigDecimal("7.5"))
                .weightedTotal(BigDecimal.ZERO)
                .build();
        score1b.calculateWeightedTotal();

        review1b = reviewRepository.save(review1b);
        score1b.setReview(review1b);
        reviewScoreRepository.save(score1b);
        review1b.setWeightedScore(score1b.getWeightedTotal());
        review1b.setReviewScore(score1b);
        reviewRepository.save(review1b);

        // === GRANT DECISION FOR PROPOSAL 1 ===
        GrantDecision decision1 = grantDecisionRepository.save(GrantDecision.builder()
                .proposal(proposal1)
                .decidedBy(adminUser)
                .decision(GrantDecision.Decision.APPROVED)
                .decisionRemarks("Excellent proposal with high novelty and strong commercial potential. Full grant approved as requested.")
                .aggregatedScoreAtDecision(new BigDecimal("8.35"))
                .decisionDate(LocalDateTime.now().minusDays(40))
                .build());

        GrantFunding funding1 = grantFundingRepository.save(GrantFunding.builder()
                .proposal(proposal1)
                .decision(decision1)
                .requestedAmount(new BigDecimal("58000"))
                .approvedAmount(new BigDecimal("55000"))
                .grantStatus(GrantFunding.GrantStatus.SANCTIONED)
                .sanctionDate(LocalDate.now().minusDays(35))
                .grantReference("GX/2024/AI-WASTE/001")
                .remarks("Grant sanctioned after committee review. Equipment procurement approved.")
                .projectPhase("Phase 1: Prototype Development")
                .build());

        // === MILESTONES FOR PROPOSAL 1 ===
        milestoneRepository.save(Milestone.builder()
                .proposal(proposal1)
                .title("Component Procurement & Setup")
                .description("Procure Raspberry Pi 4, camera modules, servo motors, conveyor components, and power supply units.")
                .dueDate(LocalDate.now().minusDays(20))
                .status(Milestone.MilestoneStatus.COMPLETED)
                .completionPercentage(100)
                .remarks("All components procured and initial setup completed successfully.")
                .createdBy(adminUser)
                .build());

        milestoneRepository.save(Milestone.builder()
                .proposal(proposal1)
                .title("AI Model Training & Optimization")
                .description("Train YOLOv8 model on custom Indian waste dataset (minimum 10,000 images per category). Optimize for edge deployment.")
                .dueDate(LocalDate.now().plusDays(10))
                .status(Milestone.MilestoneStatus.IN_PROGRESS)
                .completionPercentage(65)
                .remarks("Model achieving 87% accuracy currently. Further training in progress.")
                .createdBy(adminUser)
                .build());

        milestoneRepository.save(Milestone.builder()
                .proposal(proposal1)
                .title("Prototype Assembly & Integration")
                .description("Assemble conveyor mechanism, integrate camera system, wire servo gates, and integrate with Raspberry Pi.")
                .dueDate(LocalDate.now().plusDays(30))
                .status(Milestone.MilestoneStatus.NOT_STARTED)
                .completionPercentage(0)
                .remarks("")
                .createdBy(adminUser)
                .build());

        milestoneRepository.save(Milestone.builder()
                .proposal(proposal1)
                .title("System Testing & Validation")
                .description("Test complete system with real waste samples. Validate accuracy metrics and throughput.")
                .dueDate(LocalDate.now().plusDays(60))
                .status(Milestone.MilestoneStatus.NOT_STARTED)
                .completionPercentage(0)
                .remarks("")
                .createdBy(adminUser)
                .build());

        // === REVIEW ASSIGNMENTS FOR PROPOSAL 2 ===
        ReviewAssignment assign2a = reviewAssignmentRepository.save(ReviewAssignment.builder()
                .proposal(proposal2)
                .faculty(faculty1)
                .evaluatorNumber(1)
                .assignedBy(adminUser)
                .deadline(LocalDate.now().plusDays(7))
                .status(ReviewAssignment.AssignmentStatus.COMPLETED)
                .build());

        ReviewAssignment assign2b = reviewAssignmentRepository.save(ReviewAssignment.builder()
                .proposal(proposal2)
                .faculty(faculty3)
                .evaluatorNumber(2)
                .assignedBy(adminUser)
                .deadline(LocalDate.now().plusDays(7))
                .status(ReviewAssignment.AssignmentStatus.PENDING)
                .build());

        // === REVIEW FOR PROPOSAL 2 (only evaluator 1 submitted) ===
        Review review2a = Review.builder()
                .assignment(assign2a)
                .proposal(proposal2)
                .faculty(faculty1)
                .technicalComments("Well-conceived IoT project. LoRa choice is appropriate for rural Indian conditions. The multi-parameter sensor approach is comprehensive.")
                .strengths("Low cost, high impact on public health, scalable architecture, aligns with government schemes.")
                .weaknesses("Long-term calibration of sensors needs more analysis. Data security aspect needs strengthening.")
                .recommendation(Review.Recommendation.STRONGLY_RECOMMEND)
                .additionalRemarks("This project addresses a critical public health need. Highly recommend for funding.")
                .isLocked(true)
                .build();

        ReviewScore score2a = ReviewScore.builder()
                .noveltyScore(new BigDecimal("8.5"))
                .feasibilityScore(new BigDecimal("9.0"))
                .commercialImpactScore(new BigDecimal("8.0"))
                .weightedTotal(BigDecimal.ZERO)
                .build();
        score2a.calculateWeightedTotal();

        review2a = reviewRepository.save(review2a);
        score2a.setReview(review2a);
        reviewScoreRepository.save(score2a);
        review2a.setWeightedScore(score2a.getWeightedTotal());
        review2a.setReviewScore(score2a);
        reviewRepository.save(review2a);

        // === STATUS HISTORY ===
        recordStatus(proposal1, null, "SUBMITTED", adminUser, "Initial submission", statusHistoryRepository);
        recordStatus(proposal1, "SUBMITTED", "EVALUATORS_ASSIGNED", adminUser, "Two evaluators assigned", statusHistoryRepository);
        recordStatus(proposal1, "EVALUATORS_ASSIGNED", "UNDER_REVIEW", adminUser, "Review process started", statusHistoryRepository);
        recordStatus(proposal1, "UNDER_REVIEW", "REVIEW_2_COMPLETED", adminUser, "Both reviews completed", statusHistoryRepository);
        recordStatus(proposal1, "REVIEW_2_COMPLETED", "APPROVED", adminUser, "Grant approved by R&D Committee", statusHistoryRepository);
        recordStatus(proposal1, "APPROVED", "GRANT_SANCTIONED", adminUser, "Grant sanctioned", statusHistoryRepository);
        recordStatus(proposal1, "GRANT_SANCTIONED", "MILESTONE_TRACKING", adminUser, "Milestone tracking started", statusHistoryRepository);

        recordStatus(proposal2, null, "SUBMITTED", adminUser, "Initial submission", statusHistoryRepository);
        recordStatus(proposal2, "SUBMITTED", "EVALUATORS_ASSIGNED", adminUser, "Two evaluators assigned", statusHistoryRepository);
        recordStatus(proposal2, "EVALUATORS_ASSIGNED", "UNDER_REVIEW", adminUser, "Review process started", statusHistoryRepository);
        recordStatus(proposal2, "UNDER_REVIEW", "REVIEW_1_COMPLETED", adminUser, "Evaluator 1 submitted review", statusHistoryRepository);

        recordStatus(proposal3, null, "SUBMITTED", adminUser, "Initial submission", statusHistoryRepository);
        recordStatus(proposal3, "SUBMITTED", "PENDING_EVALUATOR_ASSIGNMENT", adminUser, "Awaiting evaluator assignment", statusHistoryRepository);

        // === NOTIFICATIONS ===
        saveNotification(studentUser1, proposal1, "Proposal Submitted", "Your proposal 'AI-Powered Smart Waste Segregation System' has been successfully submitted.", "PROPOSAL_SUBMITTED", notificationRepository);
        saveNotification(studentUser1, proposal1, "Evaluators Assigned", "Two faculty evaluators have been assigned to review your proposal.", "EVALUATORS_ASSIGNED", notificationRepository);
        saveNotification(studentUser1, proposal1, "Proposal Approved!", "Congratulations! Your proposal has been approved by the R&D Committee.", "PROPOSAL_APPROVED", notificationRepository);
        saveNotification(studentUser1, proposal1, "Grant Sanctioned", "Your grant of Rs. 55,000 has been sanctioned. Reference: GX/2024/AI-WASTE/001", "GRANT_SANCTIONED", notificationRepository);
        saveNotification(facultyUser1, proposal1, "New Proposal Assigned", "You have been assigned to evaluate 'AI-Powered Smart Waste Segregation System'.", "ASSIGNMENT", notificationRepository);
        saveNotification(studentUser2, proposal2, "Proposal Submitted", "Your proposal 'Low-Cost IoT Smart Water Quality Monitoring Network' has been submitted.", "PROPOSAL_SUBMITTED", notificationRepository);
        saveNotification(studentUser3, proposal3, "Proposal Received", "Your proposal 'Autonomous Agricultural Robot' has been received and is pending evaluator assignment.", "PROPOSAL_SUBMITTED", notificationRepository);

        log.info("Sample data initialization complete. Users created: admin/admin123, dr.priya/faculty123, dr.rajan/faculty123, dr.meena/faculty123, arjun.cs21/student123, divya.ec22/student123, rahul.mech21/student123");
    }

    private void recordStatus(Proposal proposal, String prev, String next, User changedBy, String remarks, StatusHistoryRepository repo) {
        repo.save(StatusHistory.builder()
                .proposal(proposal)
                .previousStatus(prev)
                .newStatus(next)
                .changedBy(changedBy)
                .remarks(remarks)
                .build());
    }

    private void saveNotification(User user, Proposal proposal, String title, String message, String type, NotificationRepository repo) {
        repo.save(Notification.builder()
                .user(user)
                .proposal(proposal)
                .title(title)
                .message(message)
                .notificationType(type)
                .isRead(false)
                .build());
    }
}
