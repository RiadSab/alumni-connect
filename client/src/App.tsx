// Route table. RootLayout wraps every page (shared header). Pages under
// RequireAuth need a logged-in user; the rest are public.

import { BrowserRouter, Routes, Route } from "react-router-dom";
import { RootLayout } from "@/routes/RootLayout";
import { RequireAuth } from "@/routes/RequireAuth";
import { HomePage } from "@/pages/HomePage";
import { JobDetailPage } from "@/pages/JobDetailPage";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterCandidatePage } from "@/pages/RegisterCandidatePage";
import { RegisterCompanyPage } from "@/pages/RegisterCompanyPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { MyApplicationsPage } from "@/pages/MyApplicationsPage";
import { ProfilePage } from "@/pages/ProfilePage";
import { ProfileEditPage } from "@/pages/ProfileEditPage";
import { ChangePasswordPage } from "@/pages/ChangePasswordPage";
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
          <Route path="register/candidate" element={<RegisterCandidatePage />} />
          <Route path="register/company" element={<RegisterCompanyPage />} />

          {/* Logged-in only */}
          <Route element={<RequireAuth />}>
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="applications" element={<MyApplicationsPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="profile/edit" element={<ProfileEditPage />} />
            <Route path="settings/password" element={<ChangePasswordPage />} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
