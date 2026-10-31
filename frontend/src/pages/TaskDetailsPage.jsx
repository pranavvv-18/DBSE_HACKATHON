import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, CalendarDays, CheckCircle2, Circle, Clock, Clock3, History, MessageSquare, Pencil, Tag, Trash2, User, UserCheck } from 'lucide-react'
import { employeeApi, messageFromError, taskApi } from '../services/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Avatar, EmptyState, ErrorState, formatDate, isManager, Loading, PageHeader, PriorityBadge, SelectField, StatusBadge } from '../components/ui'
import TaskFormModal from '../components/TaskFormModal'

function formatDateTime(val) {
  if (!val) return '—'
  const d = new Date(val)
  return Number.isNaN(d.getTime()) ? val : new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  }).format(d)
}

function getActivityIcon(action) {
  const a = String(action || '').toLowerCase()
  if (a.includes('complete')) return <CheckCircle2 size={16} className="text-green" />
  if (a.includes('assign')) return <UserCheck size={16} className="text-teal" />
  if (a.includes('status')) return <Clock size={16} className="text-blue" />
  if (a.includes('priority')) return <Tag size={16} className="text-amber" />
  if (a.includes('create')) return <Circle size={16} className="text-teal" />
  return <History size={16} className="text-slate" />
}

export default function TaskDetailsPage() {
  const { taskId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const manager = isManager(user)
  const toast = useToast()

  const [task, setTask] = useState(null)
  const [activities, setActivities] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [dialog, setDialog] = useState(false)
  const [employees, setEmployees] = useState([])
  const [updating, setUpdating] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [t, act] = await Promise.all([
        taskApi.get(taskId),
        taskApi.activities(taskId).catch(() => [])
      ])
      setTask(t)
      setActivities(act || [])
    } catch (e) {
      setError(messageFromError(e))
    } finally {
      setLoading(false)
    }
  }, [taskId])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (manager) {
      employeeApi.list({ page: 0, size: 100 })
        .then(data => setEmployees(data.content || []))
        .catch(() => setEmployees([]))
    }
  }, [manager])

  async function updateStatus(status) {
    setUpdating(true)
    try {
      await taskApi.updateStatus(task.id, status)
      toast(status === 'COMPLETED' ? 'Task marked complete.' : 'Task status updated.')
      await load()
    } catch (e) {
      toast(messageFromError(e), 'error')
    } finally {
      setUpdating(false)
    }
  }

  async function remove() {
    if (!window.confirm(`Delete “${task.title}”? This action cannot be undone.`)) return
    try {
      await taskApi.remove(task.id)
      toast('Task deleted.')
      navigate('/tasks', { replace: true })
    } catch (e) {
      toast(messageFromError(e), 'error')
    }
  }

  return loading ? (
    <Loading label="Loading task details…" />
  ) : error ? (
    <>
      <Link className="back-link" to={manager ? '/tasks' : '/my-tasks'}>
        <ArrowLeft size={16} />
        Back to tasks
      </Link>
      <ErrorState message={error} onRetry={load} />
    </>
  ) : !task ? (
    <EmptyState title="Task not found">
      This task may have been removed or is no longer available to your account.
    </EmptyState>
  ) : (
    <div className="task-details-view">
      <Link className="back-link" to={manager ? '/tasks' : '/my-tasks'}>
        <ArrowLeft size={16} />
        Back to {manager ? 'all tasks' : 'my tasks'}
      </Link>

      <PageHeader
        eyebrow={`TASK #${task.id}`}
        title={task.title}
        description={task.description || 'No task description has been provided.'}
        actions={
          <>
            {manager && (
              <>
                <button className="button button-secondary" onClick={() => setDialog(true)}>
                  <Pencil size={15} />
                  Edit task
                </button>
                <button className="button button-danger" onClick={remove}>
                  <Trash2 size={15} />
                  Delete
                </button>
              </>
            )}
          </>
        }
      />

      <div className="detail-grid">
        <section className="section-panel detail-main">
          <div className="detail-heading">
            <div>
              <span className="eyebrow">CURRENT STATUS</span>
              <div className="detail-status-row">
                <StatusBadge status={task.status} />
                <PriorityBadge priority={task.priority} />
                {task.overdue && <span className="overdue-label inline">Overdue</span>}
              </div>
            </div>

            <div className="status-update-control">
              <span className="field-label">Update status</span>
              <select
                className="status-select"
                value={String(task.status).toUpperCase()}
                disabled={updating}
                onChange={e => updateStatus(e.target.value)}
              >
                <option value="PENDING">Pending</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>
          </div>

          <div className="detail-section">
            <span className="eyebrow">ABOUT THIS TASK</span>
            <p className="detail-description">{task.description || 'No additional context provided.'}</p>
          </div>

          <div className="detail-dates">
            <div>
              <span><CalendarDays size={16} />Start date</span>
              <strong>{formatDate(task.startDate)}</strong>
            </div>
            <div>
              <span><CalendarDays size={16} />Deadline</span>
              <strong className={task.overdue ? 'date-overdue' : ''}>{formatDate(task.deadline)}</strong>
            </div>
            <div>
              <span><Clock3 size={16} />Last updated</span>
              <strong>{formatDate(task.updatedAt)}</strong>
            </div>
            <div>
              <span><Clock3 size={16} />Created</span>
              <strong>{formatDate(task.createdAt)}</strong>
            </div>
          </div>

          {/* Activity Timeline Section */}
          <div className="timeline-section">
            <div className="section-head">
              <div>
                <span className="eyebrow">AUDIT TRAIL</span>
                <h2>Task activity timeline</h2>
              </div>
              <span className="section-note">{activities.length} {activities.length === 1 ? 'event' : 'events'} recorded</span>
            </div>

            {activities.length === 0 ? (
              <div className="timeline-empty">
                <History size={20} className="text-slate" />
                <p>No activity has been logged for this task yet.</p>
              </div>
            ) : (
              <div className="timeline-container">
                {activities.map((act, idx) => (
                  <div key={act.id || idx} className="timeline-item">
                    <div className="timeline-marker">
                      <div className="timeline-icon-wrap">
                        {getActivityIcon(act.action)}
                      </div>
                      {idx !== activities.length - 1 && <div className="timeline-line" />}
                    </div>

                    <div className="timeline-content">
                      <div className="timeline-header">
                        <div className="timeline-user">
                          <Avatar name={act.performedBy?.fullName || 'System'} size="avatar-small" />
                          <strong>{act.performedBy?.fullName || 'System'}</strong>
                        </div>
                        <span className="timeline-time">{formatDateTime(act.timestamp)}</span>
                      </div>

                      <div className="timeline-body">
                        <span className="timeline-action">{act.action}</span>
                        {(act.previousValue || act.newValue) && (
                          <div className="timeline-diff">
                            {act.previousValue && (
                              <span className="diff-pill diff-prev">
                                <del>{act.previousValue.replace('_', ' ')}</del>
                              </span>
                            )}
                            {act.previousValue && act.newValue && <span className="diff-arrow">→</span>}
                            {act.newValue && (
                              <span className="diff-pill diff-next">
                                <strong>{act.newValue.replace('_', ' ')}</strong>
                              </span>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>

        <aside className="section-panel detail-assignee">
          <span className="eyebrow">ASSIGNED TO</span>
          <div className="assignee-card">
            <Avatar name={task.assignedTo?.fullName || 'Unassigned'} size="avatar-large" />
            <div>
              <strong>{task.assignedTo?.fullName || 'Unassigned'}</strong>
              <span>{task.assignedTo?.designation || 'Team member'}</span>
            </div>
          </div>

          <div className="assignee-info">
            <div>
              <span>Department</span>
              <strong>{task.assignedTo?.department || '—'}</strong>
            </div>
            <div>
              <span>Email</span>
              <strong>{task.assignedTo?.email || '—'}</strong>
            </div>
            <div>
              <span>Account status</span>
              <strong>{task.assignedTo?.active ? 'Active' : 'Inactive'}</strong>
            </div>
          </div>
        </aside>
      </div>

      {dialog && (
        <TaskFormModal
          task={task}
          employees={employees}
          onClose={() => setDialog(false)}
          onSaved={() => { setDialog(false); load() }}
        />
      )}
    </div>
  )
}
