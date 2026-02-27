import { useEffect, useState } from 'react';
import { Card, Spin, Button, message, Space, Modal, Form, Input, Switch, Popconfirm, Tag } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import { noticeApi, NoticeVo } from '../api/client';
import { useAuth } from '../stores/auth';

export default function NoticeDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [detail, setDetail] = useState<NoticeVo | null>(null);
  const [loading, setLoading] = useState(true);
  const [editVisible, setEditVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const isAuthor = user && detail && detail.publisherUserId === user.userId;
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;
  const canEdit = detail && (detail.status === 'draft' ? isAuthor : (isAuthor || isAdmin));
  const canPublishDraft = detail?.status === 'draft' && isAuthor;
  const canUnpublish = detail?.status === 'published' && (isAuthor || isAdmin);
  const canDelete = detail && (detail.status === 'draft' ? isAuthor : isAdmin);

  useEffect(() => {
    const noticeId = id ? Number(id) : NaN;
    if (!Number.isInteger(noticeId) || noticeId <= 0) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const res = await noticeApi.getById(noticeId, true);
        if (res.data.code === 0 && res.data.data && !cancelled) {
          setDetail(res.data.data);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [id]);

  const handleMarkRead = async () => {
    const noticeId = id ? Number(id) : NaN;
    if (!Number.isInteger(noticeId) || noticeId <= 0) return;
    try {
      await noticeApi.markRead(noticeId);
      message.success('已标记为已读');
      if (detail) setDetail({ ...detail, read: true });
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '操作失败');
    }
  };

  const handleEditOk = async () => {
    if (!detail) return;
    const v = await form.validateFields();
    setSubmitting(true);
    try {
      const res = await noticeApi.update(detail.id, {
        title: v.title,
        content: v.content,
        isTop: v.isTop ?? false,
      });
      if (res.data.code !== 0) {
        message.error(res.data.message || '更新失败');
        return;
      }
      message.success('已更新');
      setDetail({ ...detail, ...v, isTop: v.isTop ? 1 : 0 });
      setEditVisible(false);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '更新失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePublishDraft = async () => {
    if (!detail) return;
    try {
      const res = await noticeApi.publishDraft(detail.id);
      if (res.data.code !== 0) {
        message.error(res.data.message || '发布失败');
        return;
      }
      message.success('已发布');
      const res2 = await noticeApi.getById(detail.id);
      if (res2.data.code === 0 && res2.data.data) setDetail(res2.data.data);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '发布失败');
    }
  };

  const handleUnpublish = async () => {
    if (!detail) return;
    try {
      const res = await noticeApi.unpublish(detail.id);
      if (res.data.code !== 0) {
        message.error(res.data.message || '下架失败');
        return;
      }
      message.success('已下架');
      const res2 = await noticeApi.getById(detail.id);
      if (res2.data.code === 0 && res2.data.data) setDetail(res2.data.data);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '下架失败');
    }
  };

  const handleDelete = async () => {
    if (!detail) return;
    try {
      const res = await noticeApi.delete(detail.id);
      if (res.data.code !== 0) {
        message.error(res.data.message || '删除失败');
        return;
      }
      message.success('已删除');
      navigate('/notices');
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '删除失败');
    }
  };

  if (loading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin />
        </div>
      </Card>
    );
  }

  if (!detail) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: 24 }}>公告不存在或已删除</div>
        <div style={{ textAlign: 'center' }}>
          <Button type="primary" onClick={() => navigate('/notices')}>
            返回列表
          </Button>
        </div>
      </Card>
    );
  }

  return (
    <Card
      title={detail.title}
      extra={
        <Space>
          {detail.read === false && detail.status === 'published' && (
            <Button type="link" size="small" onClick={handleMarkRead}>
              标记已读
            </Button>
          )}
          {canEdit && (
            <Button type="link" size="small" onClick={() => { form.setFieldsValue({ title: detail.title, content: detail.content, isTop: detail.isTop === 1 }); setEditVisible(true); }}>
              编辑
            </Button>
          )}
          {canPublishDraft && (
            <Button type="link" size="small" onClick={handlePublishDraft}>
              发布
            </Button>
          )}
          {canUnpublish && (
            <Button type="link" size="small" onClick={handleUnpublish}>
              下架
            </Button>
          )}
          {canDelete && (
            <Popconfirm title="确定删除？" onConfirm={handleDelete}>
              <Button type="link" size="small" danger>
                删除
              </Button>
            </Popconfirm>
          )}
          <Button type="link" size="small" onClick={() => navigate('/notices')}>
            返回列表
          </Button>
        </Space>
      }
    >
      <div style={{ color: 'rgba(0,0,0,0.65)', marginBottom: 16 }}>
        {detail.status === 'draft' && <Tag color="default">草稿</Tag>}
        {detail.publishAt && `发布时间：${new Date(detail.publishAt).toLocaleString()}`}
        {detail.createdAt && !detail.publishAt && `创建时间：${new Date(detail.createdAt).toLocaleString()}`}
      </div>
      <div style={{ whiteSpace: 'pre-wrap' }}>{detail.content || '（无正文）'}</div>
      <Modal
        title="编辑公告"
        open={editVisible}
        onOk={handleEditOk}
        onCancel={() => setEditVisible(false)}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="公告标题" />
          </Form.Item>
          <Form.Item name="content" label="内容" rules={[{ required: true, message: '请输入内容' }]}>
            <Input.TextArea rows={6} placeholder="公告内容" />
          </Form.Item>
          <Form.Item name="isTop" label="置顶" valuePropName="checked" initialValue={false}>
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}

