// Route table. RootLayout wraps every page (shared header). Pages under
// RequireAuth need a logged-in user; the rest are public.

import { BrowserRouter, Routes, Route } from "react-router-dom";
import { RootLayout } from "@/routes/RootLayout";
import { RequireAuth } from "@/routes/RequireAuth";
import { RequireCompany } from "@/routes/RequireCompany";
import { RequireAdmin } from "@/routes/RequireAdmin";
import { RequireCandidate } from "@/routes/RequireCandidate";
import { HomePage } from "@/pages/HomePage";
import { JobDetailPage } from "@/pages/JobDetailPage";
import { LoginPage } from "@/pages/LoginPage";
import { ConfirmEmploymentPage } from "@/pages/ConfirmEmploymentPage";
import { ClaimAccountPage } from "@/pages/ClaimAccountPage";
import { ForgotPasswordPage } from "@/pages/ForgotPasswordPage";
import { RegisterCandidatePage } from "@/pages/RegisterCandidatePage";
import { RegisterCompanyPage } from "@/pages/RegisterCompanyPage";
import { RegisterCompanyMemberPage } from "@/pages/RegisterCompanyMemberPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { MyApplicationsPage } from "@/pages/MyApplicationsPage";
import { ApplicationDetailPage } from "@/pages/ApplicationDetailPage";
import { EmploymentPage } from "@/pages/EmploymentPage";
import { SavedJobsPage } from "@/pages/SavedJobsPage";
import { ProfilePage } from "@/pages/ProfilePage";
import { ProfileEditPage } from "@/pages/ProfileEditPage";
import { ChangePasswordPage } from "@/pages/ChangePasswordPage";
import { CompanyJobOffersPage } from "@/pages/CompanyJobOffersPage";
import { CompanyJobOfferCreatePage } from "@/pages/CompanyJobOfferCreatePage";
import { CompanyJobOfferEditPage } from "@/pages/CompanyJobOfferEditPage";
import { CompanyApplicantsPage } from "@/pages/CompanyApplicantsPage";
import { CompanyApplicationReviewPage } from "@/pages/CompanyApplicationReviewPage";
import { CompanyTeamPage } from "@/pages/CompanyTeamPage";
import { AdminPendingPage } from "@/pages/AdminPendingPage";
import { AdminUsersPage } from "@/pages/AdminUsersPage";
import { AdminAlumniPage } from "@/pages/AdminAlumniPage";
import { AdminReportsPage } from "@/pages/AdminReportsPage";
import { AdminCompaniesPage } from "@/pages/AdminCompaniesPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<RootLayout />}>
          {/* Public */}
          <Route index element={<HomePage />} />
          <Route path="jobs/:id" element={<JobDetailPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="forgot-password" element={<ForgotPasswordPage />} />
          <Route path="claim/:token" element={<ClaimAccountPage />} />
          <Route path="employment/confirm/:token" element={<ConfirmEmploymentPage />} />
          <Route path="register/candidate" element={<RegisterCandidatePage />} />
          <Route path="register/company" element={<RegisterCompanyPage />} />
          <Route path="register/company-member" element={<RegisterCompanyMemberPage />} />

          {/* Logged-in only */}
          <Route element={<RequireAuth />}>
            {/* Shared by every role */}
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="settings/password" element={<ChangePasswordPage />} />
            {/* Candidate + company; the page dispatches by type (admins sent home) */}
            <Route path="profile" element={<ProfilePage />} />
            <Route path="profile/edit" element={<ProfileEditPage />} />

            {/* Candidates only */}
            <Route element={<RequireCandidate />}>
              <Route path="applications" element={<MyApplicationsPage />} />
              <Route path="applications/:id" element={<ApplicationDetailPage />} />
              <Route path="saved" element={<SavedJobsPage />} />
              <Route path="profile/employment" element={<EmploymentPage />} />
            </Route>

            {/* Company users only */}
            <Route element={<RequireCompany />}>
              <Route path="company/jobs" element={<CompanyJobOffersPage />} />
              <Route path="company/jobs/new" element={<CompanyJobOfferCreatePage />} />
              <Route path="company/jobs/:id/edit" element={<CompanyJobOfferEditPage />} />
              <Route path="company/jobs/:id/applications" element={<CompanyApplicantsPage />} />
              <Route path="company/applications/:appId" element={<CompanyApplicationReviewPage />} />
              <Route path="company/team" element={<CompanyTeamPage />} />
            </Route>

            {/* Administrators only */}
            <Route element={<RequireAdmin />}>
              <Route path="admin/pending" element={<AdminPendingPage />} />
              <Route path="admin/users" element={<AdminUsersPage />} />
              <Route path="admin/companies" element={<AdminCompaniesPage />} />
              <Route path="admin/alumni" element={<AdminAlumniPage />} />
              <Route path="admin/reports" element={<AdminReportsPage />} />
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
