import { useState } from 'react'
import { Input, Button, Card, List, Spin, Empty, message } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import api from '../services/api'
import { handleApiResponse, handleApiError } from '../utils/request'

const { TextArea } = Input

interface QueryResponse {
  answer: string
  sources: Array<{
    content: string
    documentId: string
  }>
}

export default function Knowledge() {
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState<QueryResponse | null>(null)

  const handleQuery = async () => {
    if (!question.trim()) {
      message.warning('请输入问题')
      return
    }

    setLoading(true)
    try {
      const result = await api.post('/knowledge/query', {
        question,
        topK: 5,
        threshold: 0.7,
      })
      const data = handleApiResponse<QueryResponse>(result)
      if (data) {
        setResponse(data)
      }
    } catch (error: any) {
      handleApiError(error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1>知识问答</h1>
      <Card style={{ marginTop: 24 }}>
        <TextArea
          rows={4}
          placeholder="请输入您的问题..."
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onPressEnter={(e) => {
            if (e.ctrlKey || e.metaKey) {
              handleQuery()
            }
          }}
        />
        <Button
          type="primary"
          icon={<SearchOutlined />}
          onClick={handleQuery}
          loading={loading}
          style={{ marginTop: 16 }}
        >
          查询
        </Button>
      </Card>

      {loading && (
        <div style={{ textAlign: 'center', marginTop: 32 }}>
          <Spin size="large" />
        </div>
      )}

      {!loading && response && (
        <>
          <Card title="回答" style={{ marginTop: 24 }}>
            <p style={{ whiteSpace: 'pre-wrap' }}>{response.answer}</p>
          </Card>

          {response.sources && response.sources.length > 0 && (
            <Card title="相关文档片段" style={{ marginTop: 24 }}>
              <List
                dataSource={response.sources}
                renderItem={(source, index) => (
                  <List.Item>
                    <List.Item.Meta
                      title={`片段 ${index + 1}`}
                      description={
                        <div style={{ whiteSpace: 'pre-wrap' }}>
                          {source.content.substring(0, 200)}...
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
            </Card>
          )}
        </>
      )}

      {!loading && !response && (
        <Empty
          description="输入问题并点击查询按钮开始使用"
          style={{ marginTop: 64 }}
        />
      )}
    </div>
  )
}
