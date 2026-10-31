import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { ArrowDownUp, Download, ExternalLink, Plus, SlidersHorizontal } from 'lucide-react'
import { employeeApi, messageFromError, reportsApi, taskApi } from '../services/api'
import { useToast } from '../context/ToastContext'
import { Avatar, EmptyState, ErrorState, Loading, PageHeader, Pagination, PriorityBadge, SearchBox, SelectField, StatusBadge, formatDate } from '../components/ui'
import TaskFormModal from '../components/TaskFormModal'

const size = 10
export default function TasksPage() {
  const [searchParams] = useSearchParams(), location = useLocation()
  const [filters, setFilters] = useState({ search: '', status: '', priority: searchParams.get('priority') || '', employeeId: searchParams.get('employeeId') || '', overdue: '', sortBy: 'deadline', sortDirection: 'ASC' })
  const [page, setPage] = useState(0), [result, setResult] = useState(null), [employees, setEmployees] = useState([]), [loading, setLoading] = useState(true), [error, setError] = useState(''), [dialog, setDialog] = useState(null), [exporting, setExporting] = useState(false)
  const toast = useToast()
  useEffect(() => { employeeApi.list({ page: 0, size: 100 }).then(data => setEmployees(data.content || [])).catch(() => setEmployees([])) }, [])
  const load = useCallback(async () => { setLoading(true); setError(''); try { setResult(await taskApi.list({ ...filters, page, size, ...(filters.overdue ? { overdue: filters.overdue } : { overdue: undefined }) })) } catch (e) { setError(messageFromError(e)) } finally { setLoading(false) } }, [filters, page])
  useEffect(() => { const id = setTimeout(load, filters.search ? 250 : 0); return () => clearTimeout(id) }, [load, filters.search])
  function change(key, value) { setFilters(previous => ({ ...previous, [key]: value })); setPage(0) }

  async function handleDownloadCsv() {
    setExporting(true)
    try {
      const blob = await reportsApi.downloadCsv()
      const url = window.URL.createObjectURL(new Blob([blob], { type: 'text/csv;charset=utf-8;' }))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', 'taskflow-tasks-report.csv')
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      toast('Tasks exported to CSV successfully.')
    } catch (e) {
      toast(messageFromError(e), 'error')
    } finally {
      setExporting(false)
    }
  }

  const rows = result?.content || []
  const employeeSelected = new URLSearchParams(location.search).get('employeeId')
  return <><PageHeader eyebrow="WORK QUEUE" title="All tasks" description="Search and organize assignments across your team." actions={<><button className="button button-secondary" onClick={handleDownloadCsv} disabled={exporting} title="Export all tasks to CSV"><Download size={15}/>{exporting ? 'Exporting…' : 'Export CSV'}</button><button className="button button-primary" onClick={() => setDialog({ kind: 'create' })}><Plus size={17}/>Create task</button></>}/>
    <section className="section-panel task-list-panel"><div className="task-filter-top"><SearchBox value={filters.search} onChange={v => change('search', v)} placeholder="Search task titles…" ariaLabel="Search task titles"/><span className="result-count">{result ? `${result.totalElements || 0} tasks` : '—'}</span></div><div className="filters-row"><span className="filter-label"><SlidersHorizontal size={15}/>Filters</span><SelectField label="Status" aria-label="Filter by status" value={filters.status} onChange={e => change('status', e.target.value)}><option value="">All statuses</option><option value="PENDING">Pending</option><option value="IN_PROGRESS">In progress</option><option value="COMPLETED">Completed</option></SelectField><SelectField label="Priority" aria-label="Filter by priority" value={filters.priority} onChange={e => change('priority', e.target.value)}><option value="">All priorities</option><option value="HIGH">High</option><option value="MEDIUM">Medium</option><option value="LOW">Low</option></SelectField><SelectField label="Employee" aria-label="Filter by employee" value={filters.employeeId} onChange={e => change('employeeId', e.target.value)}><option value="">All employees</option>{employees.map(employee => <option key={employee.id} value={employee.id}>{employee.fullName}</option>)}</SelectField><SelectField label="Deadline" aria-label="Filter overdue tasks" value={filters.overdue} onChange={e => change('overdue', e.target.value)}><option value="">Any deadline</option><option value="true">Overdue only</option><option value="false">Not overdue</option></SelectField><button className="sort-control" onClick={() => { const by = filters.sortBy === 'deadline' ? 'createdAt' : 'deadline'; setFilters(prev => ({ ...prev, sortBy: by, sortDirection: by === 'deadline' ? 'ASC' : 'DESC' })); setPage(0) }}><ArrowDownUp size={15}/>Sort: {filters.sortBy === 'deadline' ? 'Deadline' : 'Created'} {filters.sortDirection === 'ASC' ? '↑' : '↓'}</button></div>
      {employeeSelected && <div className="filter-context">Showing assignments for the selected employee. <button onClick={() => { change('employeeId', ''); window.history.replaceState({}, '', '/tasks') }}>Clear employee filter</button></div>}
      {loading ? <Loading label="Loading tasks…"/> : error ? <ErrorState message={error} onRetry={load}/> : rows.length === 0 ? <EmptyState title={filters.search || filters.status || filters.priority || filters.employeeId || filters.overdue ? 'No tasks match these filters' : 'No tasks yet'}>{filters.search || filters.status || filters.priority || filters.employeeId || filters.overdue ? 'Change or clear a filter to see more assignments.' : 'Create a task to start coordinating work across your team.'}</EmptyState> : <><div className="table-wrap"><table className="data-table task-data-table"><thead><tr><th>Task</th><th>Assignee</th><th>Priority</th><th>Status</th><th>Deadline</th><th>Created</th><th><span className="sr-only">Open details</span></th></tr></thead><tbody>{rows.map(task => <tr key={task.id}><td><Link className="task-link" to={`/tasks/${task.id}`}><strong>{task.title}</strong><small>#{task.id}</small></Link></td><td><span className="table-person"><Avatar name={task.assignedTo?.fullName || 'Unassigned'}/>{task.assignedTo?.fullName || '—'}</span></td><td><PriorityBadge priority={task.priority}/></td><td><StatusBadge status={task.status}/></td><td><span className={task.overdue ? 'date-overdue' : ''}>{formatDate(task.deadline)}</span>{task.overdue && <small className="overdue-label">Overdue</small>}</td><td>{formatDate(task.createdAt)}</td><td><Link className="icon-button" aria-label={`View ${task.title}`} to={`/tasks/${task.id}`}><ExternalLink size={16}/></Link></td></tr>)}</tbody></table></div><Pagination page={result.page || 0} totalPages={result.totalPages || 1} onChange={setPage}/></>}
    </section>{dialog && <TaskFormModal employees={employees} onClose={() => setDialog(null)} onSaved={() => { setDialog(null); load() }}/>}</>
}
