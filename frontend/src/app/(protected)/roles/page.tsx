'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Table,
  Button,
  Input,
  Space,
  Tag,
  Popconfirm,
  Modal,
  Form,
  Select,
  Typography,
  message,
  Tooltip,
  Drawer,
  Descriptions,
} from 'antd';
import {
  PlusOutlined,
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckOutlined,
  StopOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { rolesApi, permissionsApi } from '@/lib/api';
import type { RoleDto, CreateRoleRequest } from '@/types';

const { Title } = Typography;

export default function RolesPage() {
  const qc = useQueryClient();
  const [msgApi, ctxHolder] = message.useMessage();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editRole, setEditRole] = useState<RoleDto | null>(null);
  const [viewRole, setViewRole] = useState<RoleDto | null>(null);
  const [createForm] = Form.useForm<CreateRoleRequest>();
  const [editForm] = Form.useForm<CreateRoleRequest>();

  const { data, isLoading } = useQuery({
    queryKey: ['roles', page],
    queryFn: () => rolesApi.getAll(page, 20),
  });

  const { data: allPerms } = useQuery({
    queryKey: ['permissions-all'],
    queryFn: () => permissionsApi.getAllList(),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['roles'] });

  const createMutation = useMutation({
    mutationFn: (req: CreateRoleRequest) => rolesApi.create(req),
    onSuccess: () => {
      msgApi.success('Role created');
      setCreateOpen(false);
      createForm.resetFields();
      invalidate();
    },
    onError: () => msgApi.error('Failed to create role'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, req }: { id: number; req: CreateRoleRequest }) =>
      rolesApi.update(id, req),
    onSuccess: () => {
      msgApi.success('Role updated');
      setEditRole(null);
      editForm.resetFields();
      invalidate();
    },
    onError: () => msgApi.error('Failed to update role'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => rolesApi.delete(id),
    onSuccess: () => { msgApi.success('Role deleted'); invalidate(); },
    onError: () => msgApi.error('Failed to delete role'),
  });

  const activateMutation = useMutation({
    mutationFn: (id: number) => rolesApi.activate(id),
    onSuccess: () => { msgApi.success('Role activated'); invalidate(); },
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => rolesApi.deactivate(id),
    onSuccess: () => { msgApi.success('Role deactivated'); invalidate(); },
  });

  const filtered = (data?.content ?? []).filter(
    (r) => !search || r.name.toLowerCase().includes(search.toLowerCase()),
  );

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
      sorter: (a: RoleDto, b: RoleDto) => a.name.localeCompare(b.name),
    },
    { title: 'Description', dataIndex: 'description', key: 'description' },
    {
      title: 'Permissions',
      key: 'permissions',
      render: (_: unknown, r: RoleDto) => (
        <span>{r.permissions?.length ?? 0} permission(s)</span>
      ),
    },
    {
      title: 'Users',
      dataIndex: 'userCount',
      key: 'userCount',
      render: (c: number) => c ?? '—',
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => (
        <Tag color={active ? 'success' : 'default'}>
          {active ? 'Active' : 'Inactive'}
        </Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 180,
      render: (_: unknown, record: RoleDto) => (
        <Space size={4}>
          <Tooltip title="View">
            <Button size="small" icon={<EyeOutlined />} onClick={() => setViewRole(record)} />
          </Tooltip>
          <Tooltip title="Edit">
            <Button
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setEditRole(record);
                editForm.setFieldsValue({
                  name: record.name,
                  description: record.description,
                  permissions: record.permissions,
                });
              }}
            />
          </Tooltip>
          {record.active ? (
            <Tooltip title="Deactivate">
              <Button
                size="small"
                icon={<StopOutlined />}
                onClick={() => deactivateMutation.mutate(record.id)}
              />
            </Tooltip>
          ) : (
            <Tooltip title="Activate">
              <Button
                size="small"
                icon={<CheckOutlined />}
                onClick={() => activateMutation.mutate(record.id)}
              />
            </Tooltip>
          )}
          <Popconfirm
            title="Delete this role?"
            onConfirm={() => deleteMutation.mutate(record.id)}
            okText="Delete"
            okButtonProps={{ danger: true }}
          >
            <Tooltip title="Delete">
              <Button size="small" icon={<DeleteOutlined />} danger />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const permOptions = allPerms?.map((p) => ({ value: p.name, label: p.name })) ?? [];

  return (
    <div>
      {ctxHolder}
      <div className="flex items-center justify-between mb-4">
        <Title level={4} style={{ margin: 0 }}>
          Role Management
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
          New Role
        </Button>
      </div>

      <Input
        prefix={<SearchOutlined />}
        placeholder="Search roles…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="mb-4"
        style={{ maxWidth: 320 }}
        allowClear
      />

      <Table<RoleDto>
        dataSource={filtered}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: page + 1,
          total: data?.totalElements ?? 0,
          pageSize: 20,
          onChange: (p) => setPage(p - 1),
          showTotal: (t) => `${t} roles`,
        }}
      />

      {/* Create Role Modal */}
      <Modal
        title="Create New Role"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); createForm.resetFields(); }}
        onOk={() => createForm.submit()}
        confirmLoading={createMutation.isPending}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical" onFinish={(v) => createMutation.mutate(v)}>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="permissions" label="Permissions">
            <Select mode="multiple" placeholder="Select permissions" options={permOptions} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Role Modal */}
      <Modal
        title="Edit Role"
        open={!!editRole}
        onCancel={() => { setEditRole(null); editForm.resetFields(); }}
        onOk={() => editForm.submit()}
        confirmLoading={updateMutation.isPending}
        destroyOnClose
      >
        <Form
          form={editForm}
          layout="vertical"
          onFinish={(v) => editRole && updateMutation.mutate({ id: editRole.id, req: v })}
        >
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="permissions" label="Permissions">
            <Select mode="multiple" placeholder="Select permissions" options={permOptions} />
          </Form.Item>
        </Form>
      </Modal>

      {/* View Role Drawer */}
      <Drawer
        title={`Role: ${viewRole?.name}`}
        open={!!viewRole}
        onClose={() => setViewRole(null)}
        width={400}
      >
        {viewRole && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="ID">{viewRole.id}</Descriptions.Item>
            <Descriptions.Item label="Name">{viewRole.name}</Descriptions.Item>
            <Descriptions.Item label="Description">{viewRole.description ?? '—'}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color={viewRole.active ? 'success' : 'default'}>
                {viewRole.active ? 'Active' : 'Inactive'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Permissions">
              <Space wrap>
                {viewRole.permissions?.map((p) => (
                  <Tag key={p}>{p}</Tag>
                ))}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Created">
              {new Date(viewRole.createdAt).toLocaleString()}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  );
}
