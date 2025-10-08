import { Card, Row, Col, Statistic } from 'antd'
import { FileTextOutlined, UserOutlined, QuestionCircleOutlined } from '@ant-design/icons'

export default function Dashboard() {
  return (
    <div>
      <h1>仪表盘</h1>
      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="总文档数"
              value={0}
              prefix={<FileTextOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="总用户数"
              value={0}
              prefix={<UserOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="查询次数"
              value={0}
              prefix={<QuestionCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
