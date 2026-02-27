import { useEffect, useState } from 'react';
import { List, Card, Tag, Spin, Empty } from 'antd';
import { useNavigate } from 'react-router-dom';
import { notificationApi, NotificationVo } from '../api/client';

export default function Notifications() {
  const [list, setList] = useState<NotificationVo[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const loadList = async () => {
    setLoading(true);
    try {
      const res = await notificationApi.list();
      if (res.data.code === 0 && Array.isArray(res.data.data)) {
        setList(res.data.data);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadList();
  }, []);

  const handleItemClick = async (item: NotificationVo) => {
    if (!item.isRead) {
      try {
        await notificationApi.markRead(item.id);
        setList((prev) =>
          prev.map((n) => (n.id === item.id ? { ...n, isRead: true } : n))
        );
        window.dispatchEvent(new CustomEvent('notification-read'));
      } catch {
        // ignore
      }
    }
    if (item.businessType === 'workflow' && item.businessId) {
      navigate(`/instances/${item.businessId}`);
      return;
    }
    if (item.businessType === 'notice' && item.businessId) {
      navigate(`/notices/${item.businessId}`);
      return;
    }
  };

  return (
    <Card title="站内消息">
      {loading && list.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin />
        </div>
      ) : list.length === 0 ? (
        <Empty description="暂无消息" />
      ) : (
        <List
          itemLayout="horizontal"
          dataSource={list}
          loading={loading}
          renderItem={(item) => (
            <List.Item
              key={item.id}
              style={{
                cursor: 'pointer',
                background: item.isRead ? undefined : 'rgba(22, 119, 255, 0.04)',
              }}
              onClick={() => handleItemClick(item)}
              extra={
                item.isRead ? (
                  <Tag>已读</Tag>
                ) : (
                  <Tag color="blue">未读</Tag>
                )
              }
            >
              <List.Item.Meta
                title={item.title}
                description={
                  item.createdAt
                    ? new Date(item.createdAt).toLocaleString()
                    : undefined
                }
              />
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}
