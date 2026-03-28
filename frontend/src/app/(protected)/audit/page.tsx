'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Table,
  Input,
  Space,
  Tag,
  Typography,
  DatePicker,
  Select,
  Button,
  Tooltip,
  Drawer,
  Descriptions,
} from 'antd';
import {
  SearchOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { auditApi } from '@/lib/api';
import type { AuditLog } from '@/types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

export default function AuditPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [viewLog, setViewLog] = useState<AuditLog | null>(null);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [onlyFailed, setOnlyFailed] = useState(false);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['audit', page, onlyFailed, dateRange],
    queryFn: () => {
      if (onlyFailed) return auditApi.getFailed(page, 20);
      if (dateRange) {
        return auditApi.getByDateRange(
          dateRange[0].toISOString(),
          dateRange[1].toISOString(),
          page,
          20,
        );
      }
      return auditApi.getAll(page, 20);
    },
  });

  const filtered = (data?.content ?? []).filter(
    (log) =>
      !search ||
      log.action?.toLowerCase().includes(search.toLowerCase()) ||
      log.username?.toLowerCase().includes(search.toLowerCase()) ||
      log.resource?.toLowerCase().includes(search.toLowerCase()),
  );

  const columns = [
    {
      title: 'Time',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 160,
      render: (ts: string) => new Date(ts).toLocaleString(),
      sorter: (a: AuditLog, b: AuditLog) =>
        new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
    },
    { title: 'User', dataIndex: 'username', key: 'username', width: 120 },
    { title: 'Action', dataIndex: 'action', key: 'action', width: 180 },
    { title: 'Resource', dataIndex: 'resource', key: 'resource', width: 120 },
    { title: 'IP', dataIndex: 'ipAddress', key: 'ipAddress', width: 120 },
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
      title: 'Details',
      key: 'details',
      width: 70,
      render: (_: unknown, record: AuditLog) => (
        <Tooltip title="View details">
          <Button size="small" icon={<EyeOutlined />} onClick={() => setViewLog(record)} />
        </Tooltip>
      ),
    },
  ];

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <Title level={4} style={{ margin: 0 }}>
          Audit Logs
        </Title>
        <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
          Refresh
        </Button>
      </div>

      <Space wrap className="mb-4">
        <Input
          prefix={<SearchOutlined />}
          placeholder="Search by action, user, resource…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ width: 280 }}
          allowClear
        />
        <RangePicker
          showTime
          onChange={(vals) =>
            setDateRange(vals as [dayjs.Dayjs, dayjs.Dayjs] | null)
          }
        />
        <Select
          value={onlyFailed ? 'failed' : 'all'}
          onChange={(v) => { setOnlyFailed(v === 'failed'); setPage(0); }}
          options={[
            { value: 'all', label: 'All Events' },
            { value: 'failed', label: 'Failed Only' },
          ]}
          style={{ width: 140 }}
        />
      </Space>

      <Table<AuditLog>
        dataSource={filtered}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: page + 1,
          total: data?.totalElements ?? 0,
          pageSize: 20,
          onChange: (p) => setPage(p - 1),
          showTotal: (t) => `${t} events`,
        }}
        scroll={{ x: 900 }}
      />

      <Drawer
        title="Audit Log Details"
        open={!!viewLog}
        onClose={() => setViewLog(null)}
        width={440}
      >
        {viewLog && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="ID">{viewLog.id}</Descriptions.Item>
            <Descriptions.Item label="Timestamp">
              {new Date(viewLog.timestamp).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="User">{viewLog.username ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Action">{viewLog.action}</Descriptions.Item>
            <Descriptions.Item label="Resource">{viewLog.resource}</Descriptions.Item>
            <Descriptions.Item label="IP">{viewLog.ipAddress ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="User Agent">
              <Text style={{ fontSize: 12 }}>{viewLog.userAgent ?? '—'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Status">
              {viewLog.success ? (
                <Tag color="success">Success</Tag>
              ) : (
                <Tag color="error">Failed</Tag>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Details">
              <pre style={{ whiteSpace: 'pre-wrap', fontSize: 12, margin: 0 }}>
                {viewLog.details ?? '—'}
              </pre>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  );
}
