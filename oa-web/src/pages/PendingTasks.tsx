import { useEffect, useState } from 'react';
import { Table, Card, Button, Tag, Modal, Input, message, Space, Segmented } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { workflowApi, TaskDetailVo } from '../api/client';

const statusMap: Record<string, { color: string; text: string }> = {
  pending: { color: 'blue', text: '待处理' },
  approved: { color: 'green', text: '已通过' },
  rejected: { color: 'red', text: '已驳回' },
};

const leaveTypeMap: Record<string, string> = {
  annual: '年假',
  sick: '病假',
  personal: '事假',
  other: '其他',
};

function businessSummary(row: TaskDetailVo): string {
  if (row.businessType === 'leave' && (row.leaveType != null || row.leaveDays != null)) {
    const type = leaveTypeMap[row.leaveType ?? ''] ?? row.leaveType ?? '请假';
    return `${type} ${row.leaveDays ?? '-'}天${row.leaveReason ? ` · ${row.leaveReason}` : ''}`;
  }
  if (row.businessType === 'expense' && (row.expenseAmount != null || row.expenseType != null)) {
    return `${row.expenseType ?? '报销'} ${row.expenseAmount ?? 0}元${row.expenseDescription ? ` · ${row.expenseDescription}` : ''}`;
  }
  return row.title ?? '-';
}

type TaskStatusFilter = 'pending' | 'approved' | 'rejected';

export default function PendingTasks() {
  const [list, setList] = useState<TaskDetailVo[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<TaskStatusFilter>('pending');
  const [actModal, setActModal] = useState<{ task: TaskDetailVo; action: 'approve' | 'reject' } | null>(null);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const res = statusFilter === 'pending'
        ? await workflowApi.pendingTasks()
        : await workflowApi.myTasks(statusFilter);
      if (res.data.code === 0 && Array.isArray(res.data.data)) {
        setList(res.data.data);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [statusFilter]);

  const handleAct = async () => {
    if (!actModal) return;
    setSubmitting(true);
    try {
      if (actModal.action === 'approve') {
        await workflowApi.approve(actModal.task.id, comment || undefined);
        message.success('已通过');
      } else {
        await workflowApi.reject(actModal.task.id, comment || undefined);
        message.success('已驳回');
      }
      setActModal(null);
      setComment('');
      load();
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<TaskDetailVo> = [
    { title: '任务ID', dataIndex: 'id', width: 72 },
    { title: '标题', dataIndex: 'title', ellipsis: true, width: 160, render: (_, row) => row.title || '-' },
    {
      title: '业务详情',
      key: 'business',
      ellipsis: true,
      render: (_, row) => businessSummary(row),
    },
    { title: '类型', dataIndex: 'businessType', width: 80, render: (t: string) => (t === 'leave' ? '请假' : t === 'expense' ? '报销' : t || '-') },
    { title: '节点', dataIndex: 'nodeName', key: 'nodeName', width: 110, render: (_: unknown, row: TaskDetailVo) => row.nodeName || row.nodeKey || '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 88,
      render: (s: string) => {
        const m = statusMap[s] ?? { color: 'default', text: s };
        return <Tag color={m.color}>{m.text}</Tag>;
      },
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 168, render: (v: string) => v && new Date(v).toLocaleString() },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_, row) =>
        row.status === 'pending' ? (
          <Space>
            <Button type="link" size="small" onClick={() => setActModal({ task: row, action: 'approve' })}>
              通过
            </Button>
            <Button type="link" size="small" danger onClick={() => setActModal({ task: row, action: 'reject' })}>
              驳回
            </Button>
          </Space>
        ) : null,
    },
  ];

  return (
    <>
      <Card
        title="我的待办"
        extra={
          <Segmented
            value={statusFilter}
            onChange={(v) => setStatusFilter(v as TaskStatusFilter)}
            options={[
              { value: 'pending', label: '待处理' },
              { value: 'approved', label: '已通过' },
              { value: 'rejected', label: '已驳回' },
            ]}
          />
        }
      >
        <Table rowKey="id" columns={columns} dataSource={list} loading={loading} pagination={{ pageSize: 10 }} />
      </Card>
      <Modal
        title={actModal?.action === 'approve' ? '通过' : '驳回'}
        open={!!actModal}
        onOk={handleAct}
        onCancel={() => { setActModal(null); setComment(''); }}
        confirmLoading={submitting}
        okText="确定"
      >
        <Input.TextArea
          placeholder="审批意见（选填）"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          rows={3}
        />
      </Modal>
    </>
  );
}
