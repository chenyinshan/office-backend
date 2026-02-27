import { Form, Input, Button, Card, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../stores/auth';

export default function Login() {
  const [form] = Form.useForm();
  const { login } = useAuth();
  const navigate = useNavigate();

  const onFinish = async (v: { username: string; password: string }) => {
    try {
      await login(v.username, v.password);
      message.success('登录成功');
      navigate('/');
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '登录失败');
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: '80px auto', padding: 24 }}>
      <Card title="OA 办公管理 - 登录">
        <Form form={form} onFinish={onFinish} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input placeholder="admin" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true }]}>
            <Input.Password placeholder="123456" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
