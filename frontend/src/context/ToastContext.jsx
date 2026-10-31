import { createContext, useCallback, useContext, useState } from 'react'
import { CheckCircle2, CircleAlert, X } from 'lucide-react'

const ToastContext = createContext(() => {})
export function ToastProvider({ children }) {
  const [items, setItems] = useState([])
  const notify = useCallback((message, type = 'success') => {
    const id = Date.now() + Math.random()
    setItems(current => [...current, { id, message, type }])
    window.setTimeout(() => setItems(current => current.filter(item => item.id !== id)), 4200)
  }, [])
  return <ToastContext.Provider value={notify}>{children}<div className="toast-stack" aria-live="polite" aria-atomic="false">{items.map(item => <div className={`toast toast-${item.type}`} key={item.id}><span className="toast-icon">{item.type === 'error' ? <CircleAlert size={18}/> : <CheckCircle2 size={18}/>}</span><span>{item.message}</span><button className="icon-button" aria-label="Dismiss notification" onClick={() => setItems(current => current.filter(x => x.id !== item.id))}><X size={16}/></button></div>)}</div></ToastContext.Provider>
}
export const useToast = () => useContext(ToastContext)
