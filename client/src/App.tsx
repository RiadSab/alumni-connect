// Route table. RootLayout wraps every page (shared header, and the Suspense boundary the lazy
// pages below resolve into). Pages under RequireAuth need a logged-in user; the rest are public.

import { lazy, type ComponentType } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { RootLayout } from "@/routes/RootLayout";
import { RequireAuth } from "@/routes/RequireAuth";
import { RequireCompany } from "@/routes/RequireCompany";
import { RequireAdmin } from "@/routes/RequireAdmin";
import { RequireCandidate } from "@/routes/RequireCandidate";

// Eager: the first screen anyone sees, and the two pages that must work even when a chunk doesn't.
import { HomePage } from "@/pages/HomePage";
import { LoginPage } from "@/pages/LoginPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

const RELOADED = "chunk-reloaded";

// Pages are named exports and lazy() wants a default. A failed import almost always means a deploy
// replaced the hashed chunks under an open tab, so reload once to pick up the new index.
function lazyPage<M extends Record<string, ComponentType>, K extends keyof M>(
  load: () => Promise<M>,
  name: K,
) {
  return lazy(() =>
    load()
      .then((module) => {
        sessionStorage.removeItem(RELOADED);
        return { default: module[name] };
      })
      .catch((error: unknown) => {
        if (sessionStorage.getItem(RELOADED) !== null) throw error;
        sessionStorage.setItem(RELOADED, "1");
        window.location.reload();
        // The reload takes over; never resolving keeps the fallback on screen until it does.
        return new Promise<never>(() => {});
      }),
  );
}

const JobDetailPage = lazyPage(() => import("@/pages/JobDetailPage"), "JobDetailPage");
const ForgotPasswordPage = lazyPage(() => import("@/pages/ForgotPasswordPage"), "ForgotPasswordPage");
const ClaimAccountPage = lazyPage(() => import("@/pages/ClaimAccountPage"), "ClaimAccountPage");
const ConfirmEmploymentPage = lazyPage(() => import("@/pages/ConfirmEmploymentPage"), "ConfirmEmploymentPage");
const ConfirmSessionPage = lazyPage(() => import("@/pages/ConfirmSessionPage"), "ConfirmSessionPage");
const RegisterCandidatePage = lazyPage(() => import("@/pages/RegisterCandidatePage"), "RegisterCandidatePage");
const RegisterCompanyPage = lazyPage(() => import("@/pages/RegisterCompanyPage"), "RegisterCompanyPage");
const RegisterCompanyMemberPage = lazyPage(() => import("@/pages/RegisterCompanyMemberPage"), "RegisterCompanyMemberPage");

const DashboardPage = lazyPage(() => import("@/pages/DashboardPage"), "DashboardPage");
const ChangePasswordPage = lazyPage(() => import("@/pages/ChangePasswordPage"), "ChangePasswordPage");
const NotificationsPage = lazyPage(() => import("@/pages/NotificationsPage"), "NotificationsPage");
const ProfilePage = lazyPage(() => import("@/pages/ProfilePage"), "ProfilePage");
const ProfileEditPage = lazyPage(() => import("@/pages/ProfileEditPage"), "ProfileEditPage");

const MyApplicationsPage = lazyPage(() => import("@/pages/MyApplicationsPage"), "MyApplicationsPage");
const ApplicationDetailPage = lazyPage(() => import("@/pages/ApplicationDetailPage"), "ApplicationDetailPage");
const SavedJobsPage = lazyPage(() => import("@/pages/SavedJobsPage"), "SavedJobsPage");
const EmploymentPage = lazyPage(() => import("@/pages/EmploymentPage"), "EmploymentPage");
const MentoringPage = lazyPage(() => import("@/pages/MentoringPage"), "MentoringPage");
const MentorSlotsPage = lazyPage(() => import("@/pages/MentorSlotsPage"), "MentorSlotsPage");

const CompanyJobOffersPage = lazyPage(() => import("@/pages/CompanyJobOffersPage"), "CompanyJobOffersPage");
const CompanyJobOfferCreatePage = lazyPage(() => import("@/pages/CompanyJobOfferCreatePage"), "CompanyJobOfferCreatePage");
const CompanyJobOfferEditPage = lazyPage(() => import("@/pages/CompanyJobOfferEditPage"), "CompanyJobOfferEditPage");
const CompanyApplicantsPage = lazyPage(() => import("@/pages/CompanyApplicantsPage"), "CompanyApplicantsPage");
const CompanyApplicationReviewPage = lazyPage(() => import("@/pages/CompanyApplicationReviewPage"), "CompanyApplicationReviewPage");
const CompanyTeamPage = lazyPage(() => import("@/pages/CompanyTeamPage"), "CompanyTeamPage");

const AdminPendingPage = lazyPage(() => import("@/pages/AdminPendingPage"), "AdminPendingPage");
const AdminUsersPage = lazyPage(() => import("@/pages/AdminUsersPage"), "AdminUsersPage");
const AdminCompaniesPage = lazyPage(() => import("@/pages/AdminCompaniesPage"), "AdminCompaniesPage");
const AdminAlumniPage = lazyPage(() => import("@/pages/AdminAlumniPage"), "AdminAlumniPage");
const AdminReportsPage = lazyPage(() => import("@/pages/AdminReportsPage"), "AdminReportsPage");

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
          <Route path="mentoring/confirm/:token" element={<ConfirmSessionPage />} />
          <Route path="register/candidate" element={<RegisterCandidatePage />} />
          <Route path="register/company" element={<RegisterCompanyPage />} />
          <Route path="register/company-member" element={<RegisterCompanyMemberPage />} />

          {/* Logged-in only */}
          <Route element={<RequireAuth />}>
            {/* Shared by every role */}
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="settings/password" element={<ChangePasswordPage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            {/* Candidate + company; the page dispatches by type (admins sent home) */}
            <Route path="profile" element={<ProfilePage />} />
            <Route path="profile/edit" element={<ProfileEditPage />} />

            {/* Candidates only */}
            <Route element={<RequireCandidate />}>
              <Route path="applications" element={<MyApplicationsPage />} />
              <Route path="applications/:id" element={<ApplicationDetailPage />} />
              <Route path="saved" element={<SavedJobsPage />} />
              <Route path="profile/employment" element={<EmploymentPage />} />
              <Route path="mentoring" element={<MentoringPage />} />
              <Route path="mentoring/slots" element={<MentorSlotsPage />} />
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
