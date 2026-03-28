'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Table,
  Button,
  Input,
  Space,
  Popconfirm,
  Modal,
  Form,
  Typography,
  message,
  Tooltip,
} from 'antd';
import {
  PlusOutlined,
  SearchOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { permissionsApi } from '@/lib/api';
import type { PermissionDto } from '@/types';

const { Title } = Typography;

interface CreatePermForm {
  name: string;
  description?: string;
  resource?: string;
  action?: string;
}

export default function PermissionsPage() {
  const qc = useQueryClient();
  const [msgApi, ctxHolder] = message.useMessage();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [form] = Form.useForm<CreatePermForm>();

  const { data, isLoading } = useQuery({
    queryKey: ['permissions', page],
    queryFn: () => permissionsApi.getAll(page, 50),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['permissions'] });

  const createMutation = useMutation({
    mutationFn: (vals: CreatePermForm) => permissionsApi.create(vals),
    onSuccess: () => {
      msgApi.success('Permission created');
      setCreateOpen(false);
      form.resetFields();
      invalidate();
    },
    onError: () => msgApi.error('Failed to create permission'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => permissionsApi.delete(id),
    onSuccess: () => { msgApi.success('Permission deleted'); invalidate(); },
    onError: () => msgApi.error('Failed to delete permission'),
  });

  const filtered = (data?.content ?? []).filter(
    (p) =>
      !search ||
      p.name.toLowerCase().includes(search.toLowerCase()) ||
      p.resource?.toLowerCase().includes(search.toLowerCase()),
  );

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
      sorter: (a: PermissionDto, b: PermissionDto) => a.name.localeCompare(b.name),
    },
    { title: 'Description', dataIndex: 'description', key: 'description' },
    { title: 'Resource', dataIndex: 'resource', key: 'resource' },
    { title: 'Action', dataIndex: 'action', key: 'action' },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (ts: string) => new Date(ts).toLocaleDateString(),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 80,
      render: (_: unknown, record: PermissionDto) => (
        <Popconfirm
          title="Delete this permission?"
          onConfirm={() => deleteMutation.mutate(record.id)}
          okText="Delete"
          okButtonProps={{ danger: true }}
        >
          <Tooltip title="Delete">
            <Button size="small" icon={<DeleteOutlined />} danger />
          </Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      {ctxHolder}
      <div className="flex items-center justify-between mb-4">
        <Title level={4} style={{ margin: 0 }}>
          Permission Management
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
          New Permission
        </Button>
      </div>

      <Input
        prefix={<SearchOutlined />}
        placeholder="Search permissions…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="mb-4"
        style={{ maxWidth: 320 }}
        allowClear
      />

      <Table<PermissionDto>
        dataSource={filtered}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: page + 1,
          total: data?.totalElements ?? 0,
          pageSize: 50,
          onChange: (p) => setPage(p - 1),
          showTotal: (t) => `${t} permissions`,
        }}
      />

      <Modal
        title="Create Permission"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={createMutation.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={(v) => createMutation.mutate(v)}>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input placeholder="e.g. READ_USERS" />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="resource" label="Resource">
            <Input placeholder="e.g. users" />
          </Form.Item>
          <Form.Item name="action" label="Action">
            <Input placeholder="e.g. read" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
