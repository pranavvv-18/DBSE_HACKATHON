import { useCallback, useEffect, useState } from 'react'
import { ArrowDownRight, ArrowRight, ArrowUpRight, Clock3, ListTodo, RefreshCw, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import { dashboardApi, messageFromError, taskApi } from '../services/api'
import { useAuth } from '../context/AuthContext'
import { Avatar, EmptyState, ErrorState, formatDate, isManager, Loading, PageHeader, PriorityBadge, StatusBadge } from '../components/ui'

const metricConfig = [
  ['totalTasks', 'Total tasks', ListTodo, 'metric-ink'], ['pendingTasks', 'Pending', Clock3, 'metric-amber'], ['inProgressTasks', 'In progress', ArrowUpRight, 'metric-blue'], ['completedTasks', 'Completed', ArrowDownRight, 'metric-green'], ['overdueTasks', 'Overdue', Clock3, 'metric-red']
]
function normalizeRows(payload) { return Array.isArray(payload) ? payload : payload?.content || [] }
export default function DashboardPage() {
  const { user } = useAuth(), manager = isManager(user)
  const [data, setData] = useState({ stats: null, recent: [], progress: [], upcoming: [], highPriority: [] }), [loading, setLoading] = useState(true), [error, setError] = useState('')
  const load = useCallback(async () => { setLoading(true); setError(''); try {
    const stats = await dashboardApi.stats()
    const recent = await dashboardApi.recent({ page: 0, size: 6, sortBy: 'createdAt', sortDirection: 'DESC' })
    const progress = manager ? await dashboardApi.progress() : []
    const [deadlineTasks, priorityTasks] = manager ? await Promise.all([
      taskApi.list({ sortBy: 'deadline', sortDirection: 'ASC', page: 0, size: 12 }),
      taskApi.list({ priority: 'HIGH', sortBy: 'deadline', sortDirection: 'ASC', page: 0, size: 5 })
    ]) : [[], []]
    const upcoming = normalizeRows(deadlineTasks).filter(task => !task.overdue && String(task.status).toUpperCase() !== 'COMPLETED').slice(0, 5)
    setData({ stats, recent: normalizeRows(recent), progress: normalizeRows(progress), upcoming, highPriority: normalizeRows(priorityTasks).slice(0, 5) })
  } catch (e) { setError(messageFromError(e)) } finally { setLoading(false) } }, [manager])
  useEffect(() => { load() }, [load])
  const stats = data.stats || {}
  const welcome = user?.fullName?.split(' ')[0] || 'there'
  return <><PageHeader eyebrow={manager ? 'TEAM OVERVIEW' : 'YOUR WORKSPACE'} title={`Good ${new Date().getHours() < 12 ? 'morning' : new Date().getHours() < 18 ? 'afternoon' : 'evening'}, ${welcome}`} description={manager ? 'Here’s the work in motion across your team.' : 'A clear view of your assignments and what’s coming up.'} actions={<button className="button button-secondary" onClick={load} disabled={loading}><RefreshCw size={15} className={loading ? 'spin' : ''}/>Refresh</button>}/>
    {loading ? <Loading label="Loading workspace data…"/> : error ? <ErrorState message={error} onRetry={load}/> : <>
      <section className="metrics-strip" aria-label="Task statistics">{metricConfig.map(([key, label, Icon, color]) => <div className="metric" key={key}><span className={`metric-icon ${color}`}><Icon size={17}/></span><div><span className="metric-label">{label}</span><strong>{stats[key] ?? '—'}</strong></div></div>)}{manager && <div className="metric metric-employees"><span className="metric-icon metric-teal"><Users size={17}/></span><div><span className="metric-label">Team members</span><strong>{stats.totalEmployees ?? '—'}</strong></div></div>}</section>
      {manager ? <div className="dashboard-grid"><section className="section-panel status-panel"><div className="section-head"><div><span className="eyebrow">AT A GLANCE</span><h2>Task status</h2></div><span className="section-note">Live totals</span></div><StatusDistribution data={stats.statusDistribution || []}/></section><section className="section-panel priority-panel"><div className="section-head"><div><span className="eyebrow">TEAM PULSE</span><h2>Employee progress</h2></div><Link className="text-link" to="/reports">View report <ArrowRight size={14}/></Link></div><ProgressList rows={data.progress.slice(0, 5)}/></section><section className="section-panel queue-panel"><div className="queue-column"><div className="section-head"><div><span className="eyebrow">COMING UP</span><h2>Upcoming deadlines</h2></div><Link className="text-link" to="/tasks">View queue <ArrowRight size={14}/></Link></div><DeadlineList rows={data.upcoming} empty="No upcoming deadlines."/></div><div className="queue-column"><div className="section-head"><div><span className="eyebrow">NEEDS ATTENTION</span><h2>High priority</h2></div><Link className="text-link" to="/tasks?priority=HIGH">View all <ArrowRight size={14}/></Link></div><DeadlineList rows={data.highPriority} empty="No high-priority tasks."/></div></section><section className="section-panel recent-panel"><div className="section-head"><div><span className="eyebrow">LATEST ACTIVITY</span><h2>Recent tasks</h2></div><Link className="text-link" to="/tasks">All tasks <ArrowRight size={14}/></Link></div><TaskTable rows={data.recent}/></section></div> : <div className="dashboard-grid employee-dash-grid"><section className="section-panel personal-panel"><div className="section-head"><div><span className="eyebrow">WORK IN MOTION</span><h2>Your task status</h2></div><Link className="text-link" to="/my-tasks">Open my tasks <ArrowRight size={14}/></Link></div><StatusDistribution data={stats.statusDistribution || []}/></section><section className="section-panel recent-panel"><div className="section-head"><div><span className="eyebrow">RECENT ASSIGNMENTS</span><h2>Recently updated</h2></div><Link className="text-link" to="/my-tasks">My tasks <ArrowRight size={14}/></Link></div><TaskTable rows={data.recent}/></section></div>}
    </>}
  </>
}
function StatusDistribution({ data }) {
  const normalized = ['PENDING','IN_PROGRESS','COMPLETED'].map(status => ({ status, count: Number(data.find(x => String(x.status).toUpperCase() === status)?.count || 0) }))
  const total = normalized.reduce((sum, row) => sum + row.count, 0)
  return <div className="distribution">{normalized.map(row => <div className="distribution-row" key={row.status}><div className="distribution-title"><StatusBadge status={row.status}/><span>{row.count}</span></div><div className="bar-track"><div className={`bar-fill bar-${row.status.toLowerCase()}`} style={{ width: `${total ? row.count / total * 100 : 0}%` }}/></div></div>)}{total === 0 && <p className="muted-small">No task status data has been recorded yet.</p>}</div>
}
function ProgressList({ rows }) {
  if (!rows.length) return <EmptyState title="No employee progress yet">Progress will appear once assigned work is available.</EmptyState>
  return <div className="progress-list">{rows.map(row => <div className="progress-row" key={row.employeeId}><Avatar name={row.fullName}/><div className="progress-main"><div className="progress-name"><strong>{row.fullName}</strong><span>{row.completedTasks || 0}/{row.totalTasks || 0}</span></div><div className="bar-track"><div className="bar-fill bar-completed" style={{ width: `${Math.max(0, Math.min(100, Number(row.completionRate || 0)))}%` }}/></div><small>{row.department || 'Team member'}</small></div><span className="completion-rate">{Math.round(row.completionRate || 0)}%</span></div>)}</div>
}
function DeadlineList({ rows, empty }) {
  if (!rows.length) return <p className="queue-empty">{empty}</p>
  return <div className="deadline-list">{rows.map(task => <Link className="deadline-row" to={`/tasks/${task.id}`} key={task.id}><span className="deadline-marker"/><span className="deadline-copy"><strong>{task.title}</strong><small>{task.assignedTo?.fullName || 'Unassigned'} · {formatDate(task.deadline)}</small></span><PriorityBadge priority={task.priority}/></Link>)}</div>
}
function TaskTable({ rows }) {
  if (!rows.length) return <EmptyState title="No recent tasks">New assignments and updates will appear here.</EmptyState>
  return <div className="table-wrap"><table className="data-table compact-table"><thead><tr><th>Task</th><th>Owner</th><th>Status</th><th>Deadline</th></tr></thead><tbody>{rows.map(task => <tr key={task.id}><td><Link className="task-link" to={`/tasks/${task.id}`}><strong>{task.title}</strong><small>#{task.id}</small></Link></td><td><span className="table-person"><Avatar name={task.assignedTo?.fullName || userDisplay(task)}/>{task.assignedTo?.fullName || '—'}</span></td><td><StatusBadge status={task.status}/></td><td><span className={task.overdue ? 'date-overdue' : ''}>{formatDate(task.deadline)}</span></td></tr>)}</tbody></table><div className="mobile-task-list">{rows.map(task => <Link key={task.id} className="mobile-task-row" to={`/tasks/${task.id}`}><span><strong>{task.title}</strong><small>{formatDate(task.deadline)} · {task.assignedTo?.fullName || 'Assigned to you'}</small></span><StatusBadge status={task.status}/></Link>)}</div></div>
}
function userDisplay(task) { return task.assignedTo?.fullName || 'Assigned' }
