import { useState, useEffect, useRef } from 'react';
import { Outlet, useNavigate, useLocation, Link } from 'react-router-dom';
import { Layout as AntLayout, Menu, Button, Space, Typography, Badge } from 'antd';
import type { MenuProps } from 'antd';
import { LogoutOutlined, UnorderedListOutlined, FileTextOutlined, FormOutlined, DollarOutlined, BellOutlined, TeamOutlined, BarChartOutlined, MessageOutlined, ApiOutlined } from '@ant-design/icons';
import { useAuth } from '../stores/auth';
import { notificationApi } from '../api/client';

const { Header, Sider, Content } = AntLayout;

const menuItems: (unreadCount: number) => MenuProps['items'] = (unreadCount) => [
  { key: '/tasks/pending', icon: <UnorderedListOutlined />, label: <Link to="/tasks/pending">我的待办</Link> },
  { key: '/instances/my', icon: <FileTextOutlined />, label: <Link to="/instances/my">我发起的</Link> },
  {
    key: '/notifications',
    icon: (
      <Badge count={unreadCount} size="small" offset={[4, 0]}>
        <MessageOutlined />
      </Badge>
    ),
    label: <Link to="/notifications">消息</Link>,
  },
  { key: '/notices', icon: <BellOutlined />, label: <Link to="/notices">公告</Link> },
  { key: '/org', icon: <TeamOutlined />, label: <Link to="/org">组织管理</Link> },
  { key: '/stats', icon: <BarChartOutlined />, label: <Link to="/stats">报表统计</Link> },
  { key: '/connection-demo', icon: <ApiOutlined />, label: <Link to="/connection-demo">长/短连接演示</Link> },
  { key: '/leave/start', icon: <FormOutlined />, label: <Link to="/leave/start">发起请假</Link> },
  { key: '/expense/start', icon: <DollarOutlined />, label: <Link to="/expense/start">发起报销</Link> },
];

export default function Layout() {
  const [collapsed, setCollapsed] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const loadUnreadCount = async () => {
    try {
      const res = await notificationApi.unreadCount();
      if (res.data.code === 0 && typeof res.data.data === 'number') {
        setUnreadCount(res.data.data);
      }
    } catch {
      // ignore
    }
  };

  const prevPathnameRef = useRef<string | null>(null);

  // 仅首次进入拉未读数，或从消息页离开时再拉一次（避免 Strict Mode 下重复请求）
  useEffect(() => {
    const path = location.pathname;
    if (prevPathnameRef.current === null) {
      prevPathnameRef.current = path;
      loadUnreadCount();
      return;
    }
    if (prevPathnameRef.current === '/notifications' && path !== '/notifications') {
      prevPathnameRef.current = path;
      loadUnreadCount();
      return;
    }
    prevPathnameRef.current = path;
  }, [location.pathname]);

  // 消息页标记已读后刷新红点
  useEffect(() => {
    const onRead = () => loadUnreadCount();
    window.addEventListener('notification-read', onRead);
    return () => window.removeEventListener('notification-read', onRead);
  }, []);

  const selectedKey =
    location.pathname.startsWith('/notifications')
      ? '/notifications'
      : location.pathname.startsWith('/notices')
        ? '/notices'
        : location.pathname.startsWith('/instances')
          ? '/instances/my'
          : location.pathname.startsWith('/org')
            ? '/org'
            : location.pathname;

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
        <div style={{ height: 32, margin: 16, color: '#fff', textAlign: 'center', lineHeight: '32px' }}>
          OA
        </div>
        <Menu
          theme="dark"
          selectedKeys={[selectedKey]}
          mode="inline"
          items={menuItems(unreadCount)}
        />
      </Sider>
      <AntLayout>
        <Header style={{ padding: '0 24px', background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}>
          <Space>
            <Typography.Text>{user?.name ?? user?.username}</Typography.Text>
            <Button type="text" icon={<LogoutOutlined />} onClick={() => logout().then(() => navigate('/login'))}>
              退出
            </Button>
          </Space>
        </Header>
        <Content style={{ margin: 24, background: '#fff', padding: 24, minHeight: 280 }}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
}
