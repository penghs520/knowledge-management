import { useState, useEffect, useRef } from 'react'
import { Layout, Input, Button, Card, Space, message, Spin, Modal, Dropdown } from 'antd'
import { SendOutlined, PlusOutlined, DeleteOutlined, MoreOutlined, EditOutlined, CopyOutlined, ReloadOutlined } from '@ant-design/icons'
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

  const sendStreamingMessage = async (userQuestion: string, existingMessages: Message[]) => {
    setLoading(true)

    // Add user message to UI immediately if not regenerating
    const needsUserMessage = existingMessages.length === 0 || existingMessages[existingMessages.length - 1].role !== 'USER'
    const tempUserMessage: Message | null = needsUserMessage ? {
      id: Date.now(),
      role: 'USER',
      content: userQuestion,
      createdAt: new Date().toISOString(),
    } : null

    // Add temporary assistant message for streaming
    const tempAssistantMessage: Message = {
      id: Date.now() + 1,
      role: 'ASSISTANT',
      content: '',
      createdAt: new Date().toISOString(),
    }

    const newMessages = tempUserMessage
      ? [...existingMessages, tempUserMessage, tempAssistantMessage]
      : [...existingMessages, tempAssistantMessage]

    setMessages(newMessages)

    try {
      const token = localStorage.getItem('token')
      const response = await fetch('/api/conversations/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          conversationId: currentConversation?.id,
          question: userQuestion,
          topK: 5,
          threshold: 0.5,
        }),
      })

      if (!response.ok) {
        throw new Error('Stream request failed')
      }

      const reader = response.body?.getReader()
      const decoder = new TextDecoder()
      let streamedConversationId: number | null = null
      let streamedContent = ''
      let buffer = '' // Buffer for incomplete lines

      if (reader) {
        try {
          while (true) {
            const { done, value } = await reader.read()
            if (done) {
              console.log('Stream done, buffer remaining:', buffer)
              break
            }

            const chunk = decoder.decode(value, { stream: true })
            console.log('Received chunk:', chunk)
            buffer += chunk

            // NDJSON format: split by newlines
            const lines = buffer.split('\n')

            // Keep the last potentially incomplete line in buffer
            buffer = lines.pop() || ''

            for (const line of lines) {
              const trimmed = line.trim()
              // Skip empty lines
              if (!trimmed) {
                continue
              }

              // Parse JSON directly (NDJSON format)
              try {
                const data = JSON.parse(trimmed)
                console.log('Parsed data:', data)

                    if (data.type === 'start') {
                      streamedConversationId = data.conversationId
                      console.log('Started conversation:', streamedConversationId)
                    } else if (data.type === 'content') {
                      streamedContent += data.content
                      console.log('Content added, total length:', streamedContent.length)
                      // Update the temporary assistant message
                      setMessages((prev) => {
                        const updated = [...prev]
                        updated[updated.length - 1] = {
                          ...updated[updated.length - 1],
                          content: streamedContent,
                        }
                        return updated
                      })
                    } else if (data.type === 'done') {
                      console.log('Received done message')
                      setLoading(false)
                      // Reload conversations and messages
                      loadConversations()
                      if (streamedConversationId) {
                        if (!currentConversation || currentConversation.id !== streamedConversationId) {
                          loadConversationMessages(streamedConversationId)
                        } else {
                          loadConversationMessages(currentConversation.id)
                        }
                      }
                } else if (data.type === 'error') {
                  console.error('Received error:', data.message)
                  message.error(data.message || '发生错误')
                  setLoading(false)
                  setQuestion(userQuestion)
                }
              } catch (e) {
                console.error('Error parsing JSON:', e, 'Line:', trimmed)
              }
            }
          }

          // Process any remaining data in buffer
          const trimmed = buffer.trim()
          if (trimmed) {
            try {
              const data = JSON.parse(trimmed)
              console.log('Parsed remaining buffer:', data)
              if (data.type === 'content') {
                streamedContent += data.content
                setMessages((prev) => {
                  const updated = [...prev]
                  updated[updated.length - 1] = {
                    ...updated[updated.length - 1],
                    content: streamedContent,
                  }
                  return updated
                })
              } else if (data.type === 'done') {
                setLoading(false)
                loadConversations()
                if (streamedConversationId) {
                  if (!currentConversation || currentConversation.id !== streamedConversationId) {
                    loadConversationMessages(streamedConversationId)
                  } else {
                    loadConversationMessages(currentConversation.id)
                  }
                }
              }
            } catch (e) {
              console.error('Error parsing remaining buffer:', e, 'Buffer:', trimmed)
            }
          }
        } finally {
          // Ensure loading is always set to false when stream ends
          setLoading(false)
          // Reload conversations and messages
          if (streamedConversationId) {
            loadConversations()
            if (!currentConversation || currentConversation.id !== streamedConversationId) {
              loadConversationMessages(streamedConversationId)
            } else {
              loadConversationMessages(currentConversation.id)
            }
          }
        }
      }
    } catch (error: any) {
      handleApiError(error)
      setQuestion(userQuestion)
      setLoading(false)
    }
  }

  const handleSendMessage = async () => {
    if (!question.trim()) {
      message.warning('请输入问题')
      return
    }

    const userQuestion = question
    setQuestion('')
    await sendStreamingMessage(userQuestion, messages)
  }

  const handleRegenerateResponse = async (messageIndex: number) => {
    // Only allow regenerating the last assistant message
    if (messageIndex !== messages.length - 1) {
      message.warning('只能重新生成最后一条回答')
      return
    }

    // Find the user message before this assistant message
    if (messageIndex === 0 || messages[messageIndex - 1].role !== 'USER') {
      message.error('无法找到对应的用户消息')
      return
    }

    if (!currentConversation) {
      message.error('请先选择一个对话')
      return
    }

    const userMessage = messages[messageIndex - 1]
    const assistantMessage = messages[messageIndex]

    // Check if these are real messages (not temporary ones)
    const userMessageId = userMessage.id
    const assistantMessageId = assistantMessage.id
    const isRealMessage = (id: number) => !isNaN(id) && id < Date.now() - 1000000

    try {
      // Delete messages from backend if they are real (not temporary)
      const messageIdsToDelete: number[] = []
      if (isRealMessage(userMessageId)) {
        messageIdsToDelete.push(userMessageId)
      }
      if (isRealMessage(assistantMessageId)) {
        messageIdsToDelete.push(assistantMessageId)
      }

      if (messageIdsToDelete.length > 0) {
        await api.delete(`/conversations/${currentConversation.id}/messages`, {
          params: { messageIds: messageIdsToDelete }
        })
      }

      // Remove the user and assistant messages from UI (last 2 messages)
      const messagesBeforeRegeneration = messages.slice(0, messageIndex - 1)

      // Regenerate the response
      await sendStreamingMessage(userMessage.content, messagesBeforeRegeneration)
    } catch (error: any) {
      handleApiError(error)
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

  const handleCopyMessage = (content: string) => {
    navigator.clipboard.writeText(content).then(() => {
      message.success('已复制到剪贴板')
    }).catch(() => {
      message.error('复制失败')
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
              <h2>欢迎使用智能对话</h2>
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
              {messages.map((msg, index) => (
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
                      {msg.content || (
                        <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                          <style>{`
                            @keyframes dot-pulse {
                              0%, 80%, 100% {
                                transform: scale(0.8);
                                opacity: 0.5;
                              }
                              40% {
                                transform: scale(1);
                                opacity: 1;
                              }
                            }
                            .dot {
                              width: 8px;
                              height: 8px;
                              background-color: #333;
                              border-radius: 50%;
                              animation: dot-pulse 1.4s infinite ease-in-out;
                            }
                            .dot:nth-child(1) { animation-delay: 0s; }
                            .dot:nth-child(2) { animation-delay: 0.2s; }
                            .dot:nth-child(3) { animation-delay: 0.4s; }
                          `}</style>
                          <div className="dot"></div>
                          <div className="dot"></div>
                          <div className="dot"></div>
                        </div>
                      )}
                    </div>
                    {msg.content && (
                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          marginTop: '8px',
                          fontSize: '12px',
                          opacity: 0.7,
                        }}
                      >
                        <span>{new Date(msg.createdAt).toLocaleTimeString()}</span>
                        {msg.role === 'ASSISTANT' && (
                          <div style={{ display: 'flex', gap: '4px' }}>
                            <Button
                              type="text"
                              size="small"
                              icon={<CopyOutlined />}
                              onClick={() => handleCopyMessage(msg.content)}
                              style={{
                                fontSize: '12px',
                                padding: '0 4px',
                                height: 'auto',
                                color: 'inherit'
                              }}
                            >
                              复制
                            </Button>
                            {index === messages.length - 1 && (
                              <Button
                                type="text"
                                size="small"
                                icon={<ReloadOutlined />}
                                onClick={() => handleRegenerateResponse(index)}
                                disabled={loading}
                                style={{
                                  fontSize: '12px',
                                  padding: '0 4px',
                                  height: 'auto',
                                  color: 'inherit'
                                }}
                              >
                                重试
                              </Button>
                            )}
                          </div>
                        )}
                      </div>
                    )}
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
