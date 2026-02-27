import { useEffect, useState } from 'react';
import { Card, Table, DatePicker, Button, Space, Spin, message } from 'antd';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import * as XLSX from 'xlsx';
import { workflowApi, LeaveStatsVo, ExpenseStatsVo } from '../api/client';

const leaveTypeLabel: Record<string, string> = {
  annual: '年假',
  sick: '病假',
  personal: '事假',
  other: '其他',
};

export default function ReportStats() {
  const [leaveStats, setLeaveStats] = useState<LeaveStatsVo | null>(null);
  const [expenseStats, setExpenseStats] = useState<ExpenseStatsVo | null>(null);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null]>([null, null]);

  const load = async () => {
    setLoading(true);
    const [start, end] = dateRange;
    const params =
      start && end
        ? { startDate: start.format('YYYY-MM-DD'), endDate: end.format('YYYY-MM-DD') }
        : undefined;
    try {
      const [leaveRes, expenseRes] = await Promise.all([
        workflowApi.leaveStats(params),
        workflowApi.expenseStats(params),
      ]);
      if (leaveRes.data.code === 0) setLeaveStats(leaveRes.data.data);
      if (expenseRes.data.code === 0) setExpenseStats(expenseRes.data.data);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const exportExcel = () => {
    if (!leaveStats && !expenseStats) {
      message.warning('暂无数据可导出');
      return;
    }
    const wb = XLSX.utils.book_new();

    if (leaveStats) {
      const leaveRows: (string | number)[][] = [
        ['请假类型', '天数', '笔数'],
        ...leaveStats.byType.map((r) => [
          (leaveTypeLabel[r.leaveType] ?? r.leaveType) || '-',
          Number(r.days),
          r.count,
        ]),
        ['合计', Number(leaveStats.totalDays), leaveStats.totalCount],
      ];
      const ws = XLSX.utils.aoa_to_sheet(leaveRows);
      XLSX.utils.book_append_sheet(wb, ws, '请假统计');
    }

    if (expenseStats) {
      const expenseRows: (string | number)[][] = [
        ['报销类型', '金额(元)', '笔数'],
        ...expenseStats.byType.map((r) => [r.expenseType || '-', Number(r.amount), r.count]),
        ['合计', Number(expenseStats.totalAmount), expenseStats.totalCount],
      ];
      const ws = XLSX.utils.aoa_to_sheet(expenseRows);
      XLSX.utils.book_append_sheet(wb, ws, '报销统计');
    }

    const [start, end] = dateRange;
    const name = start && end
      ? `报表_${start.format('YYYYMMDD')}-${end.format('YYYYMMDD')}.xlsx`
      : `报表_${dayjs().format('YYYYMMDD_HHmm')}.xlsx`;
    XLSX.writeFile(wb, name);
    message.success('已导出');
  };

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <span>统计区间：</span>
          <DatePicker.RangePicker
            value={dateRange}
            onChange={(v) => setDateRange(v ?? [null, null])}
            allowClear
          />
          <Button type="primary" onClick={load} loading={loading}>
            查询
          </Button>
          <Button onClick={exportExcel} disabled={loading || (!leaveStats && !expenseStats)}>
            导出 Excel
          </Button>
        </Space>
        <div style={{ marginTop: 8, color: 'rgba(0,0,0,0.45)', fontSize: 12 }}>
          不选日期时统计全部已完成申请；请假按开始日期、报销按创建日期筛选。
        </div>
      </Card>

      <Card title="请假统计" style={{ marginBottom: 16 }}>
        {loading && !leaveStats ? (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin />
          </div>
        ) : leaveStats ? (
          <>
            <div style={{ marginBottom: 16 }}>
              总天数：<strong>{Number(leaveStats.totalDays)}</strong> 天 &nbsp; 总笔数：<strong>{leaveStats.totalCount}</strong> 笔
            </div>
            <Table
              rowKey="leaveType"
              size="small"
              dataSource={leaveStats.byType}
              columns={[
                {
                  title: '请假类型',
                  dataIndex: 'leaveType',
                  render: (t: string) => (leaveTypeLabel[t] ?? t) || '-',
                },
                { title: '天数', dataIndex: 'days', width: 100, render: (v: number) => Number(v) },
                { title: '笔数', dataIndex: 'count', width: 80 },
              ]}
              pagination={false}
            />
          </>
        ) : (
          <div style={{ color: 'rgba(0,0,0,0.45)' }}>暂无数据</div>
        )}
      </Card>

      <Card title="报销统计">
        {loading && !expenseStats ? (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin />
          </div>
        ) : expenseStats ? (
          <>
            <div style={{ marginBottom: 16 }}>
              总金额：<strong>{Number(expenseStats.totalAmount)}</strong> 元 &nbsp; 总笔数：<strong>{expenseStats.totalCount}</strong> 笔
            </div>
            <Table
              rowKey="expenseType"
              size="small"
              dataSource={expenseStats.byType}
              columns={[
                { title: '报销类型', dataIndex: 'expenseType', render: (t: string) => t || '-' },
                { title: '金额（元）', dataIndex: 'amount', width: 120, render: (v: number) => Number(v) },
                { title: '笔数', dataIndex: 'count', width: 80 },
              ]}
              pagination={false}
            />
          </>
        ) : (
          <div style={{ color: 'rgba(0,0,0,0.45)' }}>暂无数据</div>
        )}
      </Card>
    </div>
  );
}
