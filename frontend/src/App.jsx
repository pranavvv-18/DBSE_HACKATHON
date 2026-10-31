import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import { isManager, Loading } from './components/ui'
import AppLayout from './layouts/AppLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import EmployeesPage from './pages/EmployeesPage'
import TasksPage from './pages/TasksPage'
import TaskDetailsPage from './pages/TaskDetailsPage'
import CalendarPage from './pages/CalendarPage'
import MyTasksPage from './pages/MyTasksPage'
import ReportsPage from './pages/ReportsPage'
import SettingsPage from './pages/SettingsPage'

function Protected({ children, managerOnly = false }) {
  const { user, checking } = useAuth()
  if (checking) return <Loading label="Checking your session…" />
  if (!user) return <Navigate to="/login" replace />
  if (managerOnly && !isManager(user)) return <Navigate to="/dashboard" replace />
  return children
}
function RootRedirect() {
  const { user, checking } = useAuth()
  if (checking) return <Loading label="Checking your session…" />
  return <Navigate to={user ? '/dashboard' : '/login'} replace />
}
function LoginRoute() {
  const { user, checking } = useAuth()
  const location = useLocation()
  if (checking) return <Loading label="Checking your session…" />
  return user ? <Navigate to={location.state?.from || '/dashboard'} replace /> : <LoginPage />
}
export default function App() {
  return <Routes>
    <Route path="/" element={<RootRedirect/>}/><Route path="/login" element={<LoginRoute/>}/>
    <Route element={<Protected><AppLayout/></Protected>}>
      <Route path="/dashboard" element={<DashboardPage/>}/>
      <Route path="/employees" element={<Protected managerOnly><EmployeesPage/></Protected>}/>
      <Route path="/tasks" element={<Protected managerOnly><TasksPage/></Protected>}/>
      <Route path="/tasks/:taskId" element={<TaskDetailsPage/>}/>
      <Route path="/calendar" element={<CalendarPage/>}/>
      <Route path="/my-tasks" element={<MyTasksPage/>}/>
      <Route path="/reports" element={<Protected managerOnly><ReportsPage/></Protected>}/>
      <Route path="/settings" element={<SettingsPage/>}/>
    </Route>
    <Route path="*" element={<RootRedirect/>}/>
  </Routes>
}
