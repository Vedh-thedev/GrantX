/* ============================================================
   Grant-X — Complete Frontend JavaScript
   AIHT CSBS-G11 | REST API Integration
   ============================================================ */

// Target backend at http://localhost:8080/api when served locally, via file://, or on GitHub Pages
const API_BASE = (
  window.location.protocol === 'file:' ||
  window.location.hostname === 'localhost' ||
  window.location.hostname === '127.0.0.1' ||
  window.location.hostname.endsWith('github.io')
) ? 'http://localhost:8080/api' : '/api';

// ============================================================
// STATE
// ============================================================
let state = {
  token: null,
  user: null,
  currentRole: null,
  allProposals: [],
  allFaculty: [],
  allReviews: [],
  selectedAssignment: null,
  currentWizardStep: 1,
  totalWizardSteps: 5,
  teamMemberCount: 0,
};

const WIZARD_STEPS = [
  { label: 'Team Details', icon: '👥' },
  { label: 'Proposal Info', icon: '💡' },
  { label: 'Novelty & Patent', icon: '🔬' },
  { label: 'Budget', icon: '💰' },
  { label: 'Review & Submit', icon: '✅' },
];

// ============================================================
// PAGE ROUTING
// ============================================================
function showPage(pageId) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  const page = document.getElementById(pageId);
  if (page) {
    page.classList.add('active');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

// ============================================================
// AUTH
// ============================================================
async function handleLogin(event) {
  event.preventDefault();
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;

  if (!username || !password) {
    showToast('error', 'Login Failed', 'Please enter username and password.');
    return;
  }

  setLoading(true, 'Signing in...');
  try {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'Invalid credentials');
    }

    state.token = data.data.token;
    state.user = data.data;
    state.currentRole = data.data.role;

    localStorage.setItem('gx_token', state.token);
    localStorage.setItem('gx_user', JSON.stringify(state.user));

    showToast('success', 'Welcome Back!', `Logged in as ${data.data.fullName}`);
    routeToRole(state.currentRole);
  } catch (err) {
    showToast('error', 'Login Failed', err.message);
  } finally {
    setLoading(false);
  }
}

function routeToRole(role) {
  if (role === 'STUDENT') {
    initStudentApp();
    showPage('page-student');
  } else if (role === 'FACULTY') {
    initFacultyApp();
    showPage('page-faculty');
  } else if (role === 'ADMIN') {
    initAdminApp();
    showPage('page-admin');
  }
}

function logout() {
  state.token = null;
  state.user = null;
  state.currentRole = null;
  localStorage.removeItem('gx_token');
  localStorage.removeItem('gx_user');
  showToast('info', 'Logged Out', 'You have been signed out.');
  showPage('page-landing');
}

// Auto-restore session
function restoreSession() {
  const token = localStorage.getItem('gx_token');
  const user = localStorage.getItem('gx_user');
  if (token && user) {
    try {
      state.token = token;
      state.user = JSON.parse(user);
      state.currentRole = state.user.role;
      routeToRole(state.currentRole);
      return true;
    } catch (e) {
      localStorage.removeItem('gx_token');
      localStorage.removeItem('gx_user');
    }
  }
  return false;
}

// ============================================================
// API HELPER
// ============================================================
async function api(method, endpoint, body = null) {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(state.token && { Authorization: `Bearer ${state.token}` }),
    },
  };
  if (body) options.body = JSON.stringify(body);

  const res = await fetch(`${API_BASE}${endpoint}`, options);

  if (res.status === 401) {
    showToast('warning', 'Session Expired', 'Please log in again.');
    logout();
    throw new Error('Unauthorized');
  }

  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'An error occurred');
  return data.data !== undefined ? data.data : data;
}

// ============================================================
// STUDENT APP
// ============================================================
function initStudentApp() {
  const name = state.user.fullName;
  const initials = name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  document.getElementById('s-avatar').textContent = initials;
  document.getElementById('s-user-name').textContent = name;
  document.getElementById('s-welcome-name').textContent = name.split(' ')[0];

  initWizard();
  showStudentSection('dashboard');
}

function showStudentSection(section) {
  const sections = ['dashboard', 'submit', 'proposals', 'proposal-detail', 'notifications', 'profile'];
  sections.forEach(s => {
    const el = document.getElementById(`s-section-${s}`);
    if (el) el.classList.toggle('hidden', s !== section);
    const nav = document.getElementById(`nav-s-${s}`);
    if (nav) nav.classList.toggle('active', s === section);
  });

  const titles = {
    dashboard: 'Dashboard',
    submit: 'Submit Proposal',
    proposals: 'My Proposals',
    'proposal-detail': 'Proposal Detail',
    notifications: 'Notifications',
    profile: 'Profile',
  };
  document.getElementById('student-page-title').textContent = titles[section] || 'Student Portal';

  if (section === 'dashboard') loadStudentDashboard();
  if (section === 'proposals') loadStudentProposals();
  if (section === 'notifications') loadNotifications('s');
  if (section === 'profile') loadStudentProfile();
}

async function loadStudentDashboard() {
  try {
    const [stats, proposals] = await Promise.all([
      api('GET', '/student/dashboard'),
      api('GET', '/student/proposals'),
    ]);

    document.getElementById('s-stat-total').textContent = stats.totalProposals || 0;
    document.getElementById('s-stat-draft').textContent = stats.draftProposals || 0;
    document.getElementById('s-stat-review').textContent = stats.underReview || 0;
    document.getElementById('s-stat-approved').textContent = stats.approved || 0;
    document.getElementById('s-stat-rejected').textContent = stats.rejected || 0;
    document.getElementById('s-stat-funding').textContent = formatCurrency(stats.totalFundingRequested || 0);

    renderStudentProposalList(document.getElementById('s-recent-proposals'), proposals.slice(0, 5), true);
    loadNotificationBadge('s');
  } catch (e) {
    showToast('error', 'Error', e.message);
  }
}

async function loadStudentProposals() {
  const container = document.getElementById('s-proposals-list');
  container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading proposals...</div>';
  try {
    const proposals = await api('GET', '/student/proposals');
    if (!proposals || proposals.length === 0) {
      container.innerHTML = `<div class="empty-state">
        <div class="empty-state-icon">📭</div>
        <div class="empty-state-title">No Proposals Yet</div>
        <div class="empty-state-desc">Submit your first innovation proposal to get started.</div>
        <button class="btn btn-primary mt-4" onclick="showStudentSection('submit')">+ Submit Proposal</button>
      </div>`;
      return;
    }
    renderStudentProposalList(container, proposals, false);
  } catch (e) {
    container.innerHTML = `<div class="empty-state"><div class="empty-state-title">Error loading proposals</div></div>`;
    showToast('error', 'Error', e.message);
  }
}

function renderStudentProposalList(container, proposals, compact = false) {
  if (!proposals || proposals.length === 0) {
    container.innerHTML = `<div class="empty-state">
      <div class="empty-state-icon">📭</div>
      <div class="empty-state-title">No Proposals Yet</div>
      <div class="empty-state-desc">Submit your first innovation proposal.</div>
    </div>`;
    return;
  }

  container.innerHTML = proposals.map(p => `
    <div class="proposal-card" onclick="viewStudentProposal(${p.id})">
      <div class="proposal-card-header">
        <div class="proposal-card-title">${escHtml(p.title)}</div>
        ${getStatusBadge(p.status)}
      </div>
      <div class="proposal-card-meta">
        <span class="meta-item">🔖 ${escHtml(p.proposalNumber)}</span>
        <span class="meta-item">🏷️ ${escHtml(p.innovationCategory)}</span>
        <span class="meta-item">💰 ${formatCurrency(p.totalBudgetRequested || 0)}</span>
        ${p.aggregatedScore ? `<span class="meta-item">⭐ ${p.aggregatedScore}/10</span>` : ''}
        <span class="meta-item">📅 ${formatDate(p.submissionDate || p.createdAt)}</span>
      </div>
      ${!compact ? getTrackerHtml(p.status) : ''}
      <div class="proposal-card-footer">
        <span style="font-size:13px;color:var(--text-muted);">${p.reviewCount} evaluator(s) assigned</span>
        <button class="btn btn-secondary btn-sm" onclick="event.stopPropagation();viewStudentProposal(${p.id})">View Details →</button>
      </div>
    </div>
  `).join('');
}

async function viewStudentProposal(proposalId) {
  showStudentSection('proposal-detail');
  const container = document.getElementById('s-proposal-detail-content');
  container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading proposal details...</div>';

  try {
    const p = await api('GET', `/student/proposals/${proposalId}`);
    document.getElementById('s-detail-title').textContent = p.title;
    document.getElementById('s-detail-number').textContent = p.proposalNumber;

    container.innerHTML = renderProposalDetailHtml(p, 'student');
  } catch (e) {
    container.innerHTML = `<div class="empty-state"><div class="empty-state-title">Error loading proposal</div></div>`;
    showToast('error', 'Error', e.message);
  }
}

function renderProposalDetailHtml(p, role) {
  const canSubmit = p.status === 'DRAFT' && role === 'student';

  return `
    <!-- Tracker -->
    <div class="tracker-container">
      <div class="tracker-title">📍 Proposal Status Tracker</div>
      <div class="tracker-steps" id="tracker-${p.id}">
        ${getDetailedTracker(p.status)}
      </div>
    </div>

    <!-- Main Grid -->
    <div style="display:grid;grid-template-columns:2fr 1fr;gap:24px;align-items:start;">
      <!-- Left -->
      <div style="display:flex;flex-direction:column;gap:20px;">
        <!-- Basic Info -->
        <div class="card">
          <div class="card-header">
            <div class="card-title">📋 Proposal Information</div>
            <div>${getStatusBadge(p.status)}</div>
          </div>
          <div class="card-body">
            <div class="review-summary-grid">
              <div class="review-field"><div class="review-field-label">Proposal #</div><div class="review-field-value font-mono">${escHtml(p.proposalNumber)}</div></div>
              <div class="review-field"><div class="review-field-label">Category</div><div class="review-field-value">${escHtml(p.innovationCategory)}</div></div>
              <div class="review-field"><div class="review-field-label">Technology Domain</div><div class="review-field-value">${escHtml(p.technologyDomain)}</div></div>
              <div class="review-field"><div class="review-field-label">Patent Potential</div><div class="review-field-value">${escHtml(p.patentPotential)}</div></div>
              <div class="review-field"><div class="review-field-label">Submitted</div><div class="review-field-value">${formatDate(p.submissionDate)}</div></div>
              ${p.aggregatedScore ? `<div class="review-field"><div class="review-field-label">Aggregated Score</div><div class="review-field-value fw-bold text-primary-accent">${p.aggregatedScore}/10</div></div>` : ''}
            </div>
          </div>
        </div>

        <!-- Proposal Content -->
        <div class="card">
          <div class="card-header"><div class="card-title">💡 Project Details</div></div>
          <div class="card-body" style="display:flex;flex-direction:column;gap:20px;">
            ${renderDetailField('Problem Statement', p.problemStatement)}
            ${renderDetailField('Proposed Solution', p.proposedSolution)}
            ${renderDetailField('Abstract', p.abstrakt)}
            ${renderDetailField('Objectives', p.objectives)}
            ${renderDetailField('Novelty Explanation', p.noveltyExplanation)}
            ${renderDetailField('Difference from Existing', p.differenceFromExisting)}
            ${renderDetailField('Innovation Significance', p.innovationSignificance)}
          </div>
        </div>

        <!-- Team Members -->
        ${p.teamMembers && p.teamMembers.length > 0 ? `
        <div class="card">
          <div class="card-header"><div class="card-title">👥 Team Members (${p.teamMembers.length})</div></div>
          <div class="card-body">
            <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:16px;">
              ${p.teamMembers.map(m => `
                <div style="background:var(--bg-secondary);border:1px solid var(--border);border-radius:var(--radius);padding:16px;">
                  <div style="font-weight:700;font-size:15px;">${escHtml(m.memberName)}</div>
                  <div style="font-size:12px;color:var(--text-muted);margin-top:4px;">${escHtml(m.registerNumber)}</div>
                  <div style="font-size:12px;color:var(--text-muted);">${escHtml(m.department || '')}</div>
                </div>
              `).join('')}
            </div>
          </div>
        </div>
        ` : ''}

        <!-- Milestones -->
        ${p.milestones && p.milestones.length > 0 ? `
        <div class="card">
          <div class="card-header"><div class="card-title">🎯 Project Milestones</div></div>
          <div class="card-body">
            ${p.milestones.map(m => `
              <div class="milestone-card">
                <div class="milestone-header">
                  <div class="milestone-title">${escHtml(m.title)}</div>
                  <div class="flex gap-2">
                    ${getStatusBadge(m.status)}
                    <span class="milestone-due">📅 Due: ${formatDate(m.dueDate)}</span>
                  </div>
                </div>
                ${m.description ? `<div style="font-size:13px;color:var(--text-secondary);margin-bottom:12px;">${escHtml(m.description)}</div>` : ''}
                <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:6px;">
                  <span style="font-size:12px;color:var(--text-muted);">Completion</span>
                  <span style="font-size:13px;font-weight:700;color:var(--primary-light);">${m.completionPercentage}%</span>
                </div>
                <div class="progress-bar-container">
                  <div class="progress-bar ${m.completionPercentage === 100 ? 'green' : ''}" style="width:${m.completionPercentage}%"></div>
                </div>
                ${m.remarks ? `<div style="font-size:12px;color:var(--text-muted);margin-top:8px;">📝 ${escHtml(m.remarks)}</div>` : ''}
              </div>
            `).join('')}
          </div>
        </div>
        ` : ''}
      </div>

      <!-- Right Sidebar -->
      <div style="display:flex;flex-direction:column;gap:20px;">
        <!-- Budget -->
        ${p.budget ? `
        <div class="card">
          <div class="card-header"><div class="card-title">💰 Budget</div></div>
          <div class="card-body">
            <div style="display:flex;flex-direction:column;gap:8px;">
              ${renderBudgetRow('Equipment', p.budget.equipmentCost)}
              ${renderBudgetRow('Software', p.budget.softwareCost)}
              ${renderBudgetRow('Materials', p.budget.materialsCost)}
              ${renderBudgetRow('Prototype', p.budget.prototypeCost)}
              ${renderBudgetRow('Testing', p.budget.testingCost)}
              ${renderBudgetRow('Other', p.budget.otherCost)}
            </div>
            <div style="border-top:1px solid var(--border);margin-top:12px;padding-top:12px;display:flex;justify-content:space-between;align-items:center;">
              <span style="font-weight:700;">Total Requested</span>
              <span style="font-family:var(--font-display);font-size:1.2rem;font-weight:800;color:var(--primary-light);">${formatCurrency(p.budget.totalRequested)}</span>
            </div>
          </div>
        </div>
        ` : ''}

        <!-- Funding Info -->
        ${p.funding ? `
        <div class="card">
          <div class="card-header"><div class="card-title">🏦 Grant Funding</div></div>
          <div class="card-body">
            <div style="display:flex;flex-direction:column;gap:10px;">
              <div class="review-field"><div class="review-field-label">Status</div><div>${getStatusBadge(p.funding.grantStatus)}</div></div>
              <div class="review-field"><div class="review-field-label">Requested</div><div class="review-field-value">${formatCurrency(p.funding.requestedAmount)}</div></div>
              ${p.funding.approvedAmount ? `<div class="review-field"><div class="review-field-label">Approved</div><div class="review-field-value text-success fw-bold">${formatCurrency(p.funding.approvedAmount)}</div></div>` : ''}
              ${p.funding.grantReference ? `<div class="review-field"><div class="review-field-label">Reference</div><div class="review-field-value font-mono">${escHtml(p.funding.grantReference)}</div></div>` : ''}
              ${p.funding.sanctionDate ? `<div class="review-field"><div class="review-field-label">Sanction Date</div><div class="review-field-value">${formatDate(p.funding.sanctionDate)}</div></div>` : ''}
              ${p.funding.projectPhase ? `<div class="review-field"><div class="review-field-label">Project Phase</div><div class="review-field-value">${escHtml(p.funding.projectPhase)}</div></div>` : ''}
            </div>
          </div>
        </div>
        ` : ''}

        <!-- Status History -->
        ${p.statusHistory && p.statusHistory.length > 0 ? `
        <div class="card">
          <div class="card-header"><div class="card-title">📜 Status History</div></div>
          <div class="card-body">
            <div style="display:flex;flex-direction:column;gap:12px;">
              ${p.statusHistory.slice().reverse().map(h => `
                <div style="border-left:2px solid var(--primary);padding-left:12px;">
                  <div style="font-size:13px;font-weight:600;color:var(--text-primary);">→ ${formatStatus(h.newStatus)}</div>
                  ${h.remarks ? `<div style="font-size:12px;color:var(--text-muted);">${escHtml(h.remarks)}</div>` : ''}
                  <div style="font-size:11px;color:var(--text-muted);">${formatDate(h.changedAt)} by ${escHtml(h.changedBy || 'System')}</div>
                </div>
              `).join('')}
            </div>
          </div>
        </div>
        ` : ''}

        ${canSubmit ? `
        <button class="btn btn-primary btn-full btn-lg" onclick="submitDraftProposal(${p.id})">🚀 Submit Proposal</button>
        ` : ''}
      </div>
    </div>
  `;
}

async function submitDraftProposal(proposalId) {
  setLoading(true, 'Submitting proposal...');
  try {
    await api('POST', `/student/proposals/${proposalId}/submit`);
    showToast('success', 'Proposal Submitted!', 'Your proposal has been submitted for review.');
    viewStudentProposal(proposalId);
  } catch (e) {
    showToast('error', 'Error', e.message);
  } finally {
    setLoading(false);
  }
}

function loadStudentProfile() {
  const container = document.getElementById('s-profile-content');
  const u = state.user;
  container.innerHTML = `
    <div style="display:flex;align-items:center;gap:20px;margin-bottom:24px;">
      <div style="width:64px;height:64px;border-radius:50%;background:var(--gradient-primary);display:flex;align-items:center;justify-content:center;font-size:24px;font-weight:700;color:white;">
        ${u.fullName.split(' ').map(n => n[0]).join('').slice(0,2)}
      </div>
      <div>
        <div style="font-size:1.3rem;font-weight:800;">${escHtml(u.fullName)}</div>
        <div style="color:var(--text-muted);font-size:13px;">${escHtml(u.email)}</div>
        <div style="margin-top:6px;">${getStatusBadge('STUDENT')}</div>
      </div>
    </div>
    <div class="divider"></div>
    <div class="review-summary-grid">
      <div class="review-field"><div class="review-field-label">Username</div><div class="review-field-value">${escHtml(u.username)}</div></div>
      <div class="review-field"><div class="review-field-label">Full Name</div><div class="review-field-value">${escHtml(u.fullName)}</div></div>
      <div class="review-field"><div class="review-field-label">Email</div><div class="review-field-value">${escHtml(u.email)}</div></div>
      <div class="review-field"><div class="review-field-label">Role</div><div class="review-field-value">${escHtml(u.role)}</div></div>
    </div>
  `;
}

// ============================================================
// WIZARD
// ============================================================
function initWizard() {
  state.currentWizardStep = 1;
  renderWizardIndicator();
  updateWizardUI();
}

function renderWizardIndicator() {
  const indicator = document.getElementById('wizard-indicator');
  if (!indicator) return;
  let html = '';
  WIZARD_STEPS.forEach((step, i) => {
    const stepNum = i + 1;
    const isCompleted = stepNum < state.currentWizardStep;
    const isActive = stepNum === state.currentWizardStep;
    html += `
      <div style="display:flex;flex-direction:column;align-items:center;flex:1;">
        <div style="display:flex;align-items:center;width:100%;">
          <div style="
            width:38px;height:38px;border-radius:50%;
            background:${isCompleted ? 'var(--success)' : isActive ? 'var(--gradient-primary)' : 'var(--bg-secondary)'};
            border:2px solid ${isCompleted ? 'var(--success)' : isActive ? 'var(--primary)' : 'var(--border)'};
            display:flex;align-items:center;justify-content:center;
            font-size:16px;font-weight:700;color:${isCompleted || isActive ? 'white' : 'var(--text-muted)'};
            flex-shrink:0;
            box-shadow:${isActive ? 'var(--shadow-glow-sm)' : 'none'};
            transition:var(--transition);
          ">${isCompleted ? '✓' : step.icon}</div>
          ${i < WIZARD_STEPS.length - 1 ? `<div style="flex:1;height:2px;background:${isCompleted ? 'var(--success)' : 'var(--border)'};transition:var(--transition);"></div>` : ''}
        </div>
        <div style="font-size:11px;font-weight:${isActive ? '700' : '500'};color:${isCompleted ? 'var(--success)' : isActive ? 'var(--primary-light)' : 'var(--text-muted)'};margin-top:6px;text-align:center;">${step.label}</div>
      </div>
    `;
  });
  indicator.innerHTML = html;
}

function updateWizardUI() {
  for (let i = 1; i <= state.totalWizardSteps; i++) {
    const el = document.getElementById(`wizard-step-${i}`);
    if (el) el.classList.toggle('active', i === state.currentWizardStep);
  }

  const prevBtn = document.getElementById('wizard-prev-btn');
  const nextBtn = document.getElementById('wizard-next-btn');
  const draftBtn = document.getElementById('wizard-draft-btn');
  const infoEl = document.getElementById('wizard-step-info');

  if (prevBtn) prevBtn.style.visibility = state.currentWizardStep === 1 ? 'hidden' : 'visible';
  if (infoEl) infoEl.textContent = `Step ${state.currentWizardStep} of ${state.totalWizardSteps}`;

  if (state.currentWizardStep === state.totalWizardSteps) {
    if (nextBtn) {
      nextBtn.textContent = '🚀 Submit Proposal';
      nextBtn.onclick = () => submitWizard(false);
    }
    // Build review content
    buildWizardReview();
  } else {
    if (nextBtn) {
      nextBtn.textContent = 'Next →';
      nextBtn.onclick = wizardNext;
    }
  }

  renderWizardIndicator();
}

function wizardNext() {
  if (!validateWizardStep(state.currentWizardStep)) return;
  if (state.currentWizardStep < state.totalWizardSteps) {
    state.currentWizardStep++;
    updateWizardUI();
    document.querySelector('.wizard-body')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

function wizardPrev() {
  if (state.currentWizardStep > 1) {
    state.currentWizardStep--;
    updateWizardUI();
  }
}

function validateWizardStep(step) {
  if (step === 1) {
    // Basic validation — student name/reg from profile, rest optional in UI
    return true;
  }
  if (step === 2) {
    const title = document.getElementById('w-title')?.value.trim();
    const category = document.getElementById('w-category')?.value;
    const problem = document.getElementById('w-problem')?.value.trim();
    if (!title || !category || !problem) {
      showToast('warning', 'Missing Fields', 'Please fill in Title, Category, and Problem Statement.');
      return false;
    }
  }
  if (step === 3) {
    const novelty = document.getElementById('w-novelty')?.value.trim();
    const diff = document.getElementById('w-difference')?.value.trim();
    if (!novelty || !diff) {
      showToast('warning', 'Missing Fields', 'Please fill in Novelty Explanation and Difference from Existing.');
      return false;
    }
  }
  return true;
}

function updateBudgetTotal() {
  const fields = ['equipment', 'software', 'materials', 'prototype', 'testing', 'other'];
  const total = fields.reduce((sum, f) => {
    const val = parseFloat(document.getElementById(`b-${f}`)?.value || 0);
    return sum + (isNaN(val) ? 0 : val);
  }, 0);
  document.getElementById('budget-total-display').textContent = `₹${total.toLocaleString('en-IN')}`;
}

function addTeamMember() {
  state.teamMemberCount++;
  const idx = state.teamMemberCount;
  const list = document.getElementById('team-members-list');
  const div = document.createElement('div');
  div.className = 'team-member-card';
  div.id = `team-member-${idx}`;
  div.innerHTML = `
    <button class="btn-remove" onclick="removeTeamMember(${idx})">✕</button>
    <div style="font-size:13px;font-weight:600;color:var(--text-muted);margin-bottom:12px;">Team Member ${idx}</div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Full Name <span class="required">*</span></label>
        <input type="text" class="form-control tm-name" placeholder="Member's full name">
      </div>
      <div class="form-group">
        <label class="form-label">Register Number</label>
        <input type="text" class="form-control tm-regnum" placeholder="Register number">
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Department</label>
        <input type="text" class="form-control tm-dept" placeholder="Department">
      </div>
      <div class="form-group">
        <label class="form-label">Email</label>
        <input type="email" class="form-control tm-email" placeholder="Email">
      </div>
    </div>
  `;
  list.appendChild(div);
}

function removeTeamMember(idx) {
  document.getElementById(`team-member-${idx}`)?.remove();
}

function getTeamMembers() {
  return Array.from(document.querySelectorAll('.team-member-card')).map(card => ({
    memberName: card.querySelector('.tm-name')?.value.trim() || '',
    registerNumber: card.querySelector('.tm-regnum')?.value.trim() || '',
    department: card.querySelector('.tm-dept')?.value.trim() || '',
    email: card.querySelector('.tm-email')?.value.trim() || '',
  })).filter(m => m.memberName);
}

function buildWizardReview() {
  const container = document.getElementById('wizard-review-content');
  if (!container) return;
  const title = document.getElementById('w-title')?.value;
  const category = document.getElementById('w-category')?.value;
  const domain = document.getElementById('w-tech-domain')?.value;
  const patent = document.getElementById('w-patent')?.value;
  const budget = ['equipment', 'software', 'materials', 'prototype', 'testing', 'other']
    .reduce((s, f) => s + (parseFloat(document.getElementById(`b-${f}`)?.value || 0)), 0);
  const members = getTeamMembers();

  container.innerHTML = `
    <div class="review-summary-section">
      <div class="review-summary-title">📋 Project Overview</div>
      <div class="review-summary-grid">
        <div class="review-field"><div class="review-field-label">Title</div><div class="review-field-value">${escHtml(title || '—')}</div></div>
        <div class="review-field"><div class="review-field-label">Category</div><div class="review-field-value">${escHtml(category || '—')}</div></div>
        <div class="review-field"><div class="review-field-label">Tech Domain</div><div class="review-field-value">${escHtml(domain || '—')}</div></div>
        <div class="review-field"><div class="review-field-label">Patent Potential</div><div class="review-field-value">${escHtml(patent || '—')}</div></div>
        <div class="review-field"><div class="review-field-label">Team Members</div><div class="review-field-value">${members.length}</div></div>
        <div class="review-field"><div class="review-field-label">Total Budget</div><div class="review-field-value fw-bold text-primary-accent">${formatCurrency(budget)}</div></div>
      </div>
    </div>
  `;
}

async function saveWizardDraft() {
  await submitWizard(true);
}

async function submitWizard(isDraft) {
  const title = document.getElementById('w-title')?.value.trim();
  if (!title) {
    showToast('warning', 'Missing Title', 'Please enter a project title before submitting.');
    state.currentWizardStep = 2;
    updateWizardUI();
    return;
  }

  const payload = {
    title,
    problemStatement: document.getElementById('w-problem')?.value.trim() || '',
    proposedSolution: document.getElementById('w-solution')?.value.trim() || '',
    abstrakt: document.getElementById('w-abstract')?.value.trim() || '',
    objectives: document.getElementById('w-objectives')?.value.trim() || '',
    technologyDomain: document.getElementById('w-tech-domain')?.value.trim() || '',
    innovationCategory: document.getElementById('w-category')?.value || '',
    existingSolutions: document.getElementById('w-existing')?.value.trim() || '',
    noveltyExplanation: document.getElementById('w-novelty')?.value.trim() || '',
    differenceFromExisting: document.getElementById('w-difference')?.value.trim() || '',
    innovationSignificance: document.getElementById('w-significance')?.value.trim() || '',
    patentPotential: document.getElementById('w-patent')?.value || 'MEDIUM',
    noveltyDeclaration: document.getElementById('w-declaration')?.checked || false,
    teamMembers: getTeamMembers(),
    budget: {
      equipmentCost: parseFloat(document.getElementById('b-equipment')?.value || 0),
      softwareCost: parseFloat(document.getElementById('b-software')?.value || 0),
      materialsCost: parseFloat(document.getElementById('b-materials')?.value || 0),
      prototypeCost: parseFloat(document.getElementById('b-prototype')?.value || 0),
      testingCost: parseFloat(document.getElementById('b-testing')?.value || 0),
      otherCost: parseFloat(document.getElementById('b-other')?.value || 0),
      budgetJustification: document.getElementById('b-justification')?.value.trim() || '',
    },
    saveDraft: isDraft,
  };

  setLoading(true, isDraft ? 'Saving draft...' : 'Submitting proposal...');
  try {
    const result = await api('POST', '/student/proposals', payload);
    showToast('success', isDraft ? 'Draft Saved!' : 'Proposal Submitted!',
      isDraft ? `Draft saved as ${result.proposalNumber}` : `Proposal ${result.proposalNumber} submitted successfully.`);
    resetWizard();
    showStudentSection('proposals');
  } catch (e) {
    showToast('error', 'Submission Failed', e.message);
  } finally {
    setLoading(false);
  }
}

function resetWizard() {
  state.currentWizardStep = 1;
  state.teamMemberCount = 0;
  document.getElementById('team-members-list').innerHTML = '';
  ['w-title', 'w-problem', 'w-solution', 'w-abstract', 'w-objectives', 'w-tech-domain', 'w-novelty', 'w-difference', 'w-significance', 'w-existing'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  ['b-equipment', 'b-software', 'b-materials', 'b-prototype', 'b-testing', 'b-other'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '0';
  });
  updateBudgetTotal();
  updateWizardUI();
}

// ============================================================
// FACULTY APP
// ============================================================
function initFacultyApp() {
  const name = state.user.fullName;
  const initials = name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  document.getElementById('f-avatar').textContent = initials;
  document.getElementById('f-user-name').textContent = name;
  const displayName = name.replace(/^(Dr\.|Prof\.|Mr\.|Mrs\.|Ms\.)\s*/i, '').trim();
  document.getElementById('f-welcome-name').textContent = displayName ? displayName : name;
  showFacultySection('dashboard');
}

function showFacultySection(section) {
  const sections = ['dashboard', 'assignments', 'review', 'notifications', 'profile'];
  sections.forEach(s => {
    const el = document.getElementById(`f-section-${s}`);
    if (el) el.classList.toggle('hidden', s !== section);
    const nav = document.getElementById(`nav-f-${s}`);
    if (nav) nav.classList.toggle('active', s === section);
  });

  const titles = { dashboard: 'Faculty Dashboard', assignments: 'My Assignments', review: 'Proposal Evaluation', notifications: 'Notifications', profile: 'Profile' };
  document.getElementById('faculty-page-title').textContent = titles[section] || 'Faculty Portal';

  if (section === 'dashboard') loadFacultyDashboard();
  if (section === 'assignments') loadFacultyAssignments();
  if (section === 'notifications') loadNotifications('f');
  if (section === 'profile') loadFacultyProfile();
}

async function loadFacultyDashboard() {
  const container = document.getElementById('f-dashboard-assignments');
  if (container) container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading assignments...</div>';
  try {
    const assignments = await api('GET', '/faculty/assignments');
    const total = assignments.length;
    const pending = assignments.filter(a => a.status === 'PENDING' || a.status === 'IN_PROGRESS').length;
    const done = assignments.filter(a => a.status === 'COMPLETED').length;

    if (document.getElementById('f-stat-total')) document.getElementById('f-stat-total').textContent = total;
    if (document.getElementById('f-stat-pending')) document.getElementById('f-stat-pending').textContent = pending;
    if (document.getElementById('f-stat-done')) document.getElementById('f-stat-done').textContent = done;

    renderFacultyAssignmentList(container, assignments);
    loadNotificationBadge('f');
  } catch (e) {
    if (container) {
      container.innerHTML = `<div class="empty-state">
        <div class="empty-state-icon">⚠️</div>
        <div class="empty-state-title">Unable to load assignments</div>
        <div class="empty-state-desc">${escHtml(e.message)}</div>
        <button class="btn btn-outline btn-sm" style="margin-top:12px;" onclick="loadFacultyDashboard()">🔄 Retry</button>
      </div>`;
    }
    showToast('error', 'Error Loading Dashboard', e.message);
  }
}

async function loadFacultyAssignments() {
  const container = document.getElementById('f-assignments-list');
  container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading...</div>';
  try {
    const assignments = await api('GET', '/faculty/assignments');
    renderFacultyAssignmentList(container, assignments);
  } catch (e) {
    container.innerHTML = '<div class="empty-state"><div class="empty-state-title">Error loading assignments</div></div>';
  }
}

function renderFacultyAssignmentList(container, assignments) {
  if (!assignments || assignments.length === 0) {
    container.innerHTML = `<div class="empty-state">
      <div class="empty-state-icon">📭</div>
      <div class="empty-state-title">No Assignments Yet</div>
      <div class="empty-state-desc">You have not been assigned any proposals for evaluation.</div>
    </div>`;
    return;
  }

  container.innerHTML = `<div style="padding:16px;display:flex;flex-direction:column;gap:16px;">` + assignments.map(a => `
    <div style="background:var(--bg-secondary);border:1px solid var(--border);border-radius:var(--radius-lg);padding:20px;display:flex;flex-direction:column;gap:12px;">
      <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:12px;">
        <div>
          <div style="font-size:16px;font-weight:700;margin-bottom:4px;">${escHtml(a.proposalTitle)}</div>
          <div style="font-size:12px;color:var(--text-muted);">${escHtml(a.proposalNumber)} | Evaluator ${a.evaluatorNumber}</div>
        </div>
        ${getStatusBadge(a.status)}
      </div>
      <div style="display:flex;gap:16px;font-size:13px;color:var(--text-muted);flex-wrap:wrap;">
        <span>📅 Deadline: ${formatDate(a.deadline)}</span>
        ${a.reviewScore ? `<span>⭐ Your Score: ${a.reviewScore}/10</span>` : ''}
      </div>
      <div style="display:flex;gap:10px;">
        ${a.status === 'PENDING' || a.status === 'IN_PROGRESS' ? `
          <button class="btn btn-primary btn-sm" onclick="openFacultyReview(${a.id}, '${escHtml(a.proposalTitle)}')">
            ${a.hasReview ? '📋 View Review' : '✏️ Write Review'}
          </button>
        ` : `
          <button class="btn btn-secondary btn-sm" onclick="openFacultyReview(${a.id}, '${escHtml(a.proposalTitle)}')">
            👁️ View Review
          </button>
        `}
      </div>
    </div>
  `).join('') + '</div>';
}

function openFacultyReview(assignmentId, title) {
  state.selectedAssignment = assignmentId;
  document.getElementById('f-review-subtitle').textContent = title;
  showFacultySection('review');
  loadFacultyReviewForm(assignmentId);
}

async function loadFacultyReviewForm(assignmentId) {
  const container = document.getElementById('f-review-content');
  container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading review form...</div>';

  try {
    // Check if review exists
    let existingReview = null;
    try {
      existingReview = await api('GET', `/faculty/reviews/${assignmentId}`);
    } catch (e) {
      // No review yet
    }

    // Get proposal detail via assignments
    const assignments = await api('GET', '/faculty/assignments');
    const assignment = assignments.find(a => a.id === parseInt(assignmentId));

    if (!assignment) {
      container.innerHTML = '<div class="empty-state"><div class="empty-state-title">Assignment not found</div></div>';
      return;
    }

    // Get the proposal detail
    let proposal = null;
    try {
      proposal = await api('GET', `/student/proposals/${assignment.proposalId}`);
    } catch (e) {
      // Admin route fallback
      try {
        proposal = await api('GET', `/admin/proposals/${assignment.proposalId}`);
      } catch (e2) {}
    }

    if (existingReview) {
      // Show completed review
      container.innerHTML = renderCompletedReview(existingReview, proposal, assignment);
    } else {
      // Show review form
      container.innerHTML = renderReviewForm(assignment, proposal);
    }
  } catch (e) {
    container.innerHTML = `<div class="empty-state"><div class="empty-state-title">Error loading review form: ${e.message}</div></div>`;
  }
}

function renderCompletedReview(review, proposal, assignment) {
  return `
    <div style="display:flex;flex-direction:column;gap:24px;">
      <div style="background:rgba(16,185,129,0.1);border:1px solid rgba(16,185,129,0.3);border-radius:var(--radius-lg);padding:20px;">
        <div style="color:var(--success);font-weight:700;font-size:15px;margin-bottom:4px;">✅ Review Submitted & Locked</div>
        <div style="font-size:13px;color:var(--text-secondary);">Your review was submitted on ${formatDate(review.submittedAt)}. Reviews cannot be modified after submission.</div>
      </div>

      ${proposal ? `
      <div class="card">
        <div class="card-header"><div class="card-title">📋 Proposal Summary</div></div>
        <div class="card-body">
          ${renderDetailField('Title', proposal.title)}
          ${renderDetailField('Problem Statement', proposal.problemStatement)}
          ${renderDetailField('Proposed Solution', proposal.proposedSolution)}
        </div>
      </div>
      ` : ''}

      <div class="card">
        <div class="card-header"><div class="card-title">📊 Your Evaluation Scores</div></div>
        <div class="card-body">
          ${review.scores ? `
          <div class="scores-breakdown">
            <div class="score-criterion">
              <div class="score-criterion-label">Novelty</div>
              <div class="score-criterion-value">${review.scores.noveltyScore}</div>
              <div class="score-criterion-weight">Weight: 40%</div>
            </div>
            <div class="score-criterion">
              <div class="score-criterion-label">Feasibility</div>
              <div class="score-criterion-value">${review.scores.feasibilityScore}</div>
              <div class="score-criterion-weight">Weight: 30%</div>
            </div>
            <div class="score-criterion">
              <div class="score-criterion-label">Commercial Impact</div>
              <div class="score-criterion-value">${review.scores.commercialImpactScore}</div>
              <div class="score-criterion-weight">Weight: 30%</div>
            </div>
          </div>
          <div style="text-align:center;margin-top:20px;padding-top:20px;border-top:1px solid var(--border);">
            <div style="font-size:12px;color:var(--text-muted);margin-bottom:6px;">Weighted Total Score</div>
            <div style="font-family:var(--font-display);font-size:2.5rem;font-weight:800;color:var(--primary-light);">${review.weightedScore}<span style="font-size:1rem;color:var(--text-muted)">/10</span></div>
          </div>
          ` : ''}
        </div>
      </div>

      <div class="card">
        <div class="card-header"><div class="card-title">📝 Your Review Comments</div></div>
        <div class="card-body" style="display:flex;flex-direction:column;gap:16px;">
          ${renderDetailField('Technical Comments', review.technicalComments)}
          ${renderDetailField('Strengths', review.strengths)}
          ${renderDetailField('Weaknesses', review.weaknesses)}
          ${renderDetailField('Recommendation', review.recommendation?.replace('_', ' '))}
          ${review.additionalRemarks ? renderDetailField('Additional Remarks', review.additionalRemarks) : ''}
        </div>
      </div>
    </div>
  `;
}

function renderReviewForm(assignment, proposal) {
  return `
    <div style="display:flex;flex-direction:column;gap:24px;">
      ${proposal ? `
      <div class="card">
        <div class="card-header"><div class="card-title">📋 Proposal to Evaluate</div>${getStatusBadge('EVALUATORS_ASSIGNED')}</div>
        <div class="card-body" style="display:flex;flex-direction:column;gap:16px;">
          ${renderDetailField('Title', proposal.title)}
          ${renderDetailField('Innovation Category', proposal.innovationCategory)}
          ${renderDetailField('Technology Domain', proposal.technologyDomain)}
          ${renderDetailField('Problem Statement', proposal.problemStatement)}
          ${renderDetailField('Proposed Solution', proposal.proposedSolution)}
          ${renderDetailField('Novelty Explanation', proposal.noveltyExplanation)}
          ${renderDetailField('Difference from Existing', proposal.differenceFromExisting)}
          ${renderDetailField('Innovation Significance', proposal.innovationSignificance)}
          ${proposal.budget ? `<div><div style="font-size:12px;color:var(--text-muted);font-weight:700;text-transform:uppercase;margin-bottom:4px;">Total Budget Requested</div><div style="font-size:15px;font-weight:700;color:var(--primary-light);">${formatCurrency(proposal.budget.totalRequested)}</div></div>` : ''}
        </div>
      </div>
      ` : '<div class="card"><div class="card-body"><div class="inline-loader"><div class="spinner spinner-sm"></div> Loading proposal...</div></div></div>'}

      <div class="card">
        <div class="card-header"><div class="card-title">📊 Score the Proposal (0–10 each)</div></div>
        <div class="card-body">
          ${renderScoreInput('r-novelty', 'Novelty & Originality', '40% weight')}
          ${renderScoreInput('r-feasibility', 'Technical Feasibility', '30% weight')}
          ${renderScoreInput('r-commercial', 'Commercial Impact', '30% weight')}

          <div style="background:rgba(79,70,229,0.08);border:1px solid rgba(79,70,229,0.2);border-radius:var(--radius);padding:16px;margin-top:20px;display:flex;align-items:center;justify-content:space-between;">
            <div>
              <div style="font-size:12px;color:var(--text-muted);">Calculated Weighted Score</div>
              <div style="font-size:11px;color:var(--text-muted);">(Novelty×0.4 + Feasibility×0.3 + Commercial×0.3)</div>
            </div>
            <div style="font-family:var(--font-display);font-size:2rem;font-weight:800;color:var(--primary-light);" id="r-weighted-total">0.0</div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><div class="card-title">📝 Written Evaluation</div></div>
        <div class="card-body" style="display:flex;flex-direction:column;gap:16px;">
          <div class="form-group">
            <label class="form-label">Technical Comments <span class="required">*</span></label>
            <textarea id="r-technical" class="form-control" rows="4" placeholder="Provide detailed technical assessment of the proposal..."></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">Strengths <span class="required">*</span></label>
            <textarea id="r-strengths" class="form-control" rows="3" placeholder="List the key strengths of this proposal..."></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">Weaknesses <span class="required">*</span></label>
            <textarea id="r-weaknesses" class="form-control" rows="3" placeholder="List areas that need improvement..."></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">Recommendation <span class="required">*</span></label>
            <select id="r-recommendation" class="form-control">
              <option value="">Select recommendation</option>
              <option value="STRONGLY_RECOMMEND">Strongly Recommend for Funding</option>
              <option value="RECOMMEND">Recommend for Funding</option>
              <option value="CONDITIONAL_RECOMMEND">Conditionally Recommend (with revisions)</option>
              <option value="DO_NOT_RECOMMEND">Do Not Recommend</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Additional Remarks</label>
            <textarea id="r-remarks" class="form-control" rows="2" placeholder="Any other comments for the committee..."></textarea>
          </div>
        </div>
      </div>

      <div style="background:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.2);border-radius:var(--radius);padding:16px;">
        <div style="font-weight:700;color:var(--danger);margin-bottom:4px;">⚠️ Submission is Final</div>
        <div style="font-size:13px;color:var(--text-secondary);">Once submitted, your review will be permanently locked and cannot be modified. Ensure all information is correct before submitting.</div>
      </div>

      <button class="btn btn-primary btn-lg" onclick="submitFacultyReview(${assignment.id})">Submit Evaluation →</button>
    </div>
  `;
}

function renderScoreInput(id, label, weight) {
  return `
    <div class="score-input-group">
      <div class="score-input-label">
        <span class="score-input-name">${label}</span>
        <div style="display:flex;align-items:center;gap:12px;">
          <span class="score-input-weight">${weight}</span>
          <span class="score-input-current" id="${id}-display">5.0</span>
        </div>
      </div>
      <input type="range" id="${id}" class="score-slider" min="0" max="10" step="0.5" value="5"
        oninput="updateScoreDisplay('${id}'); updateWeightedTotal()">
      <div class="score-scale"><span>0 — Very Poor</span><span>5 — Average</span><span>10 — Excellent</span></div>
    </div>
  `;
}

function updateScoreDisplay(id) {
  const val = document.getElementById(id)?.value;
  const display = document.getElementById(`${id}-display`);
  if (display && val !== undefined) display.textContent = parseFloat(val).toFixed(1);
}

function updateWeightedTotal() {
  const n = parseFloat(document.getElementById('r-novelty')?.value || 5);
  const f = parseFloat(document.getElementById('r-feasibility')?.value || 5);
  const c = parseFloat(document.getElementById('r-commercial')?.value || 5);
  const weighted = (n * 0.4 + f * 0.3 + c * 0.3).toFixed(2);
  const el = document.getElementById('r-weighted-total');
  if (el) el.textContent = weighted;
}

async function submitFacultyReview(assignmentId) {
  const technicalComments = document.getElementById('r-technical')?.value.trim();
  const strengths = document.getElementById('r-strengths')?.value.trim();
  const weaknesses = document.getElementById('r-weaknesses')?.value.trim();
  const recommendation = document.getElementById('r-recommendation')?.value;

  if (!technicalComments || !strengths || !weaknesses || !recommendation) {
    showToast('warning', 'Missing Fields', 'Please fill in all required evaluation fields.');
    return;
  }

  const payload = {
    assignmentId,
    noveltyScore: parseFloat(document.getElementById('r-novelty')?.value || 5),
    feasibilityScore: parseFloat(document.getElementById('r-feasibility')?.value || 5),
    commercialImpactScore: parseFloat(document.getElementById('r-commercial')?.value || 5),
    technicalComments,
    strengths,
    weaknesses,
    recommendation,
    additionalRemarks: document.getElementById('r-remarks')?.value.trim() || '',
  };

  setLoading(true, 'Submitting review...');
  try {
    await api('POST', '/faculty/reviews', payload);
    showToast('success', 'Review Submitted!', 'Your evaluation has been submitted and locked successfully.');
    showFacultySection('dashboard');
  } catch (e) {
    showToast('error', 'Submission Failed', e.message);
  } finally {
    setLoading(false);
  }
}

function loadFacultyProfile() {
  const container = document.getElementById('f-profile-content');
  const u = state.user;
  container.innerHTML = `
    <div style="display:flex;align-items:center;gap:20px;margin-bottom:24px;">
      <div style="width:64px;height:64px;border-radius:50%;background:var(--gradient-secondary);display:flex;align-items:center;justify-content:center;font-size:24px;font-weight:700;color:white;">
        ${u.fullName.split(' ').map(n => n[0]).join('').slice(0,2)}
      </div>
      <div>
        <div style="font-size:1.3rem;font-weight:800;">${escHtml(u.fullName)}</div>
        <div style="color:var(--text-muted);font-size:13px;">${escHtml(u.email)}</div>
        <div style="margin-top:6px;"><span class="badge badge-review">Faculty Evaluator</span></div>
      </div>
    </div>
    <div class="divider"></div>
    <div class="review-summary-grid">
      <div class="review-field"><div class="review-field-label">Username</div><div class="review-field-value">${escHtml(u.username)}</div></div>
      <div class="review-field"><div class="review-field-label">Email</div><div class="review-field-value">${escHtml(u.email)}</div></div>
    </div>
  `;
}

// ============================================================
// ADMIN APP
// ============================================================
function initAdminApp() {
  const name = state.user.fullName;
  const initials = name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  document.getElementById('a-avatar').textContent = initials;
  document.getElementById('a-user-name').textContent = name;
  showAdminSection('dashboard');
}

function showAdminSection(section) {
  const sections = ['dashboard', 'proposals', 'proposal-detail', 'assign', 'reviews', 'decisions', 'decision-detail', 'grants', 'milestones', 'notifications', 'users'];
  sections.forEach(s => {
    const el = document.getElementById(`a-section-${s}`);
    if (el) el.classList.toggle('hidden', s !== section);
    const nav = document.getElementById(`nav-a-${s}`);
    if (nav) nav.classList.toggle('active', s === section);
  });

  const titles = {
    dashboard: 'R&D Dashboard', proposals: 'All Proposals', assign: 'Assign Evaluators',
    reviews: 'Review Monitor', decisions: 'Grant Decisions', 'decision-detail': 'Make Decision',
    grants: 'Grant Management', milestones: 'Milestone Management', notifications: 'Notifications', users: 'User Management'
  };
  document.getElementById('admin-page-title').textContent = titles[section] || 'Admin Portal';

  if (section === 'dashboard') loadAdminDashboard();
  if (section === 'proposals') loadAdminProposals();
  if (section === 'assign') loadAssignSection();
  if (section === 'reviews') loadReviewMonitor();
  if (section === 'decisions') loadDecisions();
  if (section === 'grants') loadGrants();
  if (section === 'milestones') loadMilestones();
  if (section === 'notifications') loadNotifications('a');
  if (section === 'users') loadUsers();
}

async function loadAdminDashboard() {
  try {
    const [stats, proposals] = await Promise.all([
      api('GET', '/admin/dashboard'),
      api('GET', '/admin/proposals'),
    ]);

    state.allProposals = proposals;

    document.getElementById('a-stat-total').textContent = stats.totalProposals || 0;
    document.getElementById('a-stat-new').textContent = stats.newSubmissions || 0;
    document.getElementById('a-stat-awaiting').textContent = stats.awaitingAssignment || 0;
    document.getElementById('a-stat-review').textContent = stats.underReview || 0;
    document.getElementById('a-stat-pending-dec').textContent = stats.pendingDecisions || 0;
    document.getElementById('a-stat-approved').textContent = stats.approvedGrants || 0;
    document.getElementById('a-stat-rejected').textContent = stats.rejected || 0;
    document.getElementById('a-stat-funding-req').textContent = formatCurrency(stats.totalFundingRequested || 0);

    renderAdminProposalTable(document.getElementById('a-recent-proposals'), proposals.slice(0, 8));
    loadNotificationBadge('a');
  } catch (e) {
    showToast('error', 'Error', e.message);
  }
}

async function loadAdminProposals() {
  try {
    if (!state.allProposals || state.allProposals.length === 0) {
      state.allProposals = await api('GET', '/admin/proposals');
    }
    renderAdminProposalTableFull(state.allProposals);
  } catch (e) {
    showToast('error', 'Error', e.message);
  }
}

function renderAdminProposalTable(container, proposals) {
  if (!proposals || proposals.length === 0) {
    container.innerHTML = '<div class="empty-state"><div class="empty-state-title">No proposals yet</div></div>';
    return;
  }
  container.innerHTML = `<div class="table-container" style="border:none;">
    <table>
      <thead>
        <tr><th>Proposal #</th><th>Title</th><th>Student</th><th>Status</th><th>Score</th><th>Actions</th></tr>
      </thead>
      <tbody>
        ${proposals.map(p => `
          <tr>
            <td><span class="font-mono" style="font-size:12px;">${escHtml(p.proposalNumber)}</span></td>
            <td style="max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${escHtml(p.title)}</td>
            <td>${escHtml(p.studentName)}</td>
            <td>${getStatusBadge(p.status)}</td>
            <td>${p.aggregatedScore ? `<strong>${p.aggregatedScore}/10</strong>` : '—'}</td>
            <td><button class="btn btn-secondary btn-sm" onclick="viewAdminProposal(${p.id})">View →</button></td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  </div>`;
}

function renderAdminProposalTableFull(proposals) {
  const tbody = document.getElementById('a-proposals-table');
  if (!proposals || proposals.length === 0) {
    tbody.innerHTML = '<tr><td colspan="9"><div class="empty-state"><div class="empty-state-title">No proposals found</div></div></td></tr>';
    return;
  }
  tbody.innerHTML = proposals.map(p => `
    <tr>
      <td><span class="font-mono" style="font-size:12px;">${escHtml(p.proposalNumber)}</span></td>
      <td style="max-width:240px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${escHtml(p.title)}</td>
      <td>${escHtml(p.studentName)}</td>
      <td>${escHtml(p.studentDepartment || '—')}</td>
      <td>${getStatusBadge(p.status)}</td>
      <td>${p.aggregatedScore ? `<strong>${p.aggregatedScore}/10</strong>` : '—'}</td>
      <td>${formatCurrency(p.totalBudgetRequested || 0)}</td>
      <td>${formatDate(p.submissionDate || p.createdAt)}</td>
      <td style="white-space:nowrap;">
        <button class="btn btn-secondary btn-sm" onclick="viewAdminProposal(${p.id})">View</button>
        ${(p.status === 'SUBMITTED' || p.status === 'PENDING_EVALUATOR_ASSIGNMENT') ? `<button class="btn btn-outline btn-sm" onclick="quickAssign(${p.id})" style="margin-left:6px;">Assign</button>` : ''}
        ${p.status === 'PENDING_GRANT_DECISION' ? `<button class="btn btn-primary btn-sm" onclick="openDecisionDetail(${p.id})" style="margin-left:6px;">Decide</button>` : ''}
      </td>
    </tr>
  `).join('');
}

function filterAdminProposals() {
  const search = document.getElementById('a-search')?.value.toLowerCase() || '';
  const status = document.getElementById('a-filter-status')?.value || '';
  let filtered = state.allProposals;
  if (search) filtered = filtered.filter(p => p.title.toLowerCase().includes(search) || p.studentName.toLowerCase().includes(search) || p.proposalNumber.toLowerCase().includes(search));
  if (status) filtered = filtered.filter(p => p.status === status);
  renderAdminProposalTableFull(filtered);
}

async function viewAdminProposal(proposalId) {
  showAdminSection('proposal-detail');
  const container = document.getElementById('a-proposal-detail-content');
  container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading...</div>';

  try {
    const p = await api('GET', `/admin/proposals/${proposalId}`);
    document.getElementById('a-detail-title').textContent = p.title;
    document.getElementById('a-detail-number').textContent = p.proposalNumber;

    // Get reviews
    let reviews = [];
    try { reviews = await api('GET', `/admin/proposals/${proposalId}/reviews`); } catch (e) {}

    container.innerHTML = renderAdminProposalDetail(p, reviews);
  } catch (e) {
    container.innerHTML = `<div class="empty-state"><div class="empty-state-title">Error loading proposal</div></div>`;
    showToast('error', 'Error', e.message);
  }
}

function renderAdminProposalDetail(p, reviews) {
  return `
    <div class="tracker-container">
      <div class="tracker-title">📍 Status Tracker</div>
      <div class="tracker-steps">${getDetailedTracker(p.status)}</div>
    </div>

    <div style="display:grid;grid-template-columns:2fr 1fr;gap:24px;align-items:start;">
      <div style="display:flex;flex-direction:column;gap:20px;">
        <div class="card">
          <div class="card-header">
            <div class="card-title">📋 Proposal Information</div>
            ${getStatusBadge(p.status)}
          </div>
          <div class="card-body">
            <div class="review-summary-grid">
              <div class="review-field"><div class="review-field-label">Proposal #</div><div class="review-field-value font-mono">${escHtml(p.proposalNumber)}</div></div>
              <div class="review-field"><div class="review-field-label">Student</div><div class="review-field-value">${escHtml(p.studentName)}</div></div>
              <div class="review-field"><div class="review-field-label">Department</div><div class="review-field-value">${escHtml(p.studentDepartment || '—')}</div></div>
              <div class="review-field"><div class="review-field-label">Register #</div><div class="review-field-value">${escHtml(p.studentRegisterNumber || '—')}</div></div>
              <div class="review-field"><div class="review-field-label">Category</div><div class="review-field-value">${escHtml(p.innovationCategory)}</div></div>
              <div class="review-field"><div class="review-field-label">Patent Potential</div><div class="review-field-value">${escHtml(p.patentPotential)}</div></div>
              ${p.aggregatedScore ? `<div class="review-field"><div class="review-field-label">Aggregated Score</div><div class="review-field-value fw-bold text-primary-accent">${p.aggregatedScore}/10</div></div>` : ''}
              <div class="review-field"><div class="review-field-label">Submitted</div><div class="review-field-value">${formatDate(p.submissionDate)}</div></div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-header"><div class="card-title">💡 Project Content</div></div>
          <div class="card-body" style="display:flex;flex-direction:column;gap:16px;">
            ${renderDetailField('Problem Statement', p.problemStatement)}
            ${renderDetailField('Proposed Solution', p.proposedSolution)}
            ${renderDetailField('Novelty', p.noveltyExplanation)}
            ${renderDetailField('Difference from Existing', p.differenceFromExisting)}
            ${renderDetailField('Innovation Significance', p.innovationSignificance)}
          </div>
        </div>

        ${reviews && reviews.length > 0 ? `
        <div class="card">
          <div class="card-header"><div class="card-title">📋 Faculty Reviews (${reviews.length})</div></div>
          <div class="card-body" style="display:flex;flex-direction:column;gap:20px;">
            ${reviews.map(r => `
              <div style="background:var(--bg-secondary);border:1px solid var(--border);border-radius:var(--radius);padding:20px;">
                <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
                  <div>
                    <div style="font-weight:700;">Evaluator ${r.evaluatorNumber}: ${escHtml(r.facultyName)}</div>
                    <div style="font-size:12px;color:var(--text-muted);">Submitted: ${formatDate(r.submittedAt)}</div>
                  </div>
                  <div style="text-align:right;">
                    <div style="font-size:11px;color:var(--text-muted);">Weighted Score</div>
                    <div style="font-family:var(--font-display);font-size:1.5rem;font-weight:800;color:var(--primary-light);">${r.weightedScore || '—'}/10</div>
                  </div>
                </div>
                ${r.scores ? `
                <div class="scores-breakdown" style="margin-bottom:16px;">
                  <div class="score-criterion"><div class="score-criterion-label">Novelty</div><div class="score-criterion-value">${r.scores.noveltyScore}</div><div class="score-criterion-weight">40%</div></div>
                  <div class="score-criterion"><div class="score-criterion-label">Feasibility</div><div class="score-criterion-value">${r.scores.feasibilityScore}</div><div class="score-criterion-weight">30%</div></div>
                  <div class="score-criterion"><div class="score-criterion-label">Commercial</div><div class="score-criterion-value">${r.scores.commercialImpactScore}</div><div class="score-criterion-weight">30%</div></div>
                </div>
                ` : ''}
                ${renderDetailField('Technical Comments', r.technicalComments)}
                ${renderDetailField('Strengths', r.strengths)}
                ${renderDetailField('Weaknesses', r.weaknesses)}
                <div><span class="badge badge-review">${r.recommendation?.replace(/_/g, ' ')}</span></div>
              </div>
            `).join('')}
          </div>
        </div>
        ` : ''}
      </div>

      <div style="display:flex;flex-direction:column;gap:20px;">
        <!-- Quick Actions -->
        <div class="card">
          <div class="card-header"><div class="card-title">⚡ Quick Actions</div></div>
          <div class="card-body" style="display:flex;flex-direction:column;gap:10px;">
            ${(p.status === 'SUBMITTED' || p.status === 'PENDING_EVALUATOR_ASSIGNMENT') ?
              `<button class="btn btn-primary btn-full" onclick="quickAssign(${p.id})">👥 Assign Evaluators</button>` : ''}
            ${p.status === 'PENDING_GRANT_DECISION' ?
              `<button class="btn btn-success btn-full" onclick="openDecisionDetail(${p.id})">⚖️ Make Grant Decision</button>` : ''}
            ${(p.status === 'GRANT_SANCTIONED' || p.status === 'MILESTONE_TRACKING') ?
              `<button class="btn btn-secondary btn-full" onclick="openAddMilestoneModalForProposal(${p.id})">+ Add Milestone</button>` : ''}
          </div>
        </div>

        ${p.budget ? `
        <div class="card">
          <div class="card-header"><div class="card-title">💰 Budget</div></div>
          <div class="card-body">
            ${renderBudgetRow('Equipment', p.budget.equipmentCost)}
            ${renderBudgetRow('Software', p.budget.softwareCost)}
            ${renderBudgetRow('Materials', p.budget.materialsCost)}
            ${renderBudgetRow('Prototype', p.budget.prototypeCost)}
            ${renderBudgetRow('Testing', p.budget.testingCost)}
            ${renderBudgetRow('Other', p.budget.otherCost)}
            <div style="border-top:1px solid var(--border);margin-top:12px;padding-top:12px;display:flex;justify-content:space-between;">
              <span style="font-weight:700;">Total</span>
              <span style="color:var(--primary-light);font-weight:800;">${formatCurrency(p.budget.totalRequested)}</span>
            </div>
          </div>
        </div>
        ` : ''}

        ${p.funding ? `
        <div class="card">
          <div class="card-header"><div class="card-title">🏦 Grant Funding</div></div>
          <div class="card-body">
            <div class="review-field mb-2"><div class="review-field-label">Status</div><div>${getStatusBadge(p.funding.grantStatus)}</div></div>
            ${p.funding.approvedAmount ? `<div class="review-field mb-2"><div class="review-field-label">Approved</div><div class="review-field-value text-success fw-bold">${formatCurrency(p.funding.approvedAmount)}</div></div>` : ''}
            ${p.funding.grantReference ? `<div class="review-field mb-2"><div class="review-field-label">Reference</div><div class="review-field-value font-mono" style="font-size:12px;">${escHtml(p.funding.grantReference)}</div></div>` : ''}
          </div>
        </div>
        ` : ''}

        ${p.milestones && p.milestones.length > 0 ? `
        <div class="card">
          <div class="card-header"><div class="card-title">🎯 Milestones</div></div>
          <div class="card-body">
            ${p.milestones.map(m => `
              <div class="milestone-card" style="margin-bottom:12px;">
                <div class="milestone-header">
                  <div class="milestone-title" style="font-size:14px;">${escHtml(m.title)}</div>
                  ${getStatusBadge(m.status)}
                </div>
                <div class="progress-bar-container">
                  <div class="progress-bar ${m.completionPercentage === 100 ? 'green' : ''}" style="width:${m.completionPercentage}%"></div>
                </div>
                <div style="display:flex;justify-content:space-between;font-size:11px;color:var(--text-muted);margin-top:4px;">
                  <span>${m.completionPercentage}%</span>
                  <span>Due: ${formatDate(m.dueDate)}</span>
                  <button class="btn btn-outline btn-sm" style="padding:2px 8px;font-size:11px;" onclick="openUpdateMilestone(${m.id}, '${m.status}', ${m.completionPercentage})">Update</button>
                </div>
              </div>
            `).join('')}
          </div>
        </div>
        ` : ''}

        ${p.statusHistory && p.statusHistory.length > 0 ? `
        <div class="card">
          <div class="card-header"><div class="card-title">📜 Status History</div></div>
          <div class="card-body">
            ${p.statusHistory.slice().reverse().slice(0,5).map(h => `
              <div style="border-left:2px solid var(--primary);padding-left:12px;margin-bottom:10px;">
                <div style="font-size:13px;font-weight:600;">→ ${formatStatus(h.newStatus)}</div>
                <div style="font-size:11px;color:var(--text-muted);">${escHtml(h.remarks || '')} | ${formatDate(h.changedAt)}</div>
              </div>
            `).join('')}
          </div>
        </div>
        ` : ''}
      </div>
    </div>
  `;
}

async function loadAssignSection() {
  const listContainer = document.getElementById('assign-proposal-list');
  try {
    const [proposals, faculty] = await Promise.all([
      api('GET', '/admin/proposals'),
      api('GET', '/admin/faculty'),
    ]);
    state.allProposals = proposals;
    state.allFaculty = faculty;

    const assignable = proposals.filter(p => p.status === 'SUBMITTED' || p.status === 'PENDING_EVALUATOR_ASSIGNMENT');

    if (assignable.length === 0) {
      listContainer.innerHTML = '<div class="empty-state"><div class="empty-state-icon">✅</div><div class="empty-state-title">All caught up!</div><div class="empty-state-desc">No proposals awaiting evaluator assignment.</div></div>';
      return;
    }

    listContainer.innerHTML = assignable.map(p => `
      <div onclick="selectProposalForAssign(${p.id})" style="padding:14px;border:1px solid var(--border);border-radius:var(--radius);margin-bottom:10px;cursor:pointer;transition:var(--transition);"
        onmouseover="this.style.borderColor='var(--primary)'" onmouseout="this.style.borderColor='var(--border)'"
        id="assign-p-${p.id}">
        <div style="font-size:14px;font-weight:700;margin-bottom:4px;">${escHtml(p.title)}</div>
        <div style="font-size:12px;color:var(--text-muted);">${escHtml(p.proposalNumber)} | ${escHtml(p.studentName)} | ${formatCurrency(p.totalBudgetRequested || 0)}</div>
        <div style="margin-top:6px;">${getStatusBadge(p.status)}</div>
      </div>
    `).join('');
  } catch (e) {
    listContainer.innerHTML = '<div class="empty-state"><div class="empty-state-title">Error loading proposals</div></div>';
    showToast('error', 'Error', e.message);
  }
}

let selectedAssignProposalId = null;

function selectProposalForAssign(proposalId) {
  selectedAssignProposalId = proposalId;
  document.querySelectorAll('[id^=assign-p-]').forEach(el => el.style.background = '');
  const selected = document.getElementById(`assign-p-${proposalId}`);
  if (selected) selected.style.background = 'rgba(79,70,229,0.1)';

  const proposal = state.allProposals.find(p => p.id === proposalId);
  const container = document.getElementById('assign-form-content');

  const facultyOptions = state.allFaculty.map(f =>
    `<option value="${f.id}">${escHtml(f.fullName)} — ${escHtml(f.department)} (${escHtml(f.designation)})</option>`
  ).join('');

  container.innerHTML = `
    <div style="margin-bottom:16px;">
      <div style="font-size:14px;font-weight:700;">${escHtml(proposal?.title)}</div>
      <div style="font-size:12px;color:var(--text-muted);">By: ${escHtml(proposal?.studentName)}</div>
    </div>
    <div class="form-group">
      <label class="form-label">Evaluator 1 <span class="required">*</span></label>
      <select id="eval1-select" class="form-control">
        <option value="">Select faculty evaluator 1...</option>
        ${facultyOptions}
      </select>
    </div>
    <div class="form-group">
      <label class="form-label">Evaluator 2 <span class="required">*</span></label>
      <select id="eval2-select" class="form-control">
        <option value="">Select faculty evaluator 2...</option>
        ${facultyOptions}
      </select>
    </div>
    <div class="form-group">
      <label class="form-label">Review Deadline</label>
      <input type="date" id="eval-deadline" class="form-control" value="${getTwoWeeksFromNow()}">
    </div>
    <button class="btn btn-primary btn-full" onclick="submitEvaluatorAssignment()">Assign Evaluators</button>
  `;
}

async function submitEvaluatorAssignment() {
  const eval1 = parseInt(document.getElementById('eval1-select')?.value);
  const eval2 = parseInt(document.getElementById('eval2-select')?.value);
  const deadline = document.getElementById('eval-deadline')?.value;

  if (!eval1 || !eval2) {
    showToast('warning', 'Missing Selection', 'Please select both evaluators.');
    return;
  }
  if (eval1 === eval2) {
    showToast('warning', 'Same Evaluator', 'Evaluator 1 and Evaluator 2 must be different faculty members.');
    return;
  }

  setLoading(true, 'Assigning evaluators...');
  try {
    await api('POST', '/admin/assign-evaluators', {
      proposalId: selectedAssignProposalId,
      evaluator1FacultyId: eval1,
      evaluator2FacultyId: eval2,
      deadline,
    });
    showToast('success', 'Evaluators Assigned!', 'Both evaluators have been notified.');
    state.allProposals = [];
    loadAssignSection();
    document.getElementById('assign-form-content').innerHTML = `
      <div class="empty-state"><div class="empty-state-icon">✅</div><div class="empty-state-title">Assigned!</div><div class="empty-state-desc">Select another proposal to assign.</div></div>
    `;
  } catch (e) {
    showToast('error', 'Assignment Failed', e.message);
  } finally {
    setLoading(false);
  }
}

function quickAssign(proposalId) {
  showAdminSection('assign');
  setTimeout(() => selectProposalForAssign(proposalId), 300);
}

async function loadReviewMonitor() {
  const tbody = document.getElementById('a-reviews-table');
  try {
    const reviews = await api('GET', '/admin/reviews');
    state.allReviews = reviews;

    if (!reviews || reviews.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8"><div class="empty-state"><div class="empty-state-title">No reviews in progress</div></div></td></tr>';
      return;
    }

    tbody.innerHTML = reviews.map(r => `
      <tr>
        <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
          <div style="font-weight:600;font-size:13px;">${escHtml(r.proposalTitle)}</div>
          <div style="font-size:11px;color:var(--text-muted);">${escHtml(r.proposalNumber)}</div>
        </td>
        <td>${escHtml(r.studentName)}</td>
        <td>
          <div style="font-size:13px;">${r.evaluator1 ? escHtml(r.evaluator1.facultyName) : '—'}</div>
          <div>${r.evaluator1 ? getStatusBadge(r.evaluator1.status) : ''}</div>
        </td>
        <td>${r.evaluator1?.score ? `<strong>${r.evaluator1.score}/10</strong>` : '—'}</td>
        <td>
          <div style="font-size:13px;">${r.evaluator2 ? escHtml(r.evaluator2.facultyName) : '—'}</div>
          <div>${r.evaluator2 ? getStatusBadge(r.evaluator2.status) : ''}</div>
        </td>
        <td>${r.evaluator2?.score ? `<strong>${r.evaluator2.score}/10</strong>` : '—'}</td>
        <td>${r.aggregatedScore ? `<strong style="color:var(--primary-light)">${r.aggregatedScore}/10</strong>` : '—'}</td>
        <td>${getStatusBadge(r.proposalStatus)}</td>
      </tr>
    `).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="8"><div class="empty-state"><div class="empty-state-title">Error loading reviews</div></div></td></tr>`;
    showToast('error', 'Error', e.message);
  }
}

async function loadDecisions() {
  const container = document.getElementById('a-decisions-list');
  container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading...</div>';
  try {
    const proposals = await api('GET', '/admin/proposals');
    const ready = proposals.filter(p => p.status === 'PENDING_GRANT_DECISION' || p.status === 'REVIEW_2_COMPLETED' || p.status === 'SCORES_AGGREGATED');

    if (ready.length === 0) {
      container.innerHTML = `<div class="empty-state">
        <div class="empty-state-icon">✅</div>
        <div class="empty-state-title">No Pending Decisions</div>
        <div class="empty-state-desc">All proposals have been reviewed. Check back when evaluations are complete.</div>
      </div>`;
      return;
    }

    container.innerHTML = ready.map(p => `
      <div class="proposal-card" style="margin-bottom:16px;" onclick="openDecisionDetail(${p.id})">
        <div class="proposal-card-header">
          <div class="proposal-card-title">${escHtml(p.title)}</div>
          ${getStatusBadge(p.status)}
        </div>
        <div class="proposal-card-meta">
          <span>🎓 ${escHtml(p.studentName)}</span>
          <span>🏫 ${escHtml(p.studentDepartment || '—')}</span>
          <span>⭐ Score: ${p.aggregatedScore ? `${p.aggregatedScore}/10` : 'Pending'}</span>
          <span>💰 ${formatCurrency(p.totalBudgetRequested || 0)}</span>
        </div>
        <div class="proposal-card-footer">
          <span style="font-size:13px;color:var(--text-muted);">🔖 ${escHtml(p.proposalNumber)}</span>
          <button class="btn btn-primary btn-sm" onclick="event.stopPropagation();openDecisionDetail(${p.id})">⚖️ Make Decision →</button>
        </div>
      </div>
    `).join('');
  } catch (e) {
    container.innerHTML = '<div class="empty-state"><div class="empty-state-title">Error loading</div></div>';
    showToast('error', 'Error', e.message);
  }
}

async function openDecisionDetail(proposalId) {
  showAdminSection('decision-detail');
  const container = document.getElementById('a-decision-detail-content');
  container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading decision summary...</div>';

  try {
    const [summary, proposal] = await Promise.all([
      api('GET', `/admin/decisions/${proposalId}`),
      api('GET', `/admin/proposals/${proposalId}`),
    ]);

    document.getElementById('a-decision-subtitle').textContent = `${summary.proposalNumber} — ${summary.proposalTitle}`;

    const canDecide = proposal.status === 'PENDING_GRANT_DECISION' || proposal.status === 'REVIEW_2_COMPLETED';

    container.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:24px;">
        <!-- Score Summary -->
        <div class="card">
          <div class="card-header"><div class="card-title">📊 Evaluation Summary</div>${getStatusBadge(summary.proposalStatus)}</div>
          <div class="card-body">
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:16px;margin-bottom:24px;">
              <div style="text-align:center;background:var(--bg-secondary);border-radius:var(--radius);padding:20px;">
                <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px;">REQUESTED FUNDING</div>
                <div style="font-family:var(--font-display);font-size:1.4rem;font-weight:800;color:var(--warning);">${formatCurrency(summary.requestedFunding)}</div>
              </div>
              <div style="text-align:center;background:var(--bg-secondary);border-radius:var(--radius);padding:20px;">
                <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px;">AGGREGATED SCORE</div>
                <div style="font-family:var(--font-display);font-size:1.4rem;font-weight:800;color:var(--primary-light);">${summary.aggregatedScore ? `${summary.aggregatedScore}/10` : 'Pending'}</div>
              </div>
              <div style="text-align:center;background:var(--bg-secondary);border-radius:var(--radius);padding:20px;">
                <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px;">EVALUATOR 1</div>
                <div style="font-family:var(--font-display);font-size:1.4rem;font-weight:800;color:${summary.evaluator1?.status === 'COMPLETED' ? 'var(--success)' : 'var(--warning)'};">${summary.evaluator1?.score ? `${summary.evaluator1.score}/10` : '⏳'}</div>
              </div>
              <div style="text-align:center;background:var(--bg-secondary);border-radius:var(--radius);padding:20px;">
                <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px;">EVALUATOR 2</div>
                <div style="font-family:var(--font-display);font-size:1.4rem;font-weight:800;color:${summary.evaluator2?.status === 'COMPLETED' ? 'var(--success)' : 'var(--warning)'};">${summary.evaluator2?.score ? `${summary.evaluator2.score}/10` : '⏳'}</div>
              </div>
            </div>

            <div class="evaluator-cards">
              ${summary.review1Details ? renderEvaluatorReviewCard(summary.review1Details, summary.evaluator1, 1) : `<div style="background:var(--bg-secondary);border:1px solid var(--border);border-radius:var(--radius);padding:20px;text-align:center;color:var(--text-muted);">Evaluator 1 review pending</div>`}
              ${summary.review2Details ? renderEvaluatorReviewCard(summary.review2Details, summary.evaluator2, 2) : `<div style="background:var(--bg-secondary);border:1px solid var(--border);border-radius:var(--radius);padding:20px;text-align:center;color:var(--text-muted);">Evaluator 2 review pending</div>`}
            </div>
          </div>
        </div>

        ${summary.latestDecision ? `
        <div class="card">
          <div class="card-header"><div class="card-title">⚖️ Previous Decision</div>${getStatusBadge(summary.latestDecision.decision)}</div>
          <div class="card-body">
            <div class="review-summary-grid">
              <div class="review-field"><div class="review-field-label">Decision</div><div class="review-field-value">${escHtml(summary.latestDecision.decision)}</div></div>
              <div class="review-field"><div class="review-field-label">Decided By</div><div class="review-field-value">${escHtml(summary.latestDecision.decidedBy)}</div></div>
              <div class="review-field"><div class="review-field-label">Date</div><div class="review-field-value">${formatDate(summary.latestDecision.decisionDate)}</div></div>
              <div class="review-field"><div class="review-field-label">Score at Decision</div><div class="review-field-value">${summary.latestDecision.aggregatedScoreAtDecision || '—'}</div></div>
            </div>
            ${summary.latestDecision.decisionRemarks ? `<div class="mt-3">${renderDetailField('Remarks', summary.latestDecision.decisionRemarks)}</div>` : ''}
          </div>
        </div>
        ` : ''}

        ${canDecide ? `
        <div class="card">
          <div class="card-header"><div class="card-title">⚖️ Make Grant Decision</div></div>
          <div class="card-body">
            <p style="font-size:14px;color:var(--text-secondary);margin-bottom:16px;">Based on the evaluations above, make a formal grant decision for this proposal.</p>
            <button class="btn btn-primary btn-lg" onclick="openDecisionModal(${summary.proposalId}, '${escHtml(summary.proposalTitle)}')">Open Decision Form →</button>
          </div>
        </div>
        ` : ''}
      </div>
    `;
  } catch (e) {
    container.innerHTML = `<div class="empty-state"><div class="empty-state-title">Error loading decision summary: ${e.message}</div></div>`;
    showToast('error', 'Error', e.message);
  }
}

function renderEvaluatorReviewCard(review, evaluator, num) {
  return `
    <div class="evaluator-card">
      <div class="evaluator-card-header">
        <div class="evaluator-avatar">${evaluator?.facultyName?.split(' ').map(n=>n[0]).join('').slice(0,2) || 'E'+num}</div>
        <div>
          <div class="evaluator-name">${escHtml(evaluator?.facultyName || `Evaluator ${num}`)}</div>
          <div class="evaluator-number">Evaluator ${num}</div>
        </div>
        <div style="margin-left:auto;font-family:var(--font-display);font-size:1.3rem;font-weight:800;color:var(--primary-light);">${review.weightedScore}/10</div>
      </div>
      ${review.scores ? `
      <div class="criterion-scores">
        <div class="criterion-row">
          <span class="criterion-name">Novelty (40%)</span>
          <div class="criterion-bar"><div class="criterion-bar-fill" style="width:${(review.scores.noveltyScore/10)*100}%"></div></div>
          <span class="criterion-score">${review.scores.noveltyScore}</span>
        </div>
        <div class="criterion-row">
          <span class="criterion-name">Feasibility (30%)</span>
          <div class="criterion-bar"><div class="criterion-bar-fill" style="width:${(review.scores.feasibilityScore/10)*100}%"></div></div>
          <span class="criterion-score">${review.scores.feasibilityScore}</span>
        </div>
        <div class="criterion-row">
          <span class="criterion-name">Commercial (30%)</span>
          <div class="criterion-bar"><div class="criterion-bar-fill" style="width:${(review.scores.commercialImpactScore/10)*100}%"></div></div>
          <span class="criterion-score">${review.scores.commercialImpactScore}</span>
        </div>
      </div>
      ` : ''}
      ${review.recommendation ? `<div style="margin-top:12px;"><span class="badge badge-review">${review.recommendation?.replace(/_/g,' ')}</span></div>` : ''}
    </div>
  `;
}

function openDecisionModal(proposalId, title) {
  document.getElementById('decision-proposal-id').value = proposalId;
  document.getElementById('decision-modal-title').textContent = `Decision for: ${title}`;
  document.getElementById('decision-approve-fields').style.display = 'block';
  document.getElementById('decision-revision-fields').classList.add('hidden');
  // Set today as default
  document.getElementById('decision-sanction-date').value = new Date().toISOString().split('T')[0];
  openModal('modal-decision');
}

function selectDecision(decision) {
  const approveFields = document.getElementById('decision-approve-fields');
  const revisionFields = document.getElementById('decision-revision-fields');
  if (decision === 'APPROVED') {
    approveFields.style.display = 'block';
    revisionFields.classList.add('hidden');
  } else if (decision === 'REVISION_REQUESTED') {
    approveFields.style.display = 'none';
    revisionFields.classList.remove('hidden');
  } else {
    approveFields.style.display = 'none';
    revisionFields.classList.add('hidden');
  }
  // Highlight selected
  ['approve', 'reject', 'revision'].forEach(opt => {
    const el = document.getElementById(`dec-opt-${opt}`);
    if (el) el.style.borderColor = 'var(--border)';
  });
  const selected = document.getElementById(`dec-opt-${decision.toLowerCase().split('_')[0]}`);
  if (selected) selected.style.borderColor = 'var(--primary)';
}

async function submitDecision() {
  const proposalId = parseInt(document.getElementById('decision-proposal-id').value);
  const decision = document.querySelector('input[name="decision"]:checked')?.value;

  if (!decision) {
    showToast('warning', 'No Decision Selected', 'Please select a decision.');
    return;
  }

  const payload = {
    proposalId,
    decision,
    decisionRemarks: document.getElementById('decision-remarks')?.value.trim() || '',
    revisionComments: document.getElementById('decision-revision-comments')?.value.trim() || '',
  };

  if (decision === 'APPROVED') {
    payload.approvedAmount = parseFloat(document.getElementById('decision-approved-amount')?.value || 0);
    payload.grantReference = document.getElementById('decision-grant-ref')?.value.trim();
    payload.sanctionDate = document.getElementById('decision-sanction-date')?.value;
    payload.projectPhase = document.getElementById('decision-phase')?.value.trim();
  }

  setLoading(true, 'Submitting decision...');
  try {
    await api('POST', '/admin/decisions', payload);
    showToast('success', 'Decision Recorded!', `Grant decision (${decision}) has been submitted.`);
    closeModal('modal-decision');
    state.allProposals = [];
    showAdminSection('decisions');
  } catch (e) {
    showToast('error', 'Decision Failed', e.message);
  } finally {
    setLoading(false);
  }
}

async function loadGrants() {
  const container = document.getElementById('a-grants-list');
  container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading grants...</div>';
  try {
    const grants = await api('GET', '/admin/grants');

    if (!grants || grants.length === 0) {
      container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">💰</div><div class="empty-state-title">No Grants Yet</div><div class="empty-state-desc">Approved proposals will appear here.</div></div>';
      return;
    }

    container.innerHTML = `<div class="table-container">
      <table>
        <thead>
          <tr><th>Proposal</th><th>Status</th><th>Requested</th><th>Approved</th><th>Reference</th><th>Sanction Date</th><th>Phase</th></tr>
        </thead>
        <tbody>
          ${grants.map(g => `
            <tr>
              <td>${g.proposal ? `<div style="font-weight:600;">${escHtml(g.proposal.title || '—')}</div><div style="font-size:11px;color:var(--text-muted);">${escHtml(g.proposal.proposalNumber || '')}</div>` : '—'}</td>
              <td>${getStatusBadge(g.grantStatus)}</td>
              <td>${formatCurrency(g.requestedAmount)}</td>
              <td style="font-weight:700;color:var(--success);">${g.approvedAmount ? formatCurrency(g.approvedAmount) : '—'}</td>
              <td><span class="font-mono" style="font-size:12px;">${escHtml(g.grantReference || '—')}</span></td>
              <td>${formatDate(g.sanctionDate)}</td>
              <td>${escHtml(g.projectPhase || '—')}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>`;
  } catch (e) {
    container.innerHTML = '<div class="empty-state"><div class="empty-state-title">Error loading grants</div></div>';
    showToast('error', 'Error', e.message);
  }
}

async function loadMilestones() {
  const container = document.getElementById('a-milestones-list');
  try {
    const proposals = await api('GET', '/admin/proposals');
    const active = proposals.filter(p => p.status === 'GRANT_SANCTIONED' || p.status === 'MILESTONE_TRACKING' || p.status === 'COMPLETED');

    // Populate modal dropdown
    const select = document.getElementById('milestone-proposal-id');
    if (select) {
      select.innerHTML = '<option value="">Select approved proposal...</option>' +
        active.map(p => `<option value="${p.id}">${escHtml(p.proposalNumber)} — ${escHtml(p.title)}</option>`).join('');
    }

    if (active.length === 0) {
      container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🎯</div><div class="empty-state-title">No Active Projects</div><div class="empty-state-desc">Approved grants will appear here for milestone management.</div></div>';
      return;
    }

    // Load milestones for each
    const milestoneData = await Promise.all(active.map(async p => {
      try {
        const milestones = await api('GET', `/admin/proposals/${p.id}/milestones`);
        return { proposal: p, milestones };
      } catch (e) {
        return { proposal: p, milestones: [] };
      }
    }));

    container.innerHTML = milestoneData.map(({ proposal: p, milestones }) => `
      <div class="card" style="margin-bottom:24px;">
        <div class="card-header">
          <div class="card-title">${escHtml(p.title)}</div>
          <div style="display:flex;gap:8px;align-items:center;">
            ${getStatusBadge(p.status)}
            <button class="btn btn-secondary btn-sm" onclick="openAddMilestoneModalForProposal(${p.id})">+ Add Milestone</button>
          </div>
        </div>
        <div class="card-body">
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:16px;">${escHtml(p.proposalNumber)} | ${escHtml(p.studentName)} | ${escHtml(p.studentDepartment || '—')}</div>
          ${milestones.length === 0 ? '<div class="empty-state" style="padding:24px;"><div class="empty-state-icon">📋</div><div class="empty-state-title">No milestones yet</div></div>' :
            milestones.map(m => `
              <div class="milestone-card">
                <div class="milestone-header">
                  <div class="milestone-title">${escHtml(m.title)}</div>
                  <div style="display:flex;gap:8px;align-items:center;">
                    ${getStatusBadge(m.status || m.milestoneStatus)}
                    <button class="btn btn-outline btn-sm" onclick="openUpdateMilestone(${m.id}, '${m.status || m.milestoneStatus}', ${m.completionPercentage || 0})">Update</button>
                  </div>
                </div>
                ${m.description ? `<div style="font-size:13px;color:var(--text-secondary);margin-bottom:12px;">${escHtml(m.description)}</div>` : ''}
                <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:6px;">
                  <span style="font-size:12px;color:var(--text-muted);">Due: ${formatDate(m.dueDate)}</span>
                  <span style="font-size:13px;font-weight:700;color:var(--primary-light);">${m.completionPercentage || 0}%</span>
                </div>
                <div class="progress-bar-container">
                  <div class="progress-bar ${(m.completionPercentage || 0) === 100 ? 'green' : ''}" style="width:${m.completionPercentage || 0}%"></div>
                </div>
              </div>
            `).join('')
          }
        </div>
      </div>
    `).join('');
  } catch (e) {
    container.innerHTML = '<div class="empty-state"><div class="empty-state-title">Error loading milestones</div></div>';
    showToast('error', 'Error', e.message);
  }
}

function openAddMilestoneModal() {
  document.getElementById('milestone-title').value = '';
  document.getElementById('milestone-desc').value = '';
  document.getElementById('milestone-due').value = '';
  document.getElementById('milestone-completion').value = '0';
  openModal('modal-milestone');
}

function openAddMilestoneModalForProposal(proposalId) {
  openAddMilestoneModal();
  const select = document.getElementById('milestone-proposal-id');
  if (select) select.value = proposalId;
}

async function submitMilestone() {
  const proposalId = parseInt(document.getElementById('milestone-proposal-id')?.value);
  const title = document.getElementById('milestone-title')?.value.trim();
  const dueDate = document.getElementById('milestone-due')?.value;

  if (!proposalId || !title || !dueDate) {
    showToast('warning', 'Missing Fields', 'Please fill in Proposal, Title, and Due Date.');
    return;
  }

  setLoading(true, 'Creating milestone...');
  try {
    await api('POST', '/admin/milestones', {
      proposalId,
      title,
      description: document.getElementById('milestone-desc')?.value.trim() || '',
      dueDate,
      status: document.getElementById('milestone-status')?.value || 'NOT_STARTED',
      completionPercentage: parseInt(document.getElementById('milestone-completion')?.value || 0),
    });
    showToast('success', 'Milestone Created!', 'Project milestone has been added.');
    closeModal('modal-milestone');
    loadMilestones();
  } catch (e) {
    showToast('error', 'Error', e.message);
  } finally {
    setLoading(false);
  }
}

function openUpdateMilestone(milestoneId, status, completion) {
  document.getElementById('update-milestone-id').value = milestoneId;
  document.getElementById('update-milestone-status').value = status;
  document.getElementById('update-milestone-completion').value = completion;
  document.getElementById('update-milestone-remarks').value = '';
  openModal('modal-milestone-update');
}

async function submitMilestoneUpdate() {
  const milestoneId = parseInt(document.getElementById('update-milestone-id')?.value);
  setLoading(true, 'Updating milestone...');
  try {
    await api('PUT', `/admin/milestones/${milestoneId}`, {
      status: document.getElementById('update-milestone-status')?.value,
      completionPercentage: parseInt(document.getElementById('update-milestone-completion')?.value || 0),
      remarks: document.getElementById('update-milestone-remarks')?.value.trim() || '',
    });
    showToast('success', 'Milestone Updated!', 'Progress has been saved.');
    closeModal('modal-milestone-update');
    loadMilestones();
  } catch (e) {
    showToast('error', 'Error', e.message);
  } finally {
    setLoading(false);
  }
}

async function loadUsers() {
  const tbody = document.getElementById('a-users-table');
  try {
    const users = await api('GET', '/admin/users');
    tbody.innerHTML = users.map(u => `
      <tr>
        <td>${u.id}</td>
        <td style="font-weight:600;">${escHtml(u.fullName)}</td>
        <td class="font-mono">${escHtml(u.username)}</td>
        <td>${escHtml(u.email)}</td>
        <td>${getRoleBadge(u.role)}</td>
        <td>${u.isActive ? '<span class="badge badge-complete">Active</span>' : '<span class="badge badge-rejected">Inactive</span>'}</td>
      </tr>
    `).join('');
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><div class="empty-state-title">Error loading users</div></div></td></tr>';
    showToast('error', 'Error', e.message);
  }
}

// ============================================================
// NOTIFICATIONS
// ============================================================
async function loadNotifications(prefix) {
  const container = document.getElementById(`${prefix}-notifications-list`);
  container.innerHTML = '<div class="inline-loader"><div class="spinner spinner-sm"></div> Loading notifications...</div>';
  try {
    const notifications = await api('GET', '/notifications');
    if (!notifications || notifications.length === 0) {
      container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🔔</div><div class="empty-state-title">No Notifications</div><div class="empty-state-desc">You have no notifications yet.</div></div>';
      return;
    }

    container.innerHTML = `<div class="notifications-list">` + notifications.map(n => {
      const isUnread = (n.read === false || n.isRead === false || (!n.read && !n.isRead));
      return `
      <div class="notification-item ${isUnread ? 'unread' : ''}" onclick="markNotifRead(${n.id}, '${prefix}')">
        <div class="notification-icon">${getNotifIcon(n.notificationType)}</div>
        <div class="notification-content">
          <div class="notification-title">${escHtml(n.title)}</div>
          <div class="notification-msg">${escHtml(n.message)}</div>
          <div class="notification-time">${formatDate(n.createdAt)}</div>
        </div>
        ${isUnread ? '<div style="width:8px;height:8px;border-radius:50%;background:var(--primary);flex-shrink:0;margin-top:6px;"></div>' : ''}
      </div>
    `;}).join('') + '</div>';
  } catch (e) {
    container.innerHTML = '<div class="empty-state"><div class="empty-state-title">Error loading notifications</div></div>';
  }
}

async function loadNotificationBadge(prefix) {
  try {
    const data = await api('GET', '/notifications/unread-count');
    const count = data.count || 0;
    const badge1 = document.getElementById(`${prefix}-notif-badge`);
    const badge2 = document.getElementById(`${prefix}-topbar-notif-badge`);
    if (count > 0) {
      [badge1, badge2].forEach(b => {
        if (b) { b.classList.remove('hidden'); b.textContent = count > 9 ? '9+' : count; }
      });
    } else {
      [badge1, badge2].forEach(b => { if (b) b.classList.add('hidden'); });
    }
  } catch (e) {}
}

async function markNotifRead(id, prefix) {
  try {
    await api('PATCH', `/notifications/${id}/read`);
    loadNotifications(prefix);
    loadNotificationBadge(prefix);
  } catch (e) {}
}

async function markAllNotificationsRead(prefix) {
  try {
    await api('PATCH', '/notifications/mark-all-read');
    showToast('success', 'Done', 'All notifications marked as read.');
    loadNotifications(prefix);
    loadNotificationBadge(prefix);
  } catch (e) {
    showToast('error', 'Error', e.message);
  }
}

// ============================================================
// HELPERS & UTILITIES
// ============================================================
function escHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function formatCurrency(amount) {
  if (!amount && amount !== 0) return '—';
  const num = parseFloat(amount);
  if (isNaN(num)) return '—';
  if (num >= 100000) return `₹${(num / 100000).toFixed(1)}L`;
  if (num >= 1000) return `₹${(num / 1000).toFixed(1)}K`;
  return `₹${num.toLocaleString('en-IN')}`;
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    if (isNaN(d)) return dateStr;
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  } catch (e) { return dateStr; }
}

function formatStatus(status) {
  if (!status) return '—';
  return status.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
}

function getTwoWeeksFromNow() {
  const d = new Date();
  d.setDate(d.getDate() + 14);
  return d.toISOString().split('T')[0];
}

function getStatusBadge(status) {
  const map = {
    DRAFT: 'badge-draft', SUBMITTED: 'badge-submitted',
    PENDING_EVALUATOR_ASSIGNMENT: 'badge-processing',
    EVALUATORS_ASSIGNED: 'badge-assigned',
    UNDER_REVIEW: 'badge-review', REVIEW_1_COMPLETED: 'badge-review',
    REVIEW_2_COMPLETED: 'badge-review', SCORES_AGGREGATED: 'badge-review',
    PENDING_GRANT_DECISION: 'badge-processing', RESUBMITTED: 'badge-submitted',
    APPROVED: 'badge-approved', REJECTED: 'badge-rejected',
    PENDING_REVISION: 'badge-revision', REVISION_REQUESTED: 'badge-revision',
    GRANT_SANCTIONED: 'badge-sanctioned', MILESTONE_TRACKING: 'badge-milestone',
    COMPLETED: 'badge-completed', PENDING: 'badge-pending',
    IN_PROGRESS: 'badge-review', NOT_STARTED: 'badge-draft',
    COMPLETED_STATUS: 'badge-complete', SANCTIONED: 'badge-sanctioned',
    STUDENT: 'badge-assigned', FACULTY: 'badge-review', ADMIN: 'badge-warning',
  };
  const cssClass = map[status] || 'badge-draft';
  const label = formatStatus(status);
  return `<span class="badge ${cssClass}">${label}</span>`;
}

function getRoleBadge(role) {
  const classes = { STUDENT: 'badge-assigned', FACULTY: 'badge-review', ADMIN: 'badge-processing' };
  return `<span class="badge ${classes[role] || 'badge-draft'}">${role}</span>`;
}

function getNotifIcon(type) {
  const icons = {
    PROPOSAL_SUBMITTED: '📨', EVALUATORS_ASSIGNED: '👥', ASSIGNMENT: '📋',
    PROPOSAL_APPROVED: '✅', PROPOSAL_REJECTED: '❌', REVISION_REQUESTED: '📝',
    GRANT_SANCTIONED: '💰', MILESTONE_ADDED: '🎯', MILESTONE_UPDATED: '🔄',
    REVIEWS_COMPLETE: '📊', EVALUATION_COMPLETE: '✅',
  };
  return icons[type] || '🔔';
}

const PROPOSAL_STATUSES = [
  { key: 'DRAFT', label: 'Draft', icon: '📝' },
  { key: 'SUBMITTED', label: 'Submitted', icon: '📨' },
  { key: 'PENDING_EVALUATOR_ASSIGNMENT', label: 'Pending Assignment', icon: '⏳' },
  { key: 'EVALUATORS_ASSIGNED', label: 'Evaluators Assigned', icon: '👥' },
  { key: 'UNDER_REVIEW', label: 'Under Review', icon: '🔍' },
  { key: 'REVIEW_1_COMPLETED', label: 'Review 1 Done', icon: '1️⃣' },
  { key: 'REVIEW_2_COMPLETED', label: 'Review 2 Done', icon: '2️⃣' },
  { key: 'PENDING_GRANT_DECISION', label: 'Pending Decision', icon: '⚖️' },
  { key: 'APPROVED', label: 'Approved', icon: '✅' },
  { key: 'GRANT_SANCTIONED', label: 'Grant Sanctioned', icon: '💰' },
  { key: 'MILESTONE_TRACKING', label: 'Milestone Tracking', icon: '🎯' },
  { key: 'COMPLETED', label: 'Completed', icon: '🏆' },
];

function getDetailedTracker(currentStatus) {
  const statusIndex = PROPOSAL_STATUSES.findIndex(s => s.key === currentStatus);
  return PROPOSAL_STATUSES.map((s, i) => {
    const isCompleted = i < statusIndex;
    const isActive = i === statusIndex;
    return `
      <div class="tracker-step ${isCompleted ? 'completed' : isActive ? 'active' : ''}">
        <div class="tracker-dot">${isCompleted ? '✓' : s.icon}</div>
        <div class="tracker-label">${s.label}</div>
      </div>
    `;
  }).join('');
}

function getTrackerHtml(status) {
  return `<div class="tracker-container" style="padding:0;background:none;border:none;">
    <div style="display:flex;gap:0;overflow-x:auto;padding:4px 0;">
      ${getDetailedTracker(status)}
    </div>
  </div>`;
}

function renderDetailField(label, value) {
  if (!value) return '';
  return `
    <div>
      <div style="font-size:12px;font-weight:700;color:var(--text-muted);text-transform:uppercase;letter-spacing:0.04em;margin-bottom:6px;">${label}</div>
      <div style="font-size:14px;color:var(--text-secondary);line-height:1.6;background:var(--bg-secondary);border-radius:var(--radius-sm);padding:12px 14px;">${escHtml(value)}</div>
    </div>
  `;
}

function renderBudgetRow(label, value) {
  if (!value || parseFloat(value) === 0) return '';
  return `<div style="display:flex;justify-content:space-between;font-size:13px;padding:4px 0;border-bottom:1px solid rgba(99,102,241,0.08);">
    <span style="color:var(--text-muted);">${label}</span>
    <span style="color:var(--text-primary);">${formatCurrency(value)}</span>
  </div>`;
}

// ============================================================
// MODALS
// ============================================================
function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('hidden');
  document.body.style.overflow = '';
}

// Close modal on overlay click
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.add('hidden');
    document.body.style.overflow = '';
  }
});

// ============================================================
// TOASTS
// ============================================================
function showToast(type, title, message, duration = 5000) {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;

  const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };

  toast.innerHTML = `
    <span class="toast-icon">${icons[type] || 'ℹ️'}</span>
    <div class="toast-content">
      <div class="toast-title">${escHtml(title)}</div>
      <div class="toast-msg">${escHtml(message)}</div>
    </div>
    <button class="toast-close" onclick="this.parentElement.remove()">✕</button>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add('exiting');
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

// ============================================================
// LOADING OVERLAY
// ============================================================
function setLoading(show, text = 'Loading...') {
  const overlay = document.getElementById('loading-overlay');
  const textEl = document.getElementById('loading-text');
  if (overlay) overlay.classList.toggle('hidden', !show);
  if (textEl) textEl.textContent = text;
}

// ============================================================
// MOBILE SIDEBAR TOGGLE
// ============================================================
function toggleSidebar(sidebarId) {
  const sidebar = document.getElementById(sidebarId);
  if (sidebar) sidebar.classList.toggle('open');
}

// Show mobile toggle buttons on small screens
function checkMobile() {
  const isMobile = window.innerWidth <= 900;
  const toggleBtn = document.getElementById('s-menu-toggle');
  if (toggleBtn) toggleBtn.classList.toggle('hidden', !isMobile);
}

window.addEventListener('resize', checkMobile);
checkMobile();

// ============================================================
// INIT
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  // Set today's date as sanction date default
  const sd = document.getElementById('decision-sanction-date');
  if (sd) sd.value = new Date().toISOString().split('T')[0];

  // Restore session or show landing
  if (!restoreSession()) {
    showPage('page-landing');
  }
});
