'use client';

import { useState } from 'react';
import { Card, Typography, Descriptions, Tag, Divider, Form, Input, Button, message, Progress } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { useAuth } from '@/hooks/useAuth';
import { securityApi } from '@/lib/api';

const { Title, Text } = Typography;

const strengthColor: Record<string, string> = {
  VERY_WEAK: '#ff4d4f',
  WEAK: '#ff7a45',
  FAIR: '#ffa940',
  GOOD: '#bae637',
  STRONG: '#52c41a',
  VERY_STRONG: '#00b96b',
};
const strengthPercent: Record<string, number> = {
  VERY_WEAK: 10, WEAK: 25, FAIR: 45, GOOD: 65, STRONG: 82, VERY_STRONG: 100,
};

export default function SettingsPage() {
  const { user } = useAuth();
  const [form] = Form.useForm();
  const [msgApi, ctxHolder] = message.useMessage();
  const [loading, setLoading] = useState(false);
  const [strength, setStrength] = useState<string | null>(null);
  const [strengthErrors, setStrengthErrors] = useState<string[]>([]);

  const handlePasswordChange = async (values: { oldPassword: string; newPassword: string }) => {
    setLoading(true);
    try {
      await securityApi.changePassword(values.oldPassword, values.newPassword);
      msgApi.success('Password changed successfully');
      form.resetFields();
      setStrength(null);
      setStrengthErrors([]);
    } catch {
      msgApi.error('Failed to change password. Check your current password and try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleNewPasswordChange = async (value: string) => {
    if (!value) { setStrength(null); setStrengthErrors([]); return; }
    try {
      const result = await securityApi.validatePassword(value);
      setStrength(result.strength);
      setStrengthErrors(result.errors);
    } catch {
      // ignore validation errors silently
    }
  };

  return (
    <div>
      {ctxHolder}
      <Title level={4} style={{ marginBottom: 24 }}>Settings</Title>

      <Card title="Your Profile" style={{ maxWidth: 600 }}>
        {user && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="Username">{user.username}</Descriptions.Item>
            <Descriptions.Item label="Name">
              {user.firstName} {user.lastName}
            </Descriptions.Item>
            <Descriptions.Item label="Email">{user.email}</Descriptions.Item>
            <Descriptions.Item label="Account Status">
              <Tag color={user.enabled ? 'success' : (!user.accountNonLocked ? 'error' : 'default')}>
                {!user.accountNonLocked ? 'LOCKED' : user.enabled ? 'ACTIVE' : 'DISABLED'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Roles">
              {user.roles.map((r) => (
                <Tag key={r} color="blue">{r}</Tag>
              ))}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Card>

      <Divider />

      <Card title="Change Password" style={{ maxWidth: 600 }}>
        <Form form={form} layout="vertical" onFinish={handlePasswordChange} autoComplete="off">
          <Form.Item
            label="Current Password"
            name="oldPassword"
            rules={[{ required: true, message: 'Please enter your current password' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Current password" />
          </Form.Item>

          <Form.Item
            label="New Password"
            name="newPassword"
            rules={[{ required: true, message: 'Please enter a new password' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="New password"
              onChange={(e) => handleNewPasswordChange(e.target.value)}
            />
          </Form.Item>

          {strength && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ marginBottom: 4 }}>
                <Text type="secondary">Strength: </Text>
                <Text style={{ color: strengthColor[strength] }}>{strength.replace('_', ' ')}</Text>
              </div>
              <Progress
                percent={strengthPercent[strength]}
                showInfo={false}
                strokeColor={strengthColor[strength]}
                size="small"
              />
              {strengthErrors.length > 0 && (
                <ul style={{ marginTop: 8, paddingLeft: 20, color: '#ff4d4f', fontSize: 13 }}>
                  {strengthErrors.map((e) => <li key={e}>{e}</li>)}
                </ul>
              )}
            </div>
          )}

          <Form.Item
            label="Confirm New Password"
            name="confirmPassword"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Please confirm your new password' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                  return Promise.reject(new Error('Passwords do not match'));
                },
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Confirm new password" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              Change Password
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Divider />

      <Card title="API Information" style={{ maxWidth: 600 }}>
        <Descriptions column={1} size="small">
          <Descriptions.Item label="Backend URL">
            <Text code>{process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="API Base Path">
            <Text code>/api/v1</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Swagger UI">
            <a
              href={`${process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'}/swagger-ui.html`}
              target="_blank"
              rel="noreferrer"
            >
              Open Swagger UI
            </a>
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  );
}
