import { useState } from 'react'
import { Upload, Button, Table, message, Space, Input } from 'antd'
import { UploadOutlined, SearchOutlined, DeleteOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd'
import api from '../services/api'
import { handleApiResponse, handleApiError } from '../utils/request'
import type { PageResponse } from '../types/api'

export default function Documents() {
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(false)

  const uploadProps: UploadProps = {
    name: 'file',
    action: '/api/documents/upload',
    headers: {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    },
    onChange(info) {
      if (info.file.status === 'done') {
        message.success(`${info.file.name} 上传成功`)
        loadDocuments()
      } else if (info.file.status === 'error') {
        message.error(`${info.file.name} 上传失败`)
      }
    },
  }

  const loadDocuments = async () => {
    setLoading(true)
    try {
      const response = await api.get<any, { data: any }>('/documents')
      const data = handleApiResponse<PageResponse<any>>(response)
      if (data) {
        setDocuments(data.content)
      }
    } catch (error: any) {
      handleApiError(error)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id: number) => {
    try {
      const response = await api.delete(`/documents/${id}`)
      const data = handleApiResponse(response)
      if (data !== null) {
        message.success('删除成功')
        loadDocuments()
      }
    } catch (error: any) {
      handleApiError(error)
    }
  }

  const columns = [
    {
      title: '文件名',
      dataIndex: 'fileName',
      key: 'fileName',
    },
    {
      title: '文件类型',
      dataIndex: 'fileType',
      key: 'fileType',
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      render: (size: number) => `${(size / 1024).toFixed(2)} KB`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <Space>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h1>文档管理</h1>
        <Upload {...uploadProps}>
          <Button icon={<UploadOutlined />}>上传文档</Button>
        </Upload>
      </div>
      <Input.Search
        placeholder="搜索文档"
        enterButton={<SearchOutlined />}
        style={{ marginBottom: 16 }}
      />
      <Table
        columns={columns}
        dataSource={documents}
        loading={loading}
        rowKey="id"
      />
    </div>
  )
}
