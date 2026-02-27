import { useEffect, useState } from 'react';
import { List, Card, Tag, Spin, Empty, Button, Modal, Form, Input, Switch, message, Tabs, Space, Popconfirm } from 'antd';
import { useNavigate } from 'react-router-dom';
import { noticeApi, NoticeVo } from '../api/client';
import { useAuth } from '../stores/auth';

export default function NoticeList() {
  const [list, setList] = useState<NoticeVo[]>([]);
  const [drafts, setDrafts] = useState<NoticeVo[]>([]);
  const [loading, setLoading] = useState(true);
  const [draftsLoading, setDraftsLoading] = useState(false);
  const [tab, setTab] = useState<'published' | 'drafts'>('published');
  const [publishVisible, setPublishVisible] = useState(false);
  const [editModal, setEditModal] = useState<NoticeVo | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();
  const [editForm] = Form.useForm();
  const navigate = useNavigate();
  const { user } = useAuth();
  const canPublish = user?.roles?.includes('ADMIN') ?? false;

  const loadList = async () => {
    setLoading(true);
    try {
      const res = await noticeApi.list();
      if (res.data.code === 0 && Array.isArray(res.data.data)) {
        setList(res.data.data);
      }
    } finally {
      setLoading(false);
    }
  };

  const loadDrafts = async () => {
    if (!canPublish) return;
    setDraftsLoading(true);
    try {
      const res = await noticeApi.myDrafts();
      if (res.data.code === 0 && Array.isArray(res.data.data)) setDrafts(res.data.data);
    } finally {
      setDraftsLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await loadList();
      if (cancelled) return;
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (tab === 'drafts' && canPublish) loadDrafts();
  }, [tab, canPublish]);

  const handlePublish = async () => {
    const v = await form.validateFields();
    setSubmitting(true);
    try {
      const res = await noticeApi.publish({
        title: v.title,
        content: v.content,
        isTop: v.isTop ?? false,
      });
      if (res.data.code !== 0) {
        message.error(res.data.message || '发布失败');
        return;
      }
      message.success('发布成功');
      setPublishVisible(false);
      form.resetFields();
      loadList();
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '发布失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSaveDraft = async () => {
    try {
      const v = form.getFieldsValue();
      if (!v.title?.trim()) {
        message.error('请输入标题');
        return;
      }
      const res = await noticeApi.saveDraft({
        title: v.title,
        content: v.content,
        isTop: v.isTop ?? false,
      });
      if (res.data.code !== 0) {
        message.error(res.data.message || '保存失败');
        return;
      }
      message.success('草稿已保存');
      setPublishVisible(false);
      form.resetFields();
      if (tab === 'drafts') loadDrafts();
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '保存失败');
    }
  };

  const handleEditOk = async () => {
    if (!editModal) return;
    const v = await editForm.validateFields();
    setSubmitting(true);
    try {
      const res = await noticeApi.update(editModal.id, {
        title: v.title,
        content: v.content,
        isTop: v.isTop ?? false,
      });
      if (res.data.code !== 0) {
        message.error(res.data.message || '更新失败');
        return;
      }
      message.success('已更新');
      setEditModal(null);
      editForm.resetFields();
      loadList();
      if (tab === 'drafts') loadDrafts();
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '更新失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePublishDraft = async (item: NoticeVo) => {
    try {
      const res = await noticeApi.publishDraft(item.id);
      if (res.data.code !== 0) {
        message.error(res.data.message || '发布失败');
        return;
      }
      message.success('已发布');
      loadList();
      loadDrafts();
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '发布失败');
    }
  };

  const handleDelete = async (item: NoticeVo) => {
    try {
      const res = await noticeApi.delete(item.id);
      if (res.data.code !== 0) {
        message.error(res.data.message || '删除失败');
        return;
      }
      message.success('已删除');
      loadList();
      loadDrafts();
      if (editModal?.id === item.id) setEditModal(null);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '删除失败');
    }
  };

  if (loading && list.length === 0) {
    return (
      <Card
        title="公告"
        extra={canPublish && (
          <Button type="primary" onClick={() => setPublishVisible(true)}>
            发布公告
          </Button>
        )}
      >
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin />
        </div>
      </Card>
    );
  }

  return (
    <>
      <Card
        title="公告"
        extra={canPublish && (
          <Button type="primary" onClick={() => setPublishVisible(true)}>
            发布公告
          </Button>
        )}
      >
        <Tabs
          activeKey={tab}
          onChange={(k) => setTab(k as 'published' | 'drafts')}
          items={[
            {
              key: 'published',
              label: '已发布',
              children:
                list.length === 0 ? (
                  <Empty description="暂无公告" />
                ) : (
                  <List
                    itemLayout="vertical"
                    dataSource={list}
                    loading={loading}
                    renderItem={(item) => (
                      <List.Item
                        key={item.id}
                        style={{ cursor: 'pointer' }}
                        onClick={() => navigate(`/notices/${item.id}`)}
                        extra={
                          item.read === false ? (
                            <Tag color="blue">未读</Tag>
                          ) : (
                            <Tag>已读</Tag>
                          )
                        }
                      >
                        <List.Item.Meta
                          title={
                            <>
                              {item.isTop === 1 && <Tag color="red">置顶</Tag>}
                              {item.title}
                            </>
                          }
                          description={
                            item.publishAt
                              ? `发布时间：${new Date(item.publishAt).toLocaleString()}`
                              : item.createdAt
                                ? `创建时间：${new Date(item.createdAt).toLocaleString()}`
                                : undefined
                          }
                        />
                      </List.Item>
                    )}
                  />
                ),
            },
            {
              key: 'drafts',
              label: '草稿箱',
              children:
                !canPublish ? (
                  <Empty description="无权限" />
                ) : draftsLoading && drafts.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: 48 }}>
                    <Spin />
                  </div>
                ) : drafts.length === 0 ? (
                  <Empty description="暂无草稿" />
                ) : (
                  <List
                    itemLayout="vertical"
                    dataSource={drafts}
                    renderItem={(item) => (
                      <List.Item
                        key={item.id}
                        style={{ cursor: 'pointer' }}
                        onClick={() => navigate(`/notices/${item.id}`)}
                        extra={
                          <Space onClick={(e) => e.stopPropagation()}>
                            <Button
                              type="link"
                              size="small"
                              onClick={() => {
                                editForm.setFieldsValue({
                                  title: item.title,
                                  content: item.content,
                                  isTop: item.isTop === 1,
                                });
                                setEditModal(item);
                              }}
                            >
                              编辑
                            </Button>
                            <Button type="link" size="small" onClick={() => handlePublishDraft(item)}>
                              发布
                            </Button>
                            <Popconfirm
                              title="确定删除该草稿？"
                              onConfirm={() => handleDelete(item)}
                            >
                              <Button type="link" size="small" danger>
                                删除
                              </Button>
                            </Popconfirm>
                          </Space>
                        }
                      >
                        <List.Item.Meta
                          title={item.title || '（无标题）'}
                          description={
                            item.updatedAt
                              ? `更新时间：${new Date(item.updatedAt).toLocaleString()}`
                              : item.createdAt
                                ? `创建时间：${new Date(item.createdAt).toLocaleString()}`
                                : undefined
                          }
                        />
                      </List.Item>
                    )}
                  />
                ),
            },
          ]}
        />
      </Card>
      <Modal
        title="发布公告"
        open={publishVisible}
        onCancel={() => { setPublishVisible(false); form.resetFields(); }}
        destroyOnClose
        width={560}
        footer={
          <Space>
            <Button onClick={() => { setPublishVisible(false); form.resetFields(); }}>
              取消
            </Button>
            <Button onClick={handleSaveDraft}>保存草稿</Button>
            <Button type="primary" loading={submitting} onClick={handlePublish}>
              发布
            </Button>
          </Space>
        }
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
      <Modal
        title="编辑公告"
        open={!!editModal}
        onOk={handleEditOk}
        onCancel={() => { setEditModal(null); editForm.resetFields(); }}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form form={editForm} layout="vertical">
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
    </>
  );
}
