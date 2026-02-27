import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './stores/auth';
import Login from './pages/Login';
import Layout from './pages/Layout';
import PendingTasks from './pages/PendingTasks';
import MyInstances from './pages/MyInstances';
import StartLeave from './pages/StartLeave';
import StartExpense from './pages/StartExpense';
import NoticeList from './pages/NoticeList';
import NoticeDetail from './pages/NoticeDetail';
import Notifications from './pages/Notifications';
import InstanceDetail from './pages/InstanceDetail';
import OrgManage from './pages/OrgManage';
import ReportStats from './pages/ReportStats';
import ConnectionDemo from './pages/ConnectionDemo';

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { token, loading } = useAuth();
  if (loading) return <div style={{ padding: 48, textAlign: 'center' }}>加载中...</div>;
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/tasks/pending" replace />} />
        <Route path="tasks/pending" element={<PendingTasks />} />
        <Route path="instances/my" element={<MyInstances />} />
        <Route path="instances/:id" element={<InstanceDetail />} />
        <Route path="notices" element={<NoticeList />} />
        <Route path="notices/:id" element={<NoticeDetail />} />
        <Route path="notifications" element={<Notifications />} />
        <Route path="org" element={<OrgManage />} />
        <Route path="stats" element={<ReportStats />} />
        <Route path="connection-demo" element={<ConnectionDemo />} />
        <Route path="leave/start" element={<StartLeave />} />
        <Route path="expense/start" element={<StartExpense />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
