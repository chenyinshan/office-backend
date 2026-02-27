import { useEffect, useState, useMemo } from 'react';
import { Table, Card, Tag, Segmented } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { workflowApi, InstanceDetailVo } from '../api/client';

const statusMap: Record<string, { color: string; text: string }> = {
  draft: { color: 'default', text: '草稿' },
  running: { color: 'processing', text: '审批中' },
  completed: { color: 'success', text: '已完成' },
  rejected: { color: 'error', text: '已驳回' },
  cancelled: { color: 'default', text: '已取消' },
};

const leaveTypeMap: Record<string, string> = {
  annual: '年假',
  sick: '病假',
  personal: '事假',
  other: '其他',
};

function businessSummary(row: InstanceDetailVo): string {
  if (row.businessType === 'leave' && (row.leaveType != null || row.leaveDays != null)) {
    const type = leaveTypeMap[row.leaveType ?? ''] ?? row.leaveType ?? '请假';
    return `${type} ${row.leaveDays ?? '-'}天${row.leaveReason ? ` · ${row.leaveReason}` : ''}`;
  }
  if (row.businessType === 'expense' && (row.expenseAmount != null || row.expenseType != null)) {
    return `${row.expenseType ?? '报销'} ${row.expenseAmount ?? 0}元${row.expenseDescription ? ` · ${row.expenseDescription}` : ''}`;
  }
  return row.title ?? '-';
}

type InstanceStatusFilter = 'all' | 'running' | 'completed' | 'rejected';

export default function MyInstances() {
  const navigate = useNavigate();
  const [list, setList] = useState<InstanceDetailVo[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<InstanceStatusFilter>('all');

  useEffect(() => {
    workflowApi.myInstances().then((res) => {
      if (res.data.code === 0 && Array.isArray(res.data.data)) {
        setList(res.data.data);
      }
      setLoading(false);
    });
  }, []);

  const filteredList = useMemo(() => {
    if (statusFilter === 'all') return list;
    return list.filter((item) => item.status === statusFilter);
  }, [list, statusFilter]);

  const columns: ColumnsType<InstanceDetailVo> = [
    { title: 'ID', dataIndex: 'id', width: 72 },
    { title: '标题', dataIndex: 'title', ellipsis: true, width: 180 },
    {
      title: '业务详情',
      key: 'business',
      ellipsis: true,
      render: (_, row) => businessSummary(row),
    },
    { title: '类型', dataIndex: 'businessType', width: 80, render: (t: string) => (t === 'leave' ? '请假' : t === 'expense' ? '报销' : t || '-') },
    {
      title: '状态',
      dataIndex: 'status',
      width: 88,
      render: (s: string) => {
        const m = statusMap[s] ?? { color: 'default', text: s };
        return <Tag color={m.color}>{m.text}</Tag>;
      },
    },
    {
      title: '当前节点',
      key: 'currentNode',
      width: 110,
      render: (_: unknown, row: InstanceDetailVo) =>
        row.status === 'running' && row.currentNodeName ? row.currentNodeName : '-',
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 168, render: (v: string) => v && new Date(v).toLocaleString() },
    { title: '结束时间', dataIndex: 'finishedAt', width: 168, render: (v: string) => v ? new Date(v).toLocaleString() : '-' },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_: unknown, row: InstanceDetailVo) => (
        <a onClick={() => navigate(`/instances/${row.id}`)}>查看</a>
      ),
    },
  ];

  return (
    <Card
      title="我发起的流程"
      extra={
        <Segmented
          value={statusFilter}
          onChange={(v) => setStatusFilter(v as InstanceStatusFilter)}
          options={[
            { value: 'all', label: '全部' },
            { value: 'running', label: '审批中' },
            { value: 'completed', label: '已完成' },
            { value: 'rejected', label: '已驳回' },
          ]}
        />
      }
    >
      <Table rowKey="id" columns={columns} dataSource={filteredList} loading={loading} pagination={{ pageSize: 10 }} />
    </Card>
  );
}
