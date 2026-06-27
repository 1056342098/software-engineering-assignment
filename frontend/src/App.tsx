import { Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { RequireAuth } from "./components/RequireAuth";
import { ApprovalsPage } from "./pages/ApprovalsPage";
import { ApprovalApplyPage } from "./pages/ApprovalApplyPage";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { PendingPage } from "./pages/PendingPage";
import { PolicyPage } from "./pages/PolicyPage";
import { ProfilePage } from "./pages/ProfilePage";
import { Navigate } from "react-router-dom";
import { StudentDetailPage } from "./pages/StudentDetailPage";
import { StudentsPage } from "./pages/StudentsPage";
import { TimelineAdminPage } from "./pages/TimelineAdminPage";
import { QaTestPage } from "./pages/QaTestPage";
import { ErrorModal } from "./components/ErrorModal";
import { NotificationsPage } from "./pages/NotificationsPage";

export default function App() {
  return (
    <>
      <ErrorModal />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<RequireAuth />}>
          <Route element={<Layout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/qa" element={<Navigate to="/policy" replace />} />
            <Route path="/policy" element={<PolicyPage />} />
            <Route path="/approvals" element={<ApprovalsPage />} />
            <Route path="/approvals/new" element={<ApprovalApplyPage />} />
            <Route path="/pending" element={<PendingPage />} />
            <Route path="/students" element={<StudentsPage />} />
            <Route path="/students/:studentId" element={<StudentDetailPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/timeline-admin" element={<TimelineAdminPage />} />
            <Route path="/selftest" element={<QaTestPage />} />
          </Route>
        </Route>
      </Routes>
    </>
  );
}
