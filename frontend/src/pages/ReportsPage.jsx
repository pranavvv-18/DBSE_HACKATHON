import { useCallback, useEffect, useState } from 'react'
import { AlertCircle, ArrowDownRight, ArrowUpRight, CheckCircle2, Download, Filter, Flame, ListChecks, RefreshCw, ShieldAlert, Users } from 'lucide-react'
import { reportsApi, messageFromError } from '../services/api'
import { Avatar, EmptyState, ErrorState, Loading, PageHeader } from '../components/ui'
import { useToast } from '../context/ToastContext'

const cards = [
  ['totalTasks', 'Tasks in system', ListChecks, 'metric-ink'],
  ['pendingTasks', 'Pending', ArrowUpRight, 'metric-amber'],
  ['inProgressTasks', 'In progress', ArrowUpRight, 'metric-blue'],
  ['completedTasks', 'Completed', ArrowDownRight, 'metric-green'],
  ['overdueTasks', 'Overdue', RefreshCw, 'metric-red']
]

export default function ReportsPage() {
  const toast = useToast()
  const [summary, setSummary] = useState(null)
  const [performanceRows, setPerformanceRows] = useState([])
  const [workloadRows, setWorkloadRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [exporting, setExporting] = useState(false)
  const [highWorkloadOnly, setHighWorkloadOnly] = useState(false)
  const [activeTab, setActiveTab] = useState('workload') // 'workload' or 'performance'

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [s, p, w] = await Promise.all([
        reportsApi.summary(),
        reportsApi.performance(),
        reportsApi.workload().catch(() => [])
      ])
      setSummary(s)
      setPerformanceRows(Array.isArray(p) ? p : p?.content || [])
      setWorkloadRows(Array.isArray(w) ? w : [])
    } catch (e) {
      setError(messageFromError(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

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
      toast('Task report CSV downloaded successfully.')
    } catch (e) {
      toast(messageFromError(e), 'error')
    } finally {
      setExporting(false)
    }
  }

  // Workload filter
  const displayedWorkload = highWorkloadOnly
    ? workloadRows.filter(w => w.workloadLevel === 'HIGH' || w.overdueTasks > 0)
    : workloadRows

  const highWorkloadCount = workloadRows.filter(w => w.workloadLevel === 'HIGH').length

  return (
    <div className="reports-page">
      <PageHeader
        eyebrow="TEAM INSIGHTS & EXPORTS"
        title="Reports & analytics"
        description="Comprehensive delivery metrics, workload balancing, and exportable task data."
        actions={
          <>
            <button className="button button-secondary" onClick={load} disabled={loading} title="Refresh report metrics">
              <RefreshCw size={15} className={loading ? 'spin' : ''} />
              Refresh
            </button>
            <button
              className="button button-primary"
              onClick={handleDownloadCsv}
              disabled={exporting || loading}
              title="Download tasks as CSV spreadsheet"
            >
              <Download size={15} className={exporting ? 'spin' : ''} />
              {exporting ? 'Exporting…' : 'Export CSV report'}
            </button>
          </>
        }
      />

      {loading ? (
        <Loading label="Loading reports and workload analytics…" />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : (
        <>
          {/* Top Metric Strip */}
          <section className="metrics-strip report-metrics">
            {cards.map(([key, label, Icon, color]) => (
              <div className="metric" key={key}>
                <span className={`metric-icon ${color}`}>
                  <Icon size={18} />
                </span>
                <div>
                  <span className="metric-label">{label}</span>
                  <strong>{summary?.[key] ?? '—'}</strong>
                </div>
              </div>
            ))}
            <div className="metric completion-metric">
              <span className="metric-icon metric-teal">
                <Users size={18} />
              </span>
              <div>
                <span className="metric-label">Completed overall</span>
                <strong>
                  {summary?.completedPercentage == null
                    ? '—'
                    : `${Number(summary.completedPercentage).toFixed(1)}%`}
                </strong>
              </div>
            </div>
          </section>

          {/* Tab Navigation */}
          <div className="reports-tab-bar">
            <button
              className={`tab-button ${activeTab === 'workload' ? 'active' : ''}`}
              onClick={() => setActiveTab('workload')}
            >
              <Flame size={16} />
              Employee Workload Analysis
              {highWorkloadCount > 0 && (
                <span className="tab-pill tab-pill-alert" title={`${highWorkloadCount} employees with high workload`}>
                  {highWorkloadCount} High
                </span>
              )}
            </button>
            <button
              className={`tab-button ${activeTab === 'performance' ? 'active' : ''}`}
              onClick={() => setActiveTab('performance')}
            >
              <CheckCircle2 size={16} />
              Completion & Performance
            </button>
          </div>

          {/* Tab 1: Employee Workload */}
          {activeTab === 'workload' && (
            <section className="section-panel report-panel">
              <div className="section-head">
                <div>
                  <span className="eyebrow">CAPACITY MANAGEMENT</span>
                  <h2>Employee workload breakdown</h2>
                </div>
                <div className="workload-controls">
                  <label className="checkbox-toggle">
                    <input
                      type="checkbox"
                      checked={highWorkloadOnly}
                      onChange={e => setHighWorkloadOnly(e.target.checked)}
                    />
                    <span>Highlight High Workload & Overdue Only</span>
                  </label>
                </div>
              </div>

              {displayedWorkload.length === 0 ? (
                <EmptyState title="No workload data found">
                  {highWorkloadOnly
                    ? 'No employees are currently flagged with high workload or overdue tasks.'
                    : 'Employee workload will appear here as tasks are assigned.'}
                </EmptyState>
              ) : (
                <div className="table-wrap">
                  <table className="data-table workload-table">
                    <thead>
                      <tr>
                        <th>Employee</th>
                        <th>Department</th>
                        <th>Total Assigned</th>
                        <th>Pending</th>
                        <th>In Progress</th>
                        <th>Completed</th>
                        <th>Overdue</th>
                        <th>Workload Level</th>
                      </tr>
                    </thead>
                    <tbody>
                      {displayedWorkload.map(emp => {
                        const levelClass =
                          emp.workloadLevel === 'HIGH'
                            ? 'level-high'
                            : emp.workloadLevel === 'MODERATE'
                            ? 'level-moderate'
                            : 'level-low'

                        return (
                          <tr key={emp.employeeId} className={emp.workloadLevel === 'HIGH' ? 'row-alert' : ''}>
                            <td>
                              <div className="person-cell">
                                <Avatar name={emp.fullName} />
                                <span>
                                  <strong>{emp.fullName}</strong>
                                  <small>{emp.designation || 'Team member'}</small>
                                </span>
                              </div>
                            </td>
                            <td>{emp.department || '—'}</td>
                            <td><strong>{emp.totalTasks}</strong></td>
                            <td>
                              <span className="count-pill pill-pending">{emp.pendingTasks}</span>
                            </td>
                            <td>
                              <span className="count-pill pill-progress">{emp.inProgressTasks}</span>
                            </td>
                            <td>
                              <span className="count-pill pill-completed">{emp.completedTasks}</span>
                            </td>
                            <td>
                              {emp.overdueTasks > 0 ? (
                                <span className="count-pill pill-overdue">
                                  {emp.overdueTasks} Overdue
                                </span>
                              ) : (
                                <span className="text-muted">0</span>
                              )}
                            </td>
                            <td>
                              <span className={`workload-badge ${levelClass}`}>
                                {emp.workloadLevel === 'HIGH' && <Flame size={13} />}
                                {emp.workloadLevel}
                              </span>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          {/* Tab 2: Performance */}
          {activeTab === 'performance' && (
            <section className="section-panel report-panel">
              <div className="section-head">
                <div>
                  <span className="eyebrow">EMPLOYEE PERFORMANCE</span>
                  <h2>Completion by employee</h2>
                </div>
                <span className="section-note">Based on active database records</span>
              </div>

              {performanceRows.length === 0 ? (
                <EmptyState title="No performance data yet">
                  Employee results will appear as tasks are assigned.
                </EmptyState>
              ) : (
                <div className="table-wrap">
                  <table className="data-table report-table">
                    <thead>
                      <tr>
                        <th>Employee</th>
                        <th>Department</th>
                        <th>Completed</th>
                        <th>Total tasks</th>
                        <th>Completion</th>
                      </tr>
                    </thead>
                    <tbody>
                      {performanceRows.map(row => (
                        <tr key={row.employeeId}>
                          <td>
                            <div className="person-cell">
                              <Avatar name={row.fullName} />
                              <span>
                                <strong>{row.fullName}</strong>
                                <small>Employee #{row.employeeId}</small>
                              </span>
                            </div>
                          </td>
                          <td>{row.department || '—'}</td>
                          <td>{row.completedTasks ?? 0}</td>
                          <td>{row.totalTasks ?? 0}</td>
                          <td>
                            <div className="report-progress">
                              <div className="bar-track">
                                <div
                                  className="bar-fill bar-completed"
                                  style={{
                                    width: `${Math.min(100, Math.max(0, Number(row.completionRate || 0)))}%`
                                  }}
                                />
                              </div>
                              <strong>{Math.round(Number(row.completionRate || 0))}%</strong>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          <p className="data-note">
            Reports and workloads are computed live from persistent MySQL records.
          </p>
        </>
      )}
    </div>
  )
}
