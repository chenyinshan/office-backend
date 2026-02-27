import { useEffect, useState } from 'react';
import { Card, Descriptions, Tag, Spin, message, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useParams, useNavigate } from 'react-router-dom';
import { workflowApi, InstanceDetailVo, TaskLogVo } from '../api/client';

const statusMap: Record<string, { color: string; text: string }> = {
  running: { color: 'processing', text: '审批中' },
  completed: { color: 'success', text: '已完成' },
  rejected: { color: 'error', text: '已驳回' },
};

const leaveTypeMap: Record<string, string> = {
  annual: '年假',
  sick: '病假',
  personal: '事假',
  other: '其他',
};

export default function InstanceDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<InstanceDetailVo | null>(null);
  const [logs, setLogs] = useState<TaskLogVo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    const numId = Number(id);
    if (Number.isNaN(numId)) {
      setLoading(false);
      return;
    }
    Promise.all([
      workflowApi.getInstance(numId),
      workflowApi.getInstanceLogs(numId),
    ])
      .then(([resDetail, resLogs]) => {
        if (resDetail.data.code === 0 && resDetail.data.data) {
          setDetail(resDetail.data.data);
        } else {
          message.error(resDetail.data.message || '加载失败');
        }
        if (resLogs.data.code === 0 && Array.isArray(resLogs.data.data)) {
          setLogs(resLogs.data.data);
        }
      })
      .catch(() => message.error('加载失败'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <Card title="流程详情">
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin />
        </div>
      </Card>
    );
  }
  if (!detail) {
    return (
      <Card title="流程详情">
        <div style={{ padding: 24 }}>未找到该流程或无权查看。</div>
      </Card>
    );
  }

  const statusInfo = statusMap[detail.status] ?? { color: 'default', text: detail.status };

  return (
    <Card
      title={detail.title ?? '流程详情'}
      extra={
        <Tag color="blue" style={{ cursor: 'pointer' }} onClick={() => navigate('/instances/my')}>
          返回我发起的
        </Tag>
      }
    >
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="流程 ID">{detail.id}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="业务类型">
          {detail.businessType === 'leave' ? '请假' : detail.businessType === 'expense' ? '报销' : detail.businessType ?? '-'}
        </Descriptions.Item>
        {detail.status === 'running' && detail.currentNodeName && (
          <Descriptions.Item label="当前节点">{detail.currentNodeName}</Descriptions.Item>
        )}
        <Descriptions.Item label="创建时间">
          {detail.createdAt ? new Date(detail.createdAt).toLocaleString() : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="结束时间">
          {detail.finishedAt ? new Date(detail.finishedAt).toLocaleString() : '-'}
        </Descriptions.Item>
        {detail.businessType === 'leave' && (
          <>
            <Descriptions.Item label="请假类型">{leaveTypeMap[detail.leaveType ?? ''] ?? detail.leaveType ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="请假天数">{detail.leaveDays ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="开始时间">
              {detail.leaveStartTime ? new Date(detail.leaveStartTime).toLocaleString() : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="结束时间">
              {detail.leaveEndTime ? new Date(detail.leaveEndTime).toLocaleString() : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="请假原因">{detail.leaveReason ?? '-'}</Descriptions.Item>
          </>
        )}
        {detail.businessType === 'expense' && (
          <>
            <Descriptions.Item label="报销金额">{detail.expenseAmount ?? '-'} 元</Descriptions.Item>
            <Descriptions.Item label="报销类型">{detail.expenseType ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="说明">{detail.expenseDescription ?? '-'}</Descriptions.Item>
          </>
        )}
      </Descriptions>
      {logs.length > 0 && (
        <>
          <div style={{ marginTop: 24, marginBottom: 8, fontWeight: 500 }}>审批记录</div>
          <Table<TaskLogVo>
            rowKey="id"
            size="small"
            pagination={false}
            dataSource={logs}
            columns={[
              {
                title: '节点',
                dataIndex: 'nodeKey',
                width: 120,
                render: (v: string) => v || '-',
              },
              {
                title: '操作',
                dataIndex: 'action',
                width: 80,
                render: (v: string) => (v === 'approve' ? <Tag color="green">通过</Tag> : v === 'reject' ? <Tag color="red">驳回</Tag> : v || '-'),
              },
              {
                title: '操作人 ID',
                dataIndex: 'operatorUserId',
                width: 100,
              },
              {
                title: '意见',
                dataIndex: 'comment',
                ellipsis: true,
                render: (v: string) => v || '-',
              },
              {
                title: '时间',
                dataIndex: 'createdAt',
                width: 168,
                render: (v: string) => (v ? new Date(v).toLocaleString() : '-'),
              },
            ]}
          />
        </>
      )}
    </Card>
  );
}
