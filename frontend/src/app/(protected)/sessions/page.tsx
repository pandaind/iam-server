'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Table,
  Button,
  Space,
  Typography,
  message,
  Tooltip,
  Popconfirm,
  Tag,
  Input,
  Collapse,
} from 'antd';
import { DeleteOutlined, LogoutOutlined } from '@ant-design/icons';
import { sessionsApi } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';
import type { UserSessionDto } from '@/types';

const { Title, Text } = Typography;

export default function SessionsPage() {
  const qc = useQueryClient();
  const [msgApi, ctxHolder] = message.useMessage();
  const { user, hasPermission } = useAuth();
  const [adminUsername, setAdminUsername] = useState('');

  const { data: mySessions, isLoading } = useQuery({
    queryKey: ['my-sessions'],
    queryFn: () => sessionsApi.getMySessions(),
    refetchInterval: 30_000,
  });

  const { data: adminSessions, isLoading: loadingAdmin } = useQuery({
    queryKey: ['admin-sessions', adminUsername],
    queryFn: () => sessionsApi.getUserSessions(adminUsername),
    enabled: !!adminUsername && hasPermission('MANAGE_SESSIONS'),
  });

  const invalidateMy = () => qc.invalidateQueries({ queryKey: ['my-sessions'] });
  const invalidateAdmin = () =>
    qc.invalidateQueries({ queryKey: ['admin-sessions', adminUsername] });

  const revokeMyMutation = useMutation({
    mutationFn: (sessionId: string) => sessionsApi.invalidateMySession(sessionId),
    onSuccess: () => { msgApi.success('Session revoked'); invalidateMy(); },
  });

  const revokeAllMyMutation = useMutation({
    mutationFn: () => sessionsApi.invalidateAllMySessions(),
    onSuccess: () => { msgApi.success('All sessions revoked'); invalidateMy(); },
  });

  const revokeAdminMutation = useMutation({
    mutationFn: (sessionId: string) =>
      sessionsApi.invalidateUserSession(adminUsername, sessionId),
    onSuccess: () => { msgApi.success('Session revoked'); invalidateAdmin(); },
  });

  const revokeAllAdminMutation = useMutation({
    mutationFn: () => sessionsApi.invalidateAllUserSessions(adminUsername),
    onSuccess: () => { msgApi.success('All user sessions revoked'); invalidateAdmin(); },
  });

  const sessionColumns = (
    revokeAction: (sessionId: string) => void,
    isPending: boolean,
  ) => [
    { title: 'Session ID', dataIndex: 'sessionId', key: 'sessionId', width: 120,
      render: (s: string) => <Text code>{s.slice(0, 12)}…</Text> },
    { title: 'IP', dataIndex: 'ipAddress', key: 'ipAddress', width: 130 },
    { title: 'User Agent', dataIndex: 'userAgent', key: 'userAgent',
      render: (ua: string) => <Text ellipsis style={{ maxWidth: 200 }}>{ua}</Text> },
    { title: 'Created', dataIndex: 'createdAt', key: 'createdAt',
      render: (ts: string) => new Date(ts).toLocaleString() },
    { title: 'Last Active', dataIndex: 'lastAccessedAt', key: 'lastAccessedAt',
      render: (ts: string) => new Date(ts).toLocaleString() },
    {
      title: 'Expires',
      dataIndex: 'expiresAt',
      key: 'expiresAt',
      render: (ts: string) => {
        const exp = new Date(ts);
        const expired = exp < new Date();
        return <Tag color={expired ? 'error' : 'success'}>{exp.toLocaleString()}</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 80,
      render: (_: unknown, record: UserSessionDto) => (
        <Popconfirm
          title="Revoke this session?"
          onConfirm={() => revokeAction(record.sessionId)}
          okText="Revoke"
          okButtonProps={{ danger: true }}
        >
          <Tooltip title="Revoke">
            <Button
              size="small"
              icon={<DeleteOutlined />}
              danger
              loading={isPending}
            />
          </Tooltip>
        </Popconfirm>
      ),
    },
  ];

  const panels = [
    {
      key: 'my',
      label: `My Active Sessions (${mySessions?.length ?? 0})`,
      children: (
        <>
          <div className="mb-3 flex justify-end">
            <Popconfirm
              title="Revoke ALL your sessions?"
              onConfirm={() => revokeAllMyMutation.mutate()}
              okText="Revoke All"
              okButtonProps={{ danger: true }}
            >
              <Button
                icon={<LogoutOutlined />}
                danger
                loading={revokeAllMyMutation.isPending}
              >
                Revoke All My Sessions
              </Button>
            </Popconfirm>
          </div>
          <Table<UserSessionDto>
            dataSource={mySessions ?? []}
            columns={sessionColumns(
              (sid) => revokeMyMutation.mutate(sid),
              revokeMyMutation.isPending,
            )}
            rowKey="sessionId"
            loading={isLoading}
            pagination={false}
            scroll={{ x: 700 }}
          />
        </>
      ),
    },
  ];

  if (hasPermission('MANAGE_SESSIONS')) {
    panels.push({
      key: 'admin',
      label: 'Admin: Manage User Sessions',
      children: (
        <>
          <Space className="mb-3">
            <Input
              placeholder="Enter username…"
              value={adminUsername}
              onChange={(e) => setAdminUsername(e.target.value)}
              style={{ width: 240 }}
            />
            {adminUsername && (
              <Popconfirm
                title={`Revoke ALL sessions for ${adminUsername}?`}
                onConfirm={() => revokeAllAdminMutation.mutate()}
                okText="Revoke All"
                okButtonProps={{ danger: true }}
              >
                <Button
                  danger
                  loading={revokeAllAdminMutation.isPending}
                  disabled={!adminSessions?.length}
                >
                  Revoke All
                </Button>
              </Popconfirm>
            )}
          </Space>
          <Table<UserSessionDto>
            dataSource={adminSessions ?? []}
            columns={sessionColumns(
              (sid) => revokeAdminMutation.mutate(sid),
              revokeAdminMutation.isPending,
            )}
            rowKey="sessionId"
            loading={loadingAdmin}
            pagination={false}
            scroll={{ x: 700 }}
            locale={{ emptyText: adminUsername ? 'No sessions found' : 'Enter a username above' }}
          />
        </>
      ),
    });
  }

  return (
    <div>
      {ctxHolder}
      <Title level={4} style={{ marginBottom: 16 }}>
        Session Management
      </Title>
      <Collapse defaultActiveKey={['my']} items={panels} />
    </div>
  );
}
