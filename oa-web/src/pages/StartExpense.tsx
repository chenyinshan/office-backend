import { useState } from 'react';
import { Form, Input, InputNumber, Card, Button, message, Upload, Tag, Space } from 'antd';
import { useNavigate } from 'react-router-dom';
import { UploadOutlined } from '@ant-design/icons';
import { workflowApi, attachmentApi, AttachmentVo } from '../api/client';

export default function StartExpense() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [attachmentList, setAttachmentList] = useState<AttachmentVo[]>([]);
  const [uploading, setUploading] = useState(false);

  const onFinish = async (v: { totalAmount: number; expenseType?: string; description?: string }) => {
    try {
      const res = await workflowApi.expenseStart({
        totalAmount: v.totalAmount,
        expenseType: v.expenseType,
        description: v.description,
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
      const res = await attachmentApi.upload(file, 'expense');
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
    return false;
  };

  const removeAttachment = (id: number) => {
    setAttachmentList((prev) => prev.filter((a) => a.id !== id));
  };

  return (
    <Card title="发起报销">
      <Form form={form} onFinish={onFinish} layout="vertical" style={{ maxWidth: 480 }}>
        <Form.Item name="totalAmount" label="报销金额（元）" rules={[{ required: true }]}>
          <InputNumber min={0} step={0.01} precision={2} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="expenseType" label="报销类型">
          <Input placeholder="如：差旅、办公" />
        </Form.Item>
        <Form.Item name="description" label="说明">
          <Input.TextArea rows={3} placeholder="选填" />
        </Form.Item>
        <Form.Item label="附件（如发票）">
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
