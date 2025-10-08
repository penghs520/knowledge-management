import { useState, useEffect, useRef } from 'react'
import { Layout, Input, Button, Card, Space, message, Spin, Modal, Dropdown } from 'antd'
import { SendOutlined, PlusOutlined, DeleteOutlined, MoreOutlined, EditOutlined } from '@ant-design/icons'
import type { MenuProps } from 'antd'
import api from '../services/api'
import { handleApiResponse, handleApiError } from '../utils/request'

const { Sider, Content } = Layout
const { TextArea } = Input

interface Message {
  id: number
  role: string
  content: string
  createdAt: string
}

interface Conversation {
  id: number
  title: string
  isActive: boolean
  createdAt: string
  updatedAt: string
  messageCount: number
  messages?: Message[]
}

export default function Chat() {
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [currentConversation, setCurrentConversation] = useState<Conversation | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [loadingConversations, setLoadingConversations] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    loadConversations()
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  const loadConversations = async () => {
    setLoadingConversations(true)
    try {
      const response = await api.get('/conversations?size=50&sort=updatedAt,desc')
      const data = handleApiResponse<any>(response)
      if (data) {
        setConversations(data.content)
      }
    } catch (error: any) {
      handleApiError(error)
    } finally {
      setLoadingConversations(false)
    }
  }

  const loadConversationMessages = async (conversationId: number) => {
    try {
      const response = await api.get(`/conversations/${conversationId}`)
      const data = handleApiResponse<Conversation>(response)
      if (data) {
        setCurrentConversation(data)
        setMessages(data.messages || [])
      }
    } catch (error: any) {
      handleApiError(error)
    }
  }

  const createNewConversation = async () => {
    try {
      const response = await api.post('/conversations', { title: '新对话' })
      const data = handleApiResponse<Conversation>(response)
      if (data) {
        setConversations([data, ...conversations])
        setCurrentConversation(data)
        setMessages([])
      }
    } catch (error: any) {
      handleApiError(error)
    }
  }

  const handleSendMessage = async () => {
    if (!question.trim()) {
      message.warning('请输入问题')
      return
    }

    setLoading(true)
    const userQuestion = question
    setQuestion('')

    try {
      const response = await api.post('/conversations/chat', {
        conversationId: currentConversation?.id,
        question: userQuestion,
        topK: 5,
        threshold: 0.5,
      })

      const data = handleApiResponse<any>(response)
      if (data) {
        // If new conversation created, update current conversation
        if (!currentConversation || currentConversation.id !== data.conversationId) {
          await loadConversations()
          await loadConversationMessages(data.conversationId)
        } else {
          // Reload messages for current conversation
          await loadConversationMessages(currentConversation.id)
        }
      }
    } catch (error: any) {
      handleApiError(error)
      setQuestion(userQuestion) // Restore question on error
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteConversation = async (conversationId: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个对话吗？',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await api.delete(`/conversations/${conversationId}`)
          if (response.data.code === 200) {
            message.success('对话已删除')
            if (currentConversation?.id === conversationId) {
              setCurrentConversation(null)
              setMessages([])
            }
            await loadConversations()
          }
        } catch (error: any) {
          handleApiError(error)
        }
      },
    })
  }

  const handleRenameConversation = (conversationId: number, currentTitle: string) => {
    let newTitle = currentTitle
    Modal.confirm({
      title: '重命名对话',
      content: (
        <Input
          defaultValue={currentTitle}
          onChange={(e) => (newTitle = e.target.value)}
          onPressEnter={async () => {
            Modal.destroyAll()
            if (!newTitle.trim()) {
              message.warning('标题不能为空')
              return
            }
            try {
              const response = await api.put(`/conversations/${conversationId}/title`, { title: newTitle })
              if (response.data.code === 200) {
                message.success('标题已更新')
                await loadConversations()
                if (currentConversation?.id === conversationId) {
                  setCurrentConversation({ ...currentConversation, title: newTitle })
                }
              }
            } catch (error: any) {
              handleApiError(error)
            }
          }}
        />
      ),
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        if (!newTitle.trim()) {
          message.warning('标题不能为空')
          return Promise.reject()
        }
        try {
          const response = await api.put(`/conversations/${conversationId}/title`, { title: newTitle })
          if (response.data.code === 200) {
            message.success('标题已更新')
            await loadConversations()
            if (currentConversation?.id === conversationId) {
              setCurrentConversation({ ...currentConversation, title: newTitle })
            }
          }
        } catch (error: any) {
          handleApiError(error)
          return Promise.reject()
        }
      },
    })
  }

  const getConversationMenuItems = (conversation: Conversation): MenuProps['items'] => [
    {
      key: 'rename',
      icon: <EditOutlined />,
      label: '重命名',
      onClick: () => handleRenameConversation(conversation.id, conversation.title),
    },
    {
      key: 'delete',
      icon: <DeleteOutlined />,
      label: '删除',
      danger: true,
      onClick: () => handleDeleteConversation(conversation.id),
    },
  ]

  return (
    <Layout style={{ height: 'calc(100vh - 64px)' }}>
      <Sider
        width={280}
        theme="light"
        style={{
          borderRight: '1px solid #f0f0f0',
          height: '100%',
          overflow: 'hidden'
        }}
      >
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          height: '100%'
        }}>
          <div style={{ padding: '16px', flexShrink: 0 }}>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              block
              onClick={createNewConversation}
            >
              新建对话
            </Button>
          </div>
          <div style={{
            flex: 1,
            overflowY: 'auto',
            overflowX: 'hidden',
            padding: '0 16px 16px 16px',
            minHeight: 0
          }}>
          {loadingConversations ? (
            <div style={{ textAlign: 'center', padding: '20px' }}>
              <Spin />
            </div>
          ) : (
            conversations.map((conversation) => (
              <Card
                key={conversation.id}
                size="small"
                hoverable
                style={{
                  marginBottom: 8,
                  cursor: 'pointer',
                  backgroundColor:
                    currentConversation?.id === conversation.id ? '#e6f7ff' : 'white',
                }}
                onClick={() => loadConversationMessages(conversation.id)}
                extra={
                  <Dropdown menu={{ items: getConversationMenuItems(conversation) }}>
                    <Button
                      type="text"
                      size="small"
                      icon={<MoreOutlined />}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </Dropdown>
                }
              >
                <div style={{ fontSize: '14px', fontWeight: 500 }}>{conversation.title}</div>
                <div style={{ fontSize: '12px', color: '#999', marginTop: 4 }}>
                  {conversation.messageCount} 条消息
                </div>
              </Card>
            ))
          )}
          </div>
        </div>
      </Sider>
      <Content style={{ display: 'flex', flexDirection: 'column' }}>
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '24px',
            backgroundColor: '#fafafa',
          }}
        >
          {!currentConversation ? (
            <div
              style={{
                textAlign: 'center',
                padding: '100px 20px',
                color: '#999',
              }}
            >
              <h2>欢迎使用知识问答</h2>
              <p>请选择一个对话或创建新对话开始提问</p>
            </div>
          ) : messages.length === 0 ? (
            <div
              style={{
                textAlign: 'center',
                padding: '100px 20px',
                color: '#999',
              }}
            >
              <p>开始你的第一个问题吧</p>
            </div>
          ) : (
            <Space direction="vertical" style={{ width: '100%' }} size="large">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  style={{
                    display: 'flex',
                    justifyContent: msg.role === 'USER' ? 'flex-end' : 'flex-start',
                  }}
                >
                  <div
                    style={{
                      maxWidth: '70%',
                      padding: '12px 16px',
                      borderRadius: '8px',
                      backgroundColor: msg.role === 'USER' ? '#1890ff' : 'white',
                      color: msg.role === 'USER' ? 'white' : 'black',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                    }}
                  >
                    <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                      {msg.content}
                    </div>
                    <div
                      style={{
                        fontSize: '12px',
                        marginTop: '8px',
                        opacity: 0.7,
                      }}
                    >
                      {new Date(msg.createdAt).toLocaleTimeString()}
                    </div>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </Space>
          )}
        </div>
        <div
          style={{
            padding: '16px 24px',
            borderTop: '1px solid #f0f0f0',
            backgroundColor: 'white',
          }}
        >
          <Space.Compact style={{ width: '100%' }}>
            <TextArea
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="输入你的问题..."
              autoSize={{ minRows: 1, maxRows: 4 }}
              onPressEnter={(e) => {
                if (!e.shiftKey) {
                  e.preventDefault()
                  handleSendMessage()
                }
              }}
              disabled={loading}
            />
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={handleSendMessage}
              loading={loading}
              disabled={!question.trim()}
            >
              发送
            </Button>
          </Space.Compact>
        </div>
      </Content>
    </Layout>
  )
}
