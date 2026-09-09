-- Grant-X Database Schema
-- Student Innovation Grant & Patent Idea Filing Portal
-- AIHT CSBS-G11

-- Create database
CREATE DATABASE IF NOT EXISTS grantx CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE grantx;

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('STUDENT','FACULTY','ADMIN') NOT NULL,
    phone VARCHAR(15),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role),
    INDEX idx_users_email (email)
) ENGINE=InnoDB;

-- ============================================================
-- STUDENTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    register_number VARCHAR(20) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    year_of_study INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_students_register (register_number)
) ENGINE=InnoDB;

-- ============================================================
-- FACULTY TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS faculty (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    specialization VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_faculty_employee (employee_id)
) ENGINE=InnoDB;

-- ============================================================
-- PROPOSALS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS proposals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_number VARCHAR(30) NOT NULL UNIQUE,
    student_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    problem_statement TEXT NOT NULL,
    proposed_solution TEXT NOT NULL,
    abstract TEXT NOT NULL,
    objectives TEXT NOT NULL,
    technology_domain VARCHAR(200) NOT NULL,
    innovation_category VARCHAR(100) NOT NULL,
    existing_solutions TEXT,
    novelty_explanation TEXT NOT NULL,
    difference_from_existing TEXT NOT NULL,
    innovation_significance TEXT NOT NULL,
    patent_potential ENUM('HIGH','MEDIUM','LOW','NONE') NOT NULL DEFAULT 'MEDIUM',
    novelty_declaration BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM(
        'DRAFT','SUBMITTED','PENDING_EVALUATOR_ASSIGNMENT',
        'EVALUATORS_ASSIGNED','UNDER_REVIEW','REVIEW_1_COMPLETED',
        'REVIEW_2_COMPLETED','SCORES_AGGREGATED','PENDING_GRANT_DECISION',
        'PENDING_REVISION','RESUBMITTED','APPROVED','REJECTED',
        'GRANT_SANCTIONED','MILESTONE_TRACKING','COMPLETED'
    ) NOT NULL DEFAULT 'DRAFT',
    aggregated_score DECIMAL(5,2),
    submission_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_proposals_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
    INDEX idx_proposals_status (status),
    INDEX idx_proposals_student (student_id),
    INDEX idx_proposals_number (proposal_number)
) ENGINE=InnoDB;

-- ============================================================
-- PROPOSAL TEAM MEMBERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS proposal_team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    register_number VARCHAR(20) NOT NULL,
    department VARCHAR(100),
    year_of_study INT,
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_team_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    INDEX idx_team_proposal (proposal_id)
) ENGINE=InnoDB;

-- ============================================================
-- PROPOSAL BUDGETS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS proposal_budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL UNIQUE,
    equipment_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    software_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    materials_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    prototype_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    testing_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    other_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_requested DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    budget_justification TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- REVIEW ASSIGNMENTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS review_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    evaluator_number INT NOT NULL COMMENT '1 or 2',
    assigned_by BIGINT NOT NULL,
    deadline DATE,
    status ENUM('PENDING','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'PENDING',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignment_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id) ON DELETE RESTRICT,
    CONSTRAINT fk_assignment_admin FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE RESTRICT,
    UNIQUE KEY uq_proposal_evaluator (proposal_id, evaluator_number),
    UNIQUE KEY uq_proposal_faculty (proposal_id, faculty_id),
    INDEX idx_assignment_faculty (faculty_id),
    INDEX idx_assignment_proposal (proposal_id)
) ENGINE=InnoDB;

-- ============================================================
-- REVIEWS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL UNIQUE,
    proposal_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    technical_comments TEXT NOT NULL,
    strengths TEXT NOT NULL,
    weaknesses TEXT NOT NULL,
    recommendation ENUM('STRONGLY_RECOMMEND','RECOMMEND','NEUTRAL','NOT_RECOMMEND','STRONGLY_NOT_RECOMMEND') NOT NULL,
    additional_remarks TEXT,
    weighted_score DECIMAL(5,2),
    is_locked BOOLEAN NOT NULL DEFAULT TRUE,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_assignment FOREIGN KEY (assignment_id) REFERENCES review_assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id) ON DELETE RESTRICT,
    INDEX idx_reviews_proposal (proposal_id),
    INDEX idx_reviews_faculty (faculty_id)
) ENGINE=InnoDB;

-- ============================================================
-- REVIEW SCORES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS review_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL UNIQUE,
    novelty_score DECIMAL(4,1) NOT NULL COMMENT '0-10',
    feasibility_score DECIMAL(4,1) NOT NULL COMMENT '0-10',
    commercial_impact_score DECIMAL(4,1) NOT NULL COMMENT '0-10',
    weighted_total DECIMAL(5,2) NOT NULL COMMENT 'Novelty*0.4 + Feasibility*0.3 + Commercial*0.3',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_score_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
    CONSTRAINT chk_novelty CHECK (novelty_score BETWEEN 0 AND 10),
    CONSTRAINT chk_feasibility CHECK (feasibility_score BETWEEN 0 AND 10),
    CONSTRAINT chk_commercial CHECK (commercial_impact_score BETWEEN 0 AND 10)
) ENGINE=InnoDB;

-- ============================================================
-- GRANT DECISIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS grant_decisions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    decided_by BIGINT NOT NULL,
    decision ENUM('APPROVED','REJECTED','REVISION_REQUESTED') NOT NULL,
    decision_remarks TEXT,
    revision_comments TEXT,
    aggregated_score_at_decision DECIMAL(5,2),
    decision_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_decision_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_decision_admin FOREIGN KEY (decided_by) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_decision_proposal (proposal_id)
) ENGINE=InnoDB;

-- ============================================================
-- GRANT FUNDING TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS grant_funding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    decision_id BIGINT NOT NULL,
    requested_amount DECIMAL(12,2) NOT NULL,
    approved_amount DECIMAL(12,2),
    grant_status ENUM('PENDING','APPROVED','SANCTIONED','PARTIALLY_APPROVED','REJECTED','COMPLETED') NOT NULL DEFAULT 'PENDING',
    sanction_date DATE,
    grant_reference VARCHAR(50),
    remarks TEXT,
    project_phase VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_funding_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_funding_decision FOREIGN KEY (decision_id) REFERENCES grant_decisions(id) ON DELETE CASCADE,
    INDEX idx_funding_proposal (proposal_id)
) ENGINE=InnoDB;

-- ============================================================
-- MILESTONES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS milestones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    due_date DATE NOT NULL,
    status ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED','DELAYED') NOT NULL DEFAULT 'NOT_STARTED',
    completion_percentage INT NOT NULL DEFAULT 0,
    remarks TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_milestone_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_milestone_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_completion CHECK (completion_percentage BETWEEN 0 AND 100),
    INDEX idx_milestone_proposal (proposal_id)
) ENGINE=InnoDB;

-- ============================================================
-- STATUS HISTORY TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by BIGINT,
    remarks TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_history_proposal (proposal_id),
    INDEX idx_history_changed_at (changed_at)
) ENGINE=InnoDB;

-- ============================================================
-- NOTIFICATIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    proposal_id BIGINT,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE SET NULL,
    INDEX idx_notification_user (user_id),
    INDEX idx_notification_read (is_read),
    INDEX idx_notification_created (created_at)
) ENGINE=InnoDB;
