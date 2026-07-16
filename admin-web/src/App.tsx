import { Box, CircularProgress } from '@mui/material';
import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './auth/authState';
import { AdminShell } from './layout/AdminShell';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';

const DashboardPage = lazy(() => import('./pages/DashboardPage').then((module) => ({ default: module.DashboardPage })));
const StudentsPage = lazy(() => import('./pages/StudentsPage').then((module) => ({ default: module.StudentsPage })));
const AcademicsPage = lazy(() => import('./pages/AcademicsPage').then((module) => ({ default: module.AcademicsPage })));
const TimetableAdminPage = lazy(() => import('./pages/TimetableAdminPage').then((module) => ({ default: module.TimetableAdminPage })));
const GradesAdminPage = lazy(() => import('./pages/GradesAdminPage').then((module) => ({ default: module.GradesAdminPage })));
const FormsAdminPage = lazy(() => import('./pages/FormsAdminPage').then((module) => ({ default: module.FormsAdminPage })));
const NotificationsAdminPage = lazy(() => import('./pages/NotificationsAdminPage').then((module) => ({ default: module.NotificationsAdminPage })));
const ActivitiesAdminPage = lazy(() => import('./pages/ActivitiesAdminPage').then((module) => ({ default: module.ActivitiesAdminPage })));
const AuditAdminPage = lazy(() => import('./pages/AuditAdminPage').then((module) => ({ default: module.AuditAdminPage })));
const AiSettingsPage = lazy(() => import('./pages/AiSettingsPage').then((module) => ({ default: module.AiSettingsPage })));

function RouteFallback() {
  return (
    <Box role="status" aria-label="Đang tải nội dung" sx={{ minHeight: 320, display: 'grid', placeItems: 'center' }}>
      <CircularProgress size={30} />
    </Box>
  );
}

function ProtectedRoutes() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === 'checking') {
    return (
      <Box role="status" aria-label="Đang kiểm tra phiên đăng nhập" sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <CircularProgress size={32} />
      </Box>
    );
  }
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return (
    <AdminShell>
      <Suspense fallback={<RouteFallback />}>
        <Routes>
          <Route index element={<DashboardPage />} />
          <Route path="students" element={<StudentsPage />} />
          <Route path="academics" element={<AcademicsPage />} />
          <Route path="timetable" element={<TimetableAdminPage />} />
          <Route path="grades" element={<GradesAdminPage />} />
          <Route path="forms" element={<FormsAdminPage />} />
          <Route path="notifications" element={<NotificationsAdminPage />} />
          <Route path="activities" element={<ActivitiesAdminPage />} />
          <Route path="audit" element={<AuditAdminPage />} />
          <Route path="ai-settings" element={<AiSettingsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
    </AdminShell>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/*" element={<ProtectedRoutes />} />
    </Routes>
  );
}
