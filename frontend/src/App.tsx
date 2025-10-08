import { Routes, Route } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Documents from './pages/Documents'
import Knowledge from './pages/Knowledge'
import Chat from './pages/Chat'
import Login from './pages/Login'

function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="documents" element={<Documents />} />
          <Route path="knowledge" element={<Knowledge />} />
          <Route path="chat" element={<Chat />} />
        </Route>
      </Routes>
    </ConfigProvider>
  )
}

export default App
