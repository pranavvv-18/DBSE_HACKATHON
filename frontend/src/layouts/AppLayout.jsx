import { useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Activity, BarChart3, CalendarDays, CheckSquare, ChevronDown, CircleUserRound, ClipboardList, LayoutDashboard, LogOut, Menu, Settings, Users } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Avatar, isManager } from '../components/ui'

const navTitle = {
  '/dashboard': 'Overview',
  '/employees': 'Employees',
  '/tasks': 'All tasks',
  '/calendar': 'Deadline calendar',
  '/my-tasks': 'My tasks',
  '/reports': 'Reports & analytics',
  '/settings': 'Profile & settings'
}

export default function AppLayout() {
  const { user, logout } = useAuth(), toast = useToast(), navigate = useNavigate(), location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false), [profileOpen, setProfileOpen] = useState(false)
  const manager = isManager(user)
  const links = manager ? [
    { to: '/dashboard', label: 'Overview', icon: LayoutDashboard },
    { to: '/employees', label: 'Employees', icon: Users },
    { to: '/tasks', label: 'All tasks', icon: ClipboardList },
    { to: '/calendar', label: 'Deadline calendar', icon: CalendarDays },
    { to: '/reports', label: 'Reports', icon: BarChart3 }
  ] : [
    { to: '/dashboard', label: 'Overview', icon: LayoutDashboard },
    { to: '/my-tasks', label: 'My tasks', icon: CheckSquare },
    { to: '/calendar', label: 'Deadline calendar', icon: CalendarDays }
  ]
  async function signOut() { try { await logout(); navigate('/login', { replace: true }) } catch { toast('Unable to sign out. Please try again.', 'error') } }

  const toggleSidebar = () => setSidebarOpen(prev => !prev)
  const closeSidebar = () => setSidebarOpen(false)

  return <div className={`app-shell ${sidebarOpen ? 'sidebar-expanded' : 'sidebar-collapsed'}`}>
    {sidebarOpen && <button className="mobile-scrim" aria-label="Close navigation" onClick={closeSidebar}/>}
    <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
      <div className="brand">
        <span className="brand-mark"><i/><i/><i/></span>
        <span>Task<span className="brand-light">Flow</span></span>
        <button className="sidebar-toggle-btn icon-button" aria-label="Toggle navigation" onClick={toggleSidebar}>
          <Menu size={20}/>
        </button>
      </div>
      <div className="workspace-label">WORKSPACE</div>
      <nav className="side-nav" aria-label="Main navigation">{links.map(item => <NavLink key={item.to} to={item.to} onClick={closeSidebar} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><item.icon size={19} strokeWidth={1.8}/><span>{item.label}</span></NavLink>)}</nav>
      <div className="sidebar-bottom"><div className="team-note"><Activity size={16}/><span>{manager ? 'Team workspace' : 'Your workspace'}</span></div><button className={`nav-link ${location.pathname === '/settings' ? 'active' : ''}`} onClick={() => { navigate('/settings'); closeSidebar() }}><Settings size={19}/><span>Settings</span></button><button className="account-card" onClick={() => setProfileOpen(value => !value)} aria-expanded={profileOpen}><Avatar name={user?.fullName || 'User'}/><span className="account-info"><strong>{user?.fullName || 'Team member'}</strong><small>{user?.designation || (manager ? 'Manager' : 'Employee')}</small></span><ChevronDown size={16}/></button>{profileOpen && <div className="account-menu"><button onClick={() => { setProfileOpen(false); navigate('/settings') }}><CircleUserRound size={17}/>Profile & settings</button><button onClick={signOut}><LogOut size={17}/>Sign out</button></div>}</div>
    </aside>
    <div className="main-frame"><header className="topbar"><button className="mobile-menu icon-button" aria-label="Toggle navigation" onClick={toggleSidebar}><Menu size={22}/></button><div className="crumb"><span>TaskFlow</span><span className="crumb-divider">/</span><strong>{navTitle[location.pathname] || 'Task details'}</strong></div><div className="topbar-right"><span className="top-date">{new Intl.DateTimeFormat(undefined, { weekday: 'short', month: 'short', day: 'numeric' }).format(new Date())}</span><button className="top-profile" onClick={() => navigate('/settings')} aria-label="Open profile settings"><Avatar name={user?.fullName || 'User'} size="avatar-small"/><span>{user?.fullName?.split(' ')[0] || 'Profile'}</span></button></div></header><main className="main-content"><div key={location.pathname} className="page-transition-wrap"><Outlet/></div></main><footer className="app-footer"><span>TaskFlow</span><span>Work, clearly assigned.</span></footer></div>
  </div>
}
