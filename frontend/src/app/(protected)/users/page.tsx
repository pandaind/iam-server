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
  UnlockOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { usersApi, rolesApi } from '@/lib/api';
import type { UserDto, CreateUserRequest, UpdateUserRequest } from '@/types';

const { Title } = Typography;

function getUserStatus(u: UserDto): { label: string; color: string } {
  if (!u.accountNonLocked) return { label: 'LOCKED', color: 'error' };
  if (!u.enabled) return { label: 'DISABLED', color: 'default' };
  return { label: 'ACTIVE', color: 'success' };
}

export default function UsersPage() {
  const qc = useQueryClient();
  const [msgApi, ctxHolder] = message.useMessage();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editUser, setEditUser] = useState<UserDto | null>(null);
  const [viewUser, setViewUser] = useState<UserDto | null>(null);
  const [createForm] = Form.useForm<CreateUserRequest>();
  const [editForm] = Form.useForm<UpdateUserRequest>();

  const { data, isLoading } = useQuery({
    queryKey: ['users', page],
    queryFn: () => usersApi.getAll(page, 20),
  });

  const { data: roles } = useQuery({
    queryKey: ['roles-active'],
    queryFn: () => rolesApi.getActive(),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['users'] });

  const createMutation = useMutation({
    mutationFn: (req: CreateUserRequest) => usersApi.create(req),
    onSuccess: () => {
      msgApi.success('User created');
      setCreateOpen(false);
      createForm.resetFields();
      invalidate();
    },
    onError: () => msgApi.error('Failed to create user'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, req }: { id: number; req: UpdateUserRequest }) =>
      usersApi.update(id, req),
    onSuccess: () => {
      msgApi.success('User updated');
      setEditUser(null);
      editForm.resetFields();
      invalidate();
    },
    onError: () => msgApi.error('Failed to update user'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => usersApi.delete(id),
    onSuccess: () => {
      msgApi.success('User deleted');
      invalidate();
    },
    onError: () => msgApi.error('Failed to delete user'),
  });

  const enableMutation = useMutation({
    mutationFn: (id: number) => usersApi.enable(id),
    onSuccess: () => { msgApi.success('User enabled'); invalidate(); },
  });

  const disableMutation = useMutation({
    mutationFn: (id: number) => usersApi.disable(id),
    onSuccess: () => { msgApi.success('User disabled'); invalidate(); },
  });

  const unlockMutation = useMutation({
    mutationFn: (id: number) => usersApi.unlock(id),
    onSuccess: () => { msgApi.success('User unlocked'); invalidate(); },
  });

  const filtered = (data?.content ?? []).filter(
    (u) =>
      !search ||
      u.username.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase()),
  );

  const columns = [
    {
      title: 'Username',
      dataIndex: 'username',
      key: 'username',
      sorter: (a: UserDto, b: UserDto) => a.username.localeCompare(b.username),
    },
    {
      title: 'Name',
      key: 'name',
      render: (_: unknown, r: UserDto) => `${r.firstName} ${r.lastName}`,
    },
    { title: 'Email', dataIndex: 'email', key: 'email' },
    {
      title: 'Roles',
      key: 'roles',
      render: (_: unknown, r: UserDto) =>
        r.roles.map((role) => (
          <Tag key={role} color="blue">
            {role}
          </Tag>
        )),
    },
    {
      title: 'Status',
      key: 'status',
      render: (_: unknown, r: UserDto) => {
        const s = getUserStatus(r);
        return <Tag color={s.color}>{s.label}</Tag>;
      },
    },
    {
      title: 'Last Login',
      dataIndex: 'lastLoginAt',
      key: 'lastLoginAt',
      render: (ts: string) => (ts ? new Date(ts).toLocaleDateString() : '—'),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 220,
      render: (_: unknown, record: UserDto) => (
        <Space size={4}>
          <Tooltip title="View">
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => setViewUser(record)}
            />
          </Tooltip>
          <Tooltip title="Edit">
            <Button
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setEditUser(record);
                editForm.setFieldsValue({
                  username: record.username,
                  email: record.email,
                  firstName: record.firstName,
                  lastName: record.lastName,
                  roles: record.roles,
                });
              }}
            />
          </Tooltip>
          {record.enabled ? (
            <Tooltip title="Disable">
              <Button
                size="small"
                icon={<StopOutlined />}
                danger
                onClick={() => disableMutation.mutate(record.id)}
              />
            </Tooltip>
          ) : (
            <Tooltip title="Enable">
              <Button
                size="small"
                icon={<CheckOutlined />}
                onClick={() => enableMutation.mutate(record.id)}
              />
            </Tooltip>
          )}
          {!record.accountNonLocked && (
            <Tooltip title="Unlock">
              <Button
                size="small"
                icon={<UnlockOutlined />}
                onClick={() => unlockMutation.mutate(record.id)}
              />
            </Tooltip>
          )}
          <Popconfirm
            title="Delete this user?"
            description="This action cannot be undone."
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

  return (
    <div>
      {ctxHolder}
      <div className="flex items-center justify-between mb-4">
        <Title level={4} style={{ margin: 0 }}>
          User Management
        </Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateOpen(true)}
        >
          New User
        </Button>
      </div>

      <Input
        prefix={<SearchOutlined />}
        placeholder="Search by username or email…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="mb-4"
        style={{ maxWidth: 360 }}
        allowClear
      />

      <Table<UserDto>
        dataSource={filtered}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: page + 1,
          total: data?.totalElements ?? 0,
          pageSize: 20,
          onChange: (p) => setPage(p - 1),
          showTotal: (t) => `${t} users`,
        }}
        scroll={{ x: 800 }}
      />

      {/* Create User Modal */}
      <Modal
        title="Create New User"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); createForm.resetFields(); }}
        onOk={() => createForm.submit()}
        confirmLoading={createMutation.isPending}
        destroyOnClose
      >
        <Form
          form={createForm}
          layout="vertical"
          onFinish={(vals) => createMutation.mutate(vals)}
        >
          <Form.Item name="username" label="Username" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="email"
            label="Email"
            rules={[{ required: true }, { type: 'email' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="password"
            label="Password"
            rules={[{ required: true, min: 8 }]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item name="firstName" label="First Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="lastName" label="Last Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="roles" label="Roles">
            <Select
              mode="multiple"
              placeholder="Select roles"
              options={roles?.map((r) => ({ value: r.name, label: r.name }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit User Modal */}
      <Modal
        title="Edit User"
        open={!!editUser}
        onCancel={() => { setEditUser(null); editForm.resetFields(); }}
        onOk={() => editForm.submit()}
        confirmLoading={updateMutation.isPending}
        destroyOnClose
      >
        <Form
          form={editForm}
          layout="vertical"
          onFinish={(vals) =>
            editUser && updateMutation.mutate({ id: editUser.id, req: vals })
          }
        >
          <Form.Item name="firstName" label="First Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="lastName" label="Last Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="email"
            label="Email"
            rules={[{ required: true }, { type: 'email' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="roles" label="Roles">
            <Select
              mode="multiple"
              placeholder="Select roles"
              options={roles?.map((r) => ({ value: r.name, label: r.name }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* View User Drawer */}
      <Drawer
        title={`User: ${viewUser?.username}`}
        open={!!viewUser}
        onClose={() => setViewUser(null)}
        width={480}
      >
        {viewUser && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="ID">{viewUser.id}</Descriptions.Item>
            <Descriptions.Item label="Username">{viewUser.username}</Descriptions.Item>
            <Descriptions.Item label="Name">
              {viewUser.firstName} {viewUser.lastName}
            </Descriptions.Item>
            <Descriptions.Item label="Email">{viewUser.email}</Descriptions.Item>
            <Descriptions.Item label="Status">
              {(() => { const s = getUserStatus(viewUser); return <Tag color={s.color}>{s.label}</Tag>; })()}
            </Descriptions.Item>
            <Descriptions.Item label="Account Locked">
              {viewUser.accountNonLocked ? 'No' : 'Yes'}
            </Descriptions.Item>
            <Descriptions.Item label="Roles">
              {viewUser.roles.map((r) => (
                <Tag key={r} color="blue">{r}</Tag>
              ))}
            </Descriptions.Item>
            <Descriptions.Item label="Created">
              {new Date(viewUser.createdAt).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="Last Login">
              {viewUser.lastLoginAt
                ? new Date(viewUser.lastLoginAt).toLocaleString()
                : '—'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  );
}
