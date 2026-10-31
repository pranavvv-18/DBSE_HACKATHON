import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use(config => {
  const method = String(config.method || 'get').toUpperCase()
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    config.headers = config.headers || {}
    if (typeof config.headers.set === 'function') {
      config.headers.set('X-TaskFlow-Request', '1')
    } else {
      config.headers['X-TaskFlow-Request'] = '1'
    }
  }
  return config
})

export function messageFromError(error) {
  const data = error?.response?.data
  if (typeof data === 'string') return data
  return data?.message || data?.detail || data?.error || error?.message || 'Something went wrong. Please try again.'
}

export const authApi = {
  me: () => http.get('/auth/me').then(r => r.data),
  login: credentials => http.post('/auth/login', credentials).then(r => r.data),
  logout: () => http.post('/auth/logout'),
}

export const employeeApi = {
  list: params => http.get('/employees', { params }).then(r => r.data),
  create: body => http.post('/employees', body).then(r => r.data),
  update: (id, body) => http.put(`/employees/${id}`, body).then(r => r.data),
  deactivate: id => http.delete(`/employees/${id}`),
}

export const taskApi = {
  list: params => http.get('/tasks', { params }).then(r => r.data),
  get: id => http.get(`/tasks/${id}`).then(r => r.data),
  create: body => http.post('/tasks', body).then(r => r.data),
  update: (id, body) => http.put(`/tasks/${id}`, body).then(r => r.data),
  remove: id => http.delete(`/tasks/${id}`),
  updateStatus: (id, status) => http.patch(`/tasks/${id}/status`, { status }).then(r => r.data),
  activities: id => http.get(`/tasks/${id}/activities`).then(r => r.data),
}

export const dashboardApi = {
  stats: () => http.get('/dashboard/stats').then(r => r.data),
  recent: params => http.get('/dashboard/recent-tasks', { params }).then(r => r.data),
  progress: () => http.get('/dashboard/employee-progress').then(r => r.data),
}

export const reportsApi = {
  summary: () => http.get('/reports/task-summary').then(r => r.data),
  performance: () => http.get('/reports/employee-performance').then(r => r.data),
  workload: () => http.get('/reports/employee-workload').then(r => r.data),
  downloadCsv: () => http.get('/reports/tasks/csv', { responseType: 'blob' }).then(r => r.data),
}
