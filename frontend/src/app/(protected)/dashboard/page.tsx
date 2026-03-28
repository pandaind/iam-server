'use client';

import { useQuery } from '@tanstack/react-query';
import { Card, Col, Row, Statistic, Typography, Table, Tag, Spin } from 'antd';
import {
  TeamOutlined,
  SafetyCertificateOutlined,
  KeyOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import { usersApi, rolesApi, sessionsApi, auditApi } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';
import type { AuditLog } from '@/types';

const { Title } = Typography;

const auditColumns = [
  { title: 'Action', dataIndex: 'action', key: 'action', width: 160 },
  { title: 'User', dataIndex: 'username', key: 'username', width: 120 },
  { title: 'Resource', dataIndex: 'resource', key: 'resource', width: 140 },
  {
    title: 'Status',
    dataIndex: 'success',
    key: 'success',
    width: 90,
    render: (ok: boolean) =>
      ok ? (
        <Tag icon={<CheckCircleOutlined />} color="success">OK</Tag>
      ) : (
        <Tag icon={<CloseCircleOutlined />} color="error">Failed</Tag>
      ),
  },
  {
    title: 'Time',
    dataIndex: 'timestamp',
    key: 'timestamp',
    render: (ts: string) => new Date(ts).toLocaleString(),
  },
];

export default function DashboardPage() {
  const { user } = useAuth();

  const { data: users, isLoading: loadingUsers } = useQuery({
    queryKey: ['users', 0, 1],
    queryFn: () => usersApi.getAll(0, 1),
  });

  const { data: roles, isLoading: loadingRoles } = useQuery({
    queryKey: ['roles-active'],
    queryFn: () => rolesApi.getActive(),
  });

  const { data: sessions, isLoading: loadingSessions } = useQuery({
    queryKey: ['my-sessions'],
    queryFn: () => sessionsApi.getMySessions(),
  });

  const { data: recentAudit, isLoading: loadingAudit } = useQuery({
    queryKey: ['audit', 0, 5],
    queryFn: () => auditApi.getAll(0, 5),
  });

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>
        Welcome back, {user?.firstName ?? user?.username}
      </Title>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              title="Total Users"
              value={users?.totalElements ?? '-'}
              prefix={<TeamOutlined style={{ color: '#1890ff' }} />}
              loading={loadingUsers}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              title="Active Roles"
              value={roles?.length ?? '-'}
              prefix={<SafetyCertificateOutlined style={{ color: '#52c41a' }} />}
              loading={loadingRoles}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              title="My Active Sessions"
              value={sessions?.length ?? '-'}
              prefix={<ClockCircleOutlined style={{ color: '#fa8c16' }} />}
              loading={loadingSessions}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              title="Recent Audit Events"
              value={recentAudit?.totalElements ?? '-'}
              prefix={<KeyOutlined style={{ color: '#13c2c2' }} />}
              loading={loadingAudit}
            />
          </Card>
        </Col>
      </Row>

      <Card title="Recent Audit Activity" className="mt-4" style={{ marginTop: 16 }}>
        {loadingAudit ? (
          <div className="text-center py-8">
            <Spin />
          </div>
        ) : (
          <Table<AuditLog>
            dataSource={recentAudit?.content ?? []}
            columns={auditColumns}
            rowKey="id"
            pagination={false}
            size="small"
          />
        )}
      </Card>
    </div>
  );
}
