import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { authApi } from '../services/api'

const AuthContext = createContext(null)
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [checking, setChecking] = useState(true)
  const refreshUser = useCallback(async () => {
    try { const me = await authApi.me(); setUser(me); return me }
    catch { setUser(null); return null }
  }, [])
  useEffect(() => { refreshUser().finally(() => setChecking(false)) }, [refreshUser])
  const login = async credentials => { const result = await authApi.login(credentials); const me = result?.user || result; setUser(me); return me }
  const logout = async () => { try { await authApi.logout() } finally { setUser(null) } }
  return <AuthContext.Provider value={{ user, checking, login, logout, refreshUser }}>{children}</AuthContext.Provider>
}
export const useAuth = () => useContext(AuthContext)
