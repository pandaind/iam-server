'use client';

import { useCallback, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import {
  Layout,
  Menu,
  Avatar,
  Dropdown,
  Typography,
  Badge,
  Space,
} from 'antd';
import {
  DashboardOutlined,
  TeamOutlined,
  SafetyCertificateOutlined,
  KeyOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  SettingOutlined,
  LogoutOutlined,
  UserOutlined,
  BellOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { useAuth } from '@/hooks/useAuth';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
  { key: '/users', icon: <TeamOutlined />, label: 'Users' },
  { key: '/roles', icon: <SafetyCertificateOutlined />, label: 'Roles' },
  { key: '/permissions', icon: <KeyOutlined />, label: 'Permissions' },
  { key: '/sessions', icon: <ClockCircleOutlined />, label: 'Sessions' },
  { key: '/audit', icon: <FileTextOutlined />, label: 'Audit Logs' },
  { key: '/settings', icon: <SettingOutlined />, label: 'Settings' },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = useState(false);
  const router = useRouter();
  const pathname = usePathname();
  const { user, logout } = useAuth();

  const handleLogout = useCallback(async () => {
    await logout();
    router.replace('/login');
  }, [logout, router]);

  const selectedKey =
    menuItems.find((item) => pathname.startsWith(item.key))?.key ?? '/dashboard';

  const userMenuItems = [
    {
      key: 'profile',
      label: 'My Profile',
      icon: <UserOutlined />,
      onClick: () => router.push(`/users/${user?.id}`),
    },
    { type: 'divider' as const },
    {
      key: 'logout',
      label: 'Sign Out',
      icon: <LogoutOutlined />,
      danger: true,
      onClick: handleLogout,
    },
  ];

  return (
    <ProtectedRoute>
      <Layout style={{ minHeight: '100vh' }}>
        <Sider
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          trigger={null}
          width={220}
          style={{ background: '#001529' }}
        >
          <div
            className="flex items-center justify-center py-4"
            style={{ borderBottom: '1px solid rgba(255,255,255,0.1)' }}
          >
            <Text
              strong
              style={{
                color: '#ffffff',
                fontSize: collapsed ? 14 : 16,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
              }}
            >
              {collapsed ? 'IAM' : 'IAM Console'}
            </Text>
          </div>
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[selectedKey]}
            items={menuItems}
            onClick={({ key }) => router.push(key)}
            style={{ marginTop: 8 }}
          />
        </Sider>

        <Layout>
          <Header
            style={{
              background: '#ffffff',
              padding: '0 24px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              borderBottom: '1px solid #f0f0f0',
              boxShadow: '0 1px 4px rgba(0,21,41,0.08)',
            }}
          >
            <button
              onClick={() => setCollapsed(!collapsed)}
              className="text-lg text-gray-600 hover:text-blue-500 transition-colors border-none bg-transparent cursor-pointer"
              aria-label="Toggle sidebar"
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </button>

            <Space size={16}>
              <Badge count={0} showZero={false}>
                <BellOutlined style={{ fontSize: 18, cursor: 'pointer' }} />
              </Badge>
              <Dropdown menu={{ items: userMenuItems }} trigger={['click']}>
                <Space className="cursor-pointer" style={{ gap: 8 }}>
                  <Avatar size="small" icon={<UserOutlined />} />
                  <Text style={{ fontSize: 14 }}>
                    {user?.firstName ?? user?.username}
                  </Text>
                </Space>
              </Dropdown>
            </Space>
          </Header>

          <Content style={{ margin: 24, minHeight: 280 }}>
            {children}
          </Content>
        </Layout>
      </Layout>
    </ProtectedRoute>
  );
}
