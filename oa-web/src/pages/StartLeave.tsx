import { useState } from 'react';
import { Form, Input, InputNumber, Card, Button, DatePicker, Select, Upload, message, Tag, Space } from 'antd';
import { useNavigate } from 'react-router-dom';
import type { Dayjs } from 'dayjs';
import { UploadOutlined } from '@ant-design/icons';
import { workflowApi, attachmentApi, AttachmentVo } from '../api/client';

export default function StartLeave() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [attachmentList, setAttachmentList] = useState<AttachmentVo[]>([]);
  const [uploading, setUploading] = useState(false);

  const onFinish = async (v: { leaveType: string; startTime: Dayjs; endTime: Dayjs; days: number; reason?: string }) => {
    try {
      const res = await workflowApi.leaveStart({
        leaveType: v.leaveType,
        startTime: v.startTime.toISOString(),
        endTime: v.endTime.toISOString(),
        days: v.days,
        reason: v.reason,
        attachmentIds: attachmentList.length > 0 ? attachmentList.map((a) => a.id) : undefined,
      });
      if (res.data.code === 0) {
        message.success('提交成功');
        form.resetFields();
        setAttachmentList([]);
        navigate('/instances/my');
      } else {
        message.error(res.data.message || '提交失败');
      }
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '提交失败');
    }
  };

  const handleUpload = async (file: File) => {
    setUploading(true);
    try {
      const res = await attachmentApi.upload(file, 'leave');
      if (res.data.code === 0 && res.data.data) {
        setAttachmentList((prev) => [...prev, res.data.data!]);
        message.success('上传成功');
      } else {
        message.error(res.data.message || '上传失败');
      }
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '上传失败');
    } finally {
      setUploading(false);
    }
    return false; // 阻止默认上传，我们手动调 API
  };

  const removeAttachment = (id: number) => {
    setAttachmentList((prev) => prev.filter((a) => a.id !== id));
  };

  return (
    <Card title="发起请假">
      <Form form={form} onFinish={onFinish} layout="vertical" style={{ maxWidth: 480 }}>
        <Form.Item name="leaveType" label="请假类型" rules={[{ required: true }]}>
          <Select
            placeholder="请选择"
            options={[
              { value: 'annual', label: '年假' },
              { value: 'sick', label: '病假' },
              { value: 'personal', label: '事假' },
            ]}
          />
        </Form.Item>
        <Form.Item name="startTime" label="开始时间" rules={[{ required: true }]}>
          <DatePicker showTime style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="endTime" label="结束时间" rules={[{ required: true }]}>
          <DatePicker showTime style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="days" label="天数" rules={[{ required: true }]}>
          <InputNumber min={0.5} step={0.5} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="reason" label="原因">
          <Input.TextArea rows={3} placeholder="选填" />
        </Form.Item>
        <Form.Item label="附件">
          <Upload beforeUpload={handleUpload} showUploadList={false} accept="*" disabled={uploading}>
            <Button icon={<UploadOutlined />} loading={uploading}>选择文件上传</Button>
          </Upload>
          {attachmentList.length > 0 && (
            <Space wrap style={{ marginTop: 8 }}>
              {attachmentList.map((a) => (
                <Tag key={a.id} closable onClose={() => removeAttachment(a.id)}>
                  {a.fileName}
                </Tag>
              ))}
            </Space>
          )}
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit">
            提交
          </Button>
          <Button style={{ marginLeft: 8 }} onClick={() => navigate(-1)}>
            返回
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
}
