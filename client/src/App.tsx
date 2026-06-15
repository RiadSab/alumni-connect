// Route table. RootLayout wraps every page (shared header). Pages under
// RequireAuth need a logged-in user; the rest are public.

import { BrowserRouter, Routes, Route } from "react-router-dom";
import { RootLayout } from "@/routes/RootLayout";
import { RequireAuth } from "@/routes/RequireAuth";
import { HomePage } from "@/pages/HomePage";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterCandidatePage } from "@/pages/RegisterCandidatePage";
import { RegisterCompanyPage } from "@/pages/RegisterCompanyPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<RootLayout />}>
          {/* Public */}
          <Route index element={<HomePage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register/candidate" element={<RegisterCandidatePage />} />
          <Route path="register/company" element={<RegisterCompanyPage />} />

          {/* Logged-in only */}
          <Route element={<RequireAuth />}>
            <Route path="dashboard" element={<DashboardPage />} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
