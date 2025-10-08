import { Card, Form, Input, Button, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import api from '../services/api'

export default function Login() {
  const navigate = useNavigate()

  const onFinish = async (values: { username: string; password: string }) => {
    try {
      const response = await api.post('/auth/login', values)

      if (response.data.code === 200) {
        localStorage.setItem('token', response.data.data.token)
        message.success(response.data.message || '登录成功')
        navigate('/')
      } else {
        message.error(response.data.message || '登录失败')
      }
    } catch (error: any) {
      console.error('登录失败:', error)
      const errorMsg = error.response?.data?.message || '登录失败，请检查用户名和密码'
      message.error(errorMsg)
    }
  }

  return (
    <div style={{
      height: '100vh',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      background: '#f0f2f5'
    }}>
      <Card title="企业知识管理系统" style={{ width: 400 }}>
        <Form onFinish={onFinish} autoComplete="off">
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
