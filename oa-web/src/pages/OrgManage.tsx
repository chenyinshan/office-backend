import { useEffect, useState } from 'react';
import { Card, Tabs, Tree, Table, Button, Space, Modal, Form, Input, InputNumber, Select, message, Popconfirm } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { orgApi, DeptTreeNode, SysPost, SysEmployee } from '../api/client';

export default function OrgManage() {
  const [deptTree, setDeptTree] = useState<DeptTreeNode[]>([]);
  const [posts, setPosts] = useState<SysPost[]>([]);
  const [employees, setEmployees] = useState<SysEmployee[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('dept');
  const [deptModal, setDeptModal] = useState<{ type: 'add' | 'edit'; parentId?: number; id?: number; initial?: Partial<DeptTreeNode> } | null>(null);
  const [selectedDeptKey, setSelectedDeptKey] = useState<string | null>(null);
  const [postModal, setPostModal] = useState<{ type: 'add' | 'edit'; id?: number; initial?: Partial<SysPost> } | null>(null);
  const [empModal, setEmpModal] = useState<{ type: 'add' | 'edit'; id?: number; initial?: Partial<SysEmployee> } | null>(null);
  const [form] = Form.useForm();
  const [postForm] = Form.useForm();
  const [empForm] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const loadDept = (noCache = false) =>
    orgApi.deptTree(noCache ? { _t: Date.now() } : undefined).then((r) => {
      if (r.data.code === 0) {
        const list = r.data.data;
        setDeptTree(Array.isArray(list) ? list : []);
      }
      return r;
    });
  const loadPost = (noCache = false) =>
    orgApi.postList(noCache ? { _t: Date.now() } : undefined).then((r) => {
      if (r.data.code === 0) {
        const list = r.data.data;
        setPosts(Array.isArray(list) ? list : []);
      }
      return r;
    });
  const loadEmp = (noCache = false) =>
    orgApi.employeeList(undefined, noCache ? { _t: Date.now() } : undefined).then((r) => {
      if (r.data.code === 0) {
        const list = r.data.data;
        setEmployees(Array.isArray(list) ? list : []);
      }
      return r;
    });

  useEffect(() => {
    setLoading(true);
    Promise.all([loadDept(), loadPost(), loadEmp()]).finally(() => setLoading(false));
  }, []);

  const treeData: DataNode[] = deptTree.map((d) => ({
    key: String(d.id),
    title: `${d.deptName}${d.deptCode ? ` (${d.deptCode})` : ''}`,
    children: (d.children || []).map((c) => ({
      key: String(c.id),
      title: `${c.deptName}${c.deptCode ? ` (${c.deptCode})` : ''}`,
    })),
  }));

  const defaultExpandedKeys = deptTree.map((d) => String(d.id));

  const selectedDeptId = selectedDeptKey ? Number(selectedDeptKey) : null;
  const findDeptById = (id: number): DeptTreeNode | undefined => {
    for (const d of deptTree) {
      if (d.id === id) return d;
      const c = (d.children || []).find((x) => x.id === id);
      if (c) return c;
    }
    return undefined;
  };
  const handleDeptDelete = async (id: number) => {
    try {
      await orgApi.deptDelete(id);
      message.success('已删除');
      setSelectedDeptKey(null);
      setLoading(true);
      await loadDept(true);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '删除失败');
    } finally {
      setLoading(false);
    }
  };

  const handleDeptOk = async () => {
    const v = await form.validateFields();
    setSubmitting(true);
    try {
      if (deptModal?.type === 'add') {
        await orgApi.deptCreate({ parentId: deptModal.parentId ?? 0, deptName: v.deptName, deptCode: v.deptCode, sortOrder: v.sortOrder });
        message.success('新增成功');
      } else if (deptModal?.id) {
        await orgApi.deptUpdate(deptModal.id, { deptName: v.deptName, deptCode: v.deptCode, sortOrder: v.sortOrder, status: v.status });
        message.success('更新成功');
      }
      setDeptModal(null);
      form.resetFields();
      setLoading(true);
      await loadDept(true);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setSubmitting(false);
      setLoading(false);
    }
  };

  const handlePostOk = async () => {
    const v = await postForm.validateFields();
    setSubmitting(true);
    try {
      if (postModal?.type === 'add') {
        await orgApi.postCreate({ postCode: v.postCode, postName: v.postName, sortOrder: v.sortOrder });
        message.success('新增成功');
      } else if (postModal?.id) {
        await orgApi.postUpdate(postModal.id, { postCode: v.postCode, postName: v.postName, sortOrder: v.sortOrder, status: v.status });
        message.success('更新成功');
      }
      setPostModal(null);
      postForm.resetFields();
      setLoading(true);
      await loadPost(true);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setSubmitting(false);
      setLoading(false);
    }
  };

  const handleEmpOk = async () => {
    const v = await empForm.validateFields();
    setSubmitting(true);
    try {
      if (empModal?.type === 'add') {
        const body = {
          deptId: v.deptId,
          postId: v.postId,
          employeeNo: v.employeeNo,
          name: v.name,
          gender: v.gender,
          phone: v.phone,
          email: v.email,
          ...(v.hireDate ? { hireDate: v.hireDate } : {}),
        };
        const res = await orgApi.employeeCreate(body);
        if (res.data.code !== 0) {
          message.error(res.data.message || '新增失败');
          return;
        }
        message.success('新增成功');
      } else if (empModal?.id) {
        const res = await orgApi.employeeUpdate(empModal.id, v);
        if (res.data.code !== 0) {
          message.error(res.data.message || '更新失败');
          return;
        }
        message.success('更新成功');
      }
      setEmpModal(null);
      empForm.resetFields();
      setLoading(true);
      await loadEmp(true);
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setSubmitting(false);
      setLoading(false);
    }
  };

  const deptOptions = deptTree.flatMap((d) => [
    { value: d.id, label: d.deptName },
    ...(d.children || []).map((c) => ({ value: c.id, label: `　${c.deptName}` })),
  ]);

  return (
    <Card title="组织管理">
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <Tabs.TabPane tab="部门" key="dept">
          <Space style={{ marginBottom: 16 }} wrap>
            <Button type="primary" onClick={() => { setDeptModal({ type: 'add', parentId: 0 }); form.setFieldsValue({ sortOrder: 0 }); }}>
              新增根部门
            </Button>
            <Button disabled={!selectedDeptId} onClick={() => { setDeptModal({ type: 'add', parentId: selectedDeptId ?? 0 }); form.setFieldsValue({ sortOrder: 0 }); }}>
              在选中下新增子部门
            </Button>
            <Button disabled={!selectedDeptId} onClick={() => { const n = findDeptById(selectedDeptId!); if (n) { setDeptModal({ type: 'edit', id: n.id, initial: n }); form.setFieldsValue({ deptName: n.deptName, deptCode: n.deptCode, sortOrder: n.sortOrder, status: n.status }); } }}>
              编辑选中部门
            </Button>
            <Popconfirm title="确定删除该部门？" disabled={!selectedDeptId} onConfirm={() => selectedDeptId != null && handleDeptDelete(selectedDeptId)}>
              <Button danger disabled={!selectedDeptId}>删除选中部门</Button>
            </Popconfirm>
          </Space>
          {loading ? '加载中...' : <Tree showLine treeData={treeData} defaultExpandedKeys={defaultExpandedKeys} selectedKeys={selectedDeptKey ? [selectedDeptKey] : []} onSelect={(keys) => setSelectedDeptKey(keys.length ? String(keys[0]) : null)} />}
        </Tabs.TabPane>
        <Tabs.TabPane tab="岗位" key="post">
          <Space style={{ marginBottom: 16 }}>
            <Button type="primary" onClick={() => { setPostModal({ type: 'add' }); postForm.setFieldsValue({ sortOrder: 0 }); }}>
              新增岗位
            </Button>
          </Space>
          <Table
            rowKey="id"
            loading={loading}
            dataSource={posts}
            columns={[
              { title: '编码', dataIndex: 'postCode', width: 120 },
              { title: '名称', dataIndex: 'postName', width: 120 },
              { title: '排序', dataIndex: 'sortOrder', width: 80 },
              { title: '状态', dataIndex: 'status', width: 80, render: (s: number) => (s === 1 ? '正常' : '停用') },
              {
                title: '操作',
                width: 160,
                render: (_, row) => (
                  <Space>
                    <Button type="link" size="small" onClick={() => { setPostModal({ type: 'edit', id: row.id, initial: row }); postForm.setFieldsValue(row); }}>
                      编辑
                    </Button>
                    <Popconfirm title="确定删除？" onConfirm={async () => { await orgApi.postDelete(row.id); message.success('已删除'); setLoading(true); await loadPost(true); setLoading(false); }}>
                      <Button type="link" size="small" danger>删除</Button>
                    </Popconfirm>
                  </Space>
                ),
              },
            ]}
            pagination={false}
          />
        </Tabs.TabPane>
        <Tabs.TabPane tab="员工" key="emp">
          <Space style={{ marginBottom: 16 }}>
            <Button type="primary" onClick={() => { setEmpModal({ type: 'add' }); empForm.setFieldsValue({}); }}>
              新增员工
            </Button>
          </Space>
          <Table
            rowKey="id"
            loading={loading}
            dataSource={employees}
            columns={[
              { title: '工号', dataIndex: 'employeeNo', width: 100 },
              { title: '姓名', dataIndex: 'name', width: 100 },
              { title: '部门ID', dataIndex: 'deptId', width: 80 },
              { title: '岗位ID', dataIndex: 'postId', width: 80 },
              { title: '手机', dataIndex: 'phone', width: 120 },
              { title: '状态', dataIndex: 'status', width: 80, render: (s: number) => (s === 1 ? '在职' : '离职') },
              {
                title: '操作',
                width: 160,
                render: (_, row) => (
                  <Space>
                    <Button type="link" size="small" onClick={() => { setEmpModal({ type: 'edit', id: row.id, initial: row }); empForm.setFieldsValue({ ...row, hireDate: row.hireDate || undefined }); }}>
                      编辑
                    </Button>
                    <Popconfirm title="确定删除？" onConfirm={async () => {
                      try {
                        await orgApi.employeeDelete(row.id);
                        message.success('已删除');
                        setLoading(true);
                        await loadEmp(true);
                      } catch (e: unknown) {
                        message.error(e instanceof Error ? e.message : '删除失败');
                      } finally {
                        setLoading(false);
                      }
                    }}>
                      <Button type="link" size="small" danger>删除</Button>
                    </Popconfirm>
                  </Space>
                ),
              },
            ]}
            pagination={{ pageSize: 10 }}
          />
        </Tabs.TabPane>
      </Tabs>

      <Modal title={deptModal?.type === 'add' ? '新增部门' : '编辑部门'} open={!!deptModal} onOk={handleDeptOk} onCancel={() => { setDeptModal(null); form.resetFields(); }} confirmLoading={submitting} destroyOnClose>
        <Form form={form} layout="vertical" initialValues={deptModal?.initial}>
          <Form.Item name="deptName" label="部门名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="deptCode" label="部门编码">
            <Input />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          {deptModal?.type === 'edit' && (
            <Form.Item name="status" label="状态">
              <Select options={[{ value: 1, label: '正常' }, { value: 0, label: '停用' }]} />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal title={postModal?.type === 'add' ? '新增岗位' : '编辑岗位'} open={!!postModal} onOk={handlePostOk} onCancel={() => { setPostModal(null); postForm.resetFields(); }} confirmLoading={submitting} destroyOnClose>
        <Form form={postForm} layout="vertical" initialValues={postModal?.initial}>
          <Form.Item name="postCode" label="岗位编码" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="postName" label="岗位名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          {postModal?.type === 'edit' && (
            <Form.Item name="status" label="状态">
              <Select options={[{ value: 1, label: '正常' }, { value: 0, label: '停用' }]} />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal title={empModal?.type === 'add' ? '新增员工' : '编辑员工'} open={!!empModal} onOk={handleEmpOk} onCancel={() => { setEmpModal(null); empForm.resetFields(); }} confirmLoading={submitting} destroyOnClose width={480}>
        <Form form={empForm} layout="vertical" initialValues={empModal?.initial}>
          <Form.Item name="deptId" label="部门" rules={[{ required: true }]}>
            <Select placeholder="请选择部门" options={deptOptions} showSearch optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="postId" label="岗位">
            <Select placeholder="请选择" allowClear options={posts.map((p) => ({ value: p.id, label: p.postName }))} />
          </Form.Item>
          <Form.Item name="employeeNo" label="工号" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="name" label="姓名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="gender" label="性别">
            <Select allowClear options={[{ value: 1, label: '男' }, { value: 2, label: '女' }]} />
          </Form.Item>
          <Form.Item name="phone" label="手机">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
          <Form.Item name="hireDate" label="入职日期">
            <Input type="date" />
          </Form.Item>
          {empModal?.type === 'edit' && (
            <Form.Item name="status" label="状态">
              <Select options={[{ value: 1, label: '在职' }, { value: 0, label: '离职' }]} />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </Card>
  );
}
