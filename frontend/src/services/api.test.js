import assert from 'node:assert/strict'
import test from 'node:test'
import { authApi, employeeApi, http, reportsApi, taskApi } from './api.js'

test('every API write carries the backend request guard header', async () => {
  const requests = []
  http.defaults.adapter = async config => {
    requests.push(config)
    return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
  }

  await authApi.login({ email: 'demo@example.test', password: 'not-a-real-password' })
  await authApi.logout()
  await employeeApi.create({})
  await employeeApi.update(1, {})
  await employeeApi.deactivate(1)
  await taskApi.create({})
  await taskApi.update(1, {})
  await taskApi.remove(1)
  await taskApi.updateStatus(1, 'COMPLETED')
  await taskApi.activities(1)
  await reportsApi.workload()
  await reportsApi.downloadCsv()
  await authApi.me()

  const writes = requests.filter(request => ['post', 'put', 'patch', 'delete'].includes(request.method))
  const reads = requests.filter(request => request.method === 'get')
  assert.equal(writes.length, 9)
  assert.equal(reads.length, 4)
  assert.ok(writes.every(request => request.headers.get('X-TaskFlow-Request') === '1'))
  assert.ok(reads.every(request => request.headers.get('X-TaskFlow-Request') == null))
})
