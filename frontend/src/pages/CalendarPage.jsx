import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertCircle, Calendar as CalendarIcon, CheckCircle2, ChevronLeft, ChevronRight, Clock, Plus, RefreshCw, Users } from 'lucide-react'
import { employeeApi, messageFromError, taskApi } from '../services/api'
import { useAuth } from '../context/AuthContext'
import { Avatar, EmptyState, ErrorState, formatDate, isManager, Loading, PageHeader, PriorityBadge, SearchBox, SelectField } from '../components/ui'
import TaskFormModal from '../components/TaskFormModal'

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

export default function CalendarPage() {
  const { user } = useAuth()
  const manager = isManager(user)

  const [currentDate, setCurrentDate] = useState(new Date())
  const [tasks, setTasks] = useState([])
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [employeeFilter, setEmployeeFilter] = useState('')
  const [modalOpen, setModalOpen] = useState(false)

  const year = currentDate.getFullYear()
  const month = currentDate.getMonth()

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = { page: 0, size: 100 }
      if (!manager) params.employeeId = user?.id
      const data = await taskApi.list(params)
      setTasks(data.content || [])
    } catch (e) {
      setError(messageFromError(e))
    } finally {
      setLoading(false)
    }
  }, [manager, user?.id])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (manager) {
      employeeApi.list({ page: 0, size: 100 })
        .then(res => setEmployees(res.content || []))
        .catch(() => setEmployees([]))
    }
  }, [manager])

  function prevMonth() {
    setCurrentDate(new Date(year, month - 1, 1))
  }

  function nextMonth() {
    setCurrentDate(new Date(year, month + 1, 1))
  }

  function goToToday() {
    setCurrentDate(new Date())
  }

  // Filter tasks
  const filteredTasks = tasks.filter(t => {
    if (search && !t.title.toLowerCase().includes(search.toLowerCase())) return false
    if (priorityFilter && t.priority !== priorityFilter) return false
    if (statusFilter && t.status !== statusFilter) return false
    if (employeeFilter && String(t.assignedTo?.id) !== String(employeeFilter)) return false
    return true
  })

  // Map tasks by deadline date string "YYYY-MM-DD"
  const tasksByDate = {}
  filteredTasks.forEach(task => {
    if (!task.deadline) return
    const key = task.deadline.slice(0, 10)
    if (!tasksByDate[key]) tasksByDate[key] = []
    tasksByDate[key].push(task)
  })

  // Calendar math
  const firstDayOfMonth = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const daysInPrevMonth = new Date(year, month, 0).getDate()

  const todayStr = new Date().toISOString().slice(0, 10)
  const monthName = currentDate.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })

  // Build grid days
  const calendarDays = []
  // Previous month padding
  for (let i = firstDayOfMonth - 1; i >= 0; i--) {
    const dayNum = daysInPrevMonth - i
    const d = new Date(year, month - 1, dayNum)
    calendarDays.push({ date: d, isCurrentMonth: false, dateStr: d.toISOString().slice(0, 10), dayNum })
  }
  // Current month
  for (let i = 1; i <= daysInMonth; i++) {
    const d = new Date(year, month, i)
    // Avoid timezone offset issues
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    calendarDays.push({ date: d, isCurrentMonth: true, dateStr, dayNum: i })
  }
  // Next month padding
  const totalSlots = Math.ceil(calendarDays.length / 7) * 7
  const remaining = totalSlots - calendarDays.length
  for (let i = 1; i <= remaining; i++) {
    const d = new Date(year, month + 1, i)
    const dateStr = `${month === 11 ? year + 1 : year}-${String((month + 2) % 12 || 12).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    calendarDays.push({ date: d, isCurrentMonth: false, dateStr, dayNum: i })
  }

  // Monthly summary
  const dueThisMonth = filteredTasks.filter(t => t.deadline && t.deadline.startsWith(`${year}-${String(month + 1).padStart(2, '0')}`)).length
  const overdueCount = filteredTasks.filter(t => t.overdue && t.status !== 'COMPLETED').length
  const completedCount = filteredTasks.filter(t => t.status === 'COMPLETED').length

  return (
    <div className="calendar-page">
      <PageHeader
        eyebrow="SCHEDULE & DEADLINES"
        title="Deadline calendar"
        description="Monitor critical target dates, upcoming milestones, and delivery risks."
        actions={
          <>
            <button className="button button-secondary" onClick={load} disabled={loading} title="Refresh calendar">
              <RefreshCw size={15} className={loading ? 'spin' : ''} />
              Refresh
            </button>
            {manager && (
              <button className="button button-primary" onClick={() => setModalOpen(true)}>
                <Plus size={16} />
                New task
              </button>
            )}
          </>
        }
      />

      {/* Summary stats */}
      <section className="metrics-strip calendar-metrics">
        <div className="metric">
          <span className="metric-icon metric-teal"><CalendarIcon size={18} /></span>
          <div>
            <span className="metric-label">Due in {currentDate.toLocaleDateString(undefined, { month: 'short' })}</span>
            <strong>{dueThisMonth}</strong>
          </div>
        </div>
        <div className="metric">
          <span className="metric-icon metric-red"><AlertCircle size={18} /></span>
          <div>
            <span className="metric-label">Overdue</span>
            <strong>{overdueCount}</strong>
          </div>
        </div>
        <div className="metric">
          <span className="metric-icon metric-green"><CheckCircle2 size={18} /></span>
          <div>
            <span className="metric-label">Completed</span>
            <strong>{completedCount}</strong>
          </div>
        </div>
      </section>

      {/* Filter toolbar & month switcher */}
      <div className="calendar-nav-bar">
        <div className="month-navigation">
          <button className="icon-button" onClick={prevMonth} aria-label="Previous month">
            <ChevronLeft size={20} />
          </button>
          <h2>{monthName}</h2>
          <button className="icon-button" onClick={nextMonth} aria-label="Next month">
            <ChevronRight size={20} />
          </button>
          <button className="button button-secondary button-small" onClick={goToToday}>
            Today
          </button>
        </div>

        <div className="calendar-filters">
          <SearchBox value={search} onChange={setSearch} placeholder="Filter deadlines…" />
          <SelectField label="" value={statusFilter} onChange={e => setStatusFilter(e.target.value)} className="compact-select">
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="COMPLETED">Completed</option>
          </SelectField>

          <SelectField label="" value={priorityFilter} onChange={e => setPriorityFilter(e.target.value)} className="compact-select">
            <option value="">All Priorities</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </SelectField>

          {manager && (
            <SelectField label="" value={employeeFilter} onChange={e => setEmployeeFilter(e.target.value)} className="compact-select">
              <option value="">All Assignees</option>
              {employees.map(emp => (
                <option key={emp.id} value={emp.id}>{emp.fullName}</option>
              ))}
            </SelectField>
          )}
        </div>
      </div>

      {loading ? (
        <Loading label="Loading deadline calendar…" />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : (
        <div className="calendar-grid-card section-panel">
          <div className="calendar-grid-header">
            {DAYS.map(day => (
              <div key={day} className="day-header">{day}</div>
            ))}
          </div>

          <div className="calendar-grid-body">
            {calendarDays.map((cell, idx) => {
              const dayTasks = tasksByDate[cell.dateStr] || []
              const isToday = cell.dateStr === todayStr

              return (
                <div
                  key={idx}
                  className={`calendar-cell ${!cell.isCurrentMonth ? 'cell-outside' : ''} ${isToday ? 'cell-today' : ''}`}
                >
                  <div className="cell-top">
                    <span className={`day-number ${isToday ? 'today-pill' : ''}`}>{cell.dayNum}</span>
                    {dayTasks.length > 0 && (
                      <span className="cell-count">{dayTasks.length} {dayTasks.length === 1 ? 'task' : 'tasks'}</span>
                    )}
                  </div>

                  <div className="cell-tasks">
                    {dayTasks.map(task => {
                      const isCompleted = task.status === 'COMPLETED'
                      const isOverdue = task.overdue && !isCompleted

                      return (
                        <Link
                          key={task.id}
                          to={`/tasks/${task.id}`}
                          className={`cal-task-chip status-${task.status.toLowerCase().replace('_', '-')} ${isOverdue ? 'chip-overdue' : ''} ${isCompleted ? 'chip-completed' : ''}`}
                          title={`${task.title} | Assigned to ${task.assignedTo?.fullName || 'User'} | ${task.status.replace('_', ' ')}`}
                        >
                          <span className={`priority-dot priority-${task.priority.toLowerCase()}`} />
                          <span className="cal-task-title">{task.title}</span>
                          {isCompleted ? (
                            <CheckCircle2 size={12} className="cal-chip-icon text-green" />
                          ) : isOverdue ? (
                            <AlertCircle size={12} className="cal-chip-icon text-red" />
                          ) : null}
                        </Link>
                      )
                    })}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {modalOpen && (
        <TaskFormModal
          employees={employees}
          onClose={() => setModalOpen(false)}
          onSaved={() => { setModalOpen(false); load() }}
        />
      )}
    </div>
  )
}

