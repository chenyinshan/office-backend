import { useState, useRef } from 'react';
import { Card, Button, Space, Tag, List, Typography, message, Input } from 'antd';
import { ApiOutlined, DisconnectOutlined, ThunderboltOutlined, LinkOutlined } from '@ant-design/icons';
import { notificationApi } from '../api/client';

interface SseEvent {
  id: string;
  time: string;
  type: string;
  raw: string;
}

interface WsEvent {
  id: string;
  time: string;
  type: string;
  raw: string;
}

export default function ConnectionDemo() {
  const [shortResult, setShortResult] = useState<{ time: string; count?: number; err?: string } | null>(null);
  const [shortLoading, setShortLoading] = useState(false);
  const [sseEvents, setSseEvents] = useState<SseEvent[]>([]);
  const [sseConnected, setSseConnected] = useState(false);
  const abortRef = useRef<AbortController | null>(null);
  const [wsEvents, setWsEvents] = useState<WsEvent[]>([]);
  const [wsConnected, setWsConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const [wsInput, setWsInput] = useState('');

  const handleShort = async () => {
    setShortLoading(true);
    setShortResult(null);
    const t0 = new Date().toISOString();
    try {
      const res = await notificationApi.unreadCount();
      const t1 = new Date().toISOString();
      if (res.data.code === 0 && typeof res.data.data === 'number') {
        setShortResult({ time: `${t0} → ${t1}`, count: res.data.data });
      } else {
        setShortResult({ time: t1, err: res.data.message || '请求异常' });
      }
    } catch (e: unknown) {
      setShortResult({
        time: new Date().toISOString(),
        err: e instanceof Error ? e.message : '请求失败',
      });
    } finally {
      setShortLoading(false);
    }
  };

  const handleSseConnect = () => {
    if (abortRef.current) return;
    const token = localStorage.getItem('token');
    if (!token) {
      message.error('请先登录');
      return;
    }
    setSseEvents([]);
    const ac = new AbortController();
    abortRef.current = ac;
    setSseConnected(true);

    fetch('/api/sse/notify', {
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream',
      },
      signal: ac.signal,
    })
      .then((res) => {
        if (!res.ok || !res.body) throw new Error(res.statusText || '连接失败');
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buf = '';
        const push = (text: string) => {
          buf += text;
          const parts = buf.split('\n\n');
          buf = parts.pop() ?? '';
          for (const part of parts) {
            const dataMatch = part.match(/^data:\s*(.+)/m);
            const data = dataMatch ? dataMatch[1].trim() : part;
            if (!data) continue;
            let type = 'message';
            try {
              const o = JSON.parse(data);
              type = o.type || type;
            } catch {
              // ignore
            }
            setSseEvents((prev) =>
              prev.concat({
                id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
                time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
                type,
                raw: data,
              })
            );
          }
        };
        const next = (value: ReadableStreamReadResult<Uint8Array>): Promise<void> => {
          if (value.done) return Promise.resolve();
          push(decoder.decode(value.value, { stream: true }));
          return reader.read().then(next);
        };
        return reader.read().then(next);
      })
      .catch((e: unknown) => {
        if ((e as Error).name === 'AbortError') return;
        setSseEvents((prev) =>
          prev.concat({
            id: `err-${Date.now()}`,
            time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
            type: 'error',
            raw: e instanceof Error ? e.message : '连接异常',
          })
        );
      })
      .finally(() => {
        abortRef.current = null;
        setSseConnected(false);
      });
  };

  const handleSseDisconnect = () => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    setSseConnected(false);
  };

  const appendWsEvent = (type: string, raw: string) => {
    setWsEvents((prev) =>
      prev.concat({
        id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
        time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
        type,
        raw,
      })
    );
  };

  const handleWsConnect = () => {
    if (wsRef.current) return;
    setWsEvents([]);
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const url = `${proto}://${window.location.host}/ws/demo`;
    const ws = new WebSocket(url);
    wsRef.current = ws;
    setWsConnected(true);

    ws.onopen = () => {
      appendWsEvent('open', 'WebSocket 连接已建立');
    };
    ws.onmessage = (e) => {
      appendWsEvent('message', e.data ?? '');
    };
    ws.onerror = (e) => {
      appendWsEvent('error', String(e));
    };
    ws.onclose = (e) => {
      appendWsEvent('close', `code=${e.code}, reason=${e.reason || 'normal'}`);
      wsRef.current = null;
      setWsConnected(false);
    };
  };

  const handleWsDisconnect = () => {
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setWsConnected(false);
  };

  const handleWsSend = () => {
    if (!wsConnected || !wsRef.current) {
      message.warning('请先建立 WebSocket 连接');
      return;
    }
    if (!wsInput) {
      return;
    }
    try {
      wsRef.current.send(wsInput);
      appendWsEvent('sent', wsInput);
      setWsInput('');
    } catch (e) {
      appendWsEvent('error', e instanceof Error ? e.message : '发送失败');
    }
  };

  return (
    <div>
      <Typography.Title level={4} style={{ marginBottom: 16 }}>
        长连接 vs 短连接 演示
      </Typography.Title>
      <Typography.Paragraph type="secondary" style={{ marginBottom: 24 }}>
        短连接：一次请求一次响应，连接即关闭。长连接：建立后保持，服务端持续推送，直到断开。
      </Typography.Paragraph>

      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Card
          title={
            <Space>
              <ThunderboltOutlined />
              短连接（REST）
            </Space>
          }
          extra={
            <Button type="primary" loading={shortLoading} onClick={handleShort} icon={<ApiOutlined />}>
              发一次短连接请求
            </Button>
          }
        >
          <p style={{ color: '#666', marginBottom: 8 }}>
            点击按钮会请求 <code>GET /api/notifications/unread-count</code>，得到一次响应后连接关闭。
          </p>
          {shortResult && (
            <div style={{ background: '#fafafa', padding: 12, borderRadius: 4 }}>
              <div>请求时间：{shortResult.time}</div>
              {shortResult.count !== undefined && <div>未读消息数：{shortResult.count}</div>}
              {shortResult.err && <div style={{ color: '#cf1322' }}>{shortResult.err}</div>}
            </div>
          )}
        </Card>

        <Card
          title={
            <Space>
              <ApiOutlined />
              长连接（SSE）
              {sseConnected ? (
                <Tag color="green">已连接</Tag>
              ) : (
                <Tag color="default">未连接</Tag>
              )}
            </Space>
          }
          extra={
            sseConnected ? (
              <Button danger onClick={handleSseDisconnect} icon={<DisconnectOutlined />}>
                断开长连接
              </Button>
            ) : (
              <Button type="primary" onClick={handleSseConnect} icon={<ApiOutlined />}>
                建立长连接
              </Button>
            )
          }
        >
          <p style={{ color: '#666', marginBottom: 8 }}>
            建立后请求 <code>GET /api/sse/notify</code>，连接保持，服务端每 15 秒推送一条心跳；下方实时显示收到的事件。
          </p>
          <List
            size="small"
            dataSource={sseEvents}
            style={{ maxHeight: 320, overflow: 'auto' }}
            renderItem={(item) => (
              <List.Item>
                <Space>
                  <Typography.Text type="secondary">[{item.time}]</Typography.Text>
                  <Tag>{item.type}</Tag>
                  <Typography.Text code style={{ fontSize: 12 }}>
                    {item.raw.length > 60 ? item.raw.slice(0, 60) + '…' : item.raw}
                  </Typography.Text>
                </Space>
              </List.Item>
            )}
          />
          {sseEvents.length === 0 && !sseConnected && (
            <div style={{ color: '#999', padding: 16 }}>点击「建立长连接」后，此处会实时追加服务端推送的事件。</div>
          )}
        </Card>

        <Card
          title={
            <Space>
              <LinkOutlined />
              长连接（WebSocket）
              {wsConnected ? <Tag color="green">已连接</Tag> : <Tag color="default">未连接</Tag>}
            </Space>
          }
          extra={
            wsConnected ? (
              <Button danger onClick={handleWsDisconnect} icon={<DisconnectOutlined />}>
                断开 WebSocket
              </Button>
            ) : (
              <Button type="primary" onClick={handleWsConnect} icon={<ApiOutlined />}>
                建立 WebSocket 连接
              </Button>
            )
          }
        >
          <p style={{ color: '#666', marginBottom: 8 }}>
            建立后请求 <code>WS /ws/demo</code>，连接保持，服务端每 5 秒推送一条心跳，并回显你发送的消息。
          </p>
          <Space style={{ marginBottom: 12 }} align="start">
            <Input
              style={{ width: 260 }}
              placeholder="输入要通过 WebSocket 发送的文本"
              value={wsInput}
              onChange={(e) => setWsInput(e.target.value)}
              disabled={!wsConnected}
              onPressEnter={handleWsSend}
            />
            <Button type="primary" onClick={handleWsSend} disabled={!wsConnected || !wsInput}>
              发送
            </Button>
          </Space>
          <List
            size="small"
            dataSource={wsEvents}
            style={{ maxHeight: 320, overflow: 'auto' }}
            renderItem={(item) => (
              <List.Item>
                <Space>
                  <Typography.Text type="secondary">[{item.time}]</Typography.Text>
                  <Tag>{item.type}</Tag>
                  <Typography.Text code style={{ fontSize: 12 }}>
                    {item.raw.length > 60 ? item.raw.slice(0, 60) + '…' : item.raw}
                  </Typography.Text>
                </Space>
              </List.Item>
            )}
          />
          {wsEvents.length === 0 && !wsConnected && (
            <div style={{ color: '#999', padding: 16 }}>点击「建立 WebSocket 连接」后，此处会实时追加服务端推送的消息。</div>
          )}
        </Card>
      </Space>
    </div>
  );
}
