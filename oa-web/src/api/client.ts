import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  if (config.data instanceof FormData) {
    config.headers['Content-Type'] = false;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default api;

export interface Result<T> {
  code: number;
  message: string;
  data: T;
  success?: boolean;
}

export interface UserInfo {
  userId: number;
  username: string;
  employeeId: number;
  employeeNo?: string;
  name: string;
  deptId?: number;
  deptName?: string;
  postId?: number;
  postName?: string;
  /** 角色编码列表，如 ["ADMIN","STAFF"] */
  roles?: string[];
}

export interface LoginRes {
  token: string;
  user: UserInfo;
}

export const authApi = {
  login: (username: string, password: string) =>
    api.post<Result<LoginRes>>('/auth/login', { username, password }),
  logout: () => api.post<Result<null>>('/auth/logout'),
  me: () => api.get<Result<UserInfo>>('/auth/me'),
};

export interface WfInstance {
  id: number;
  definitionId: number;
  processKey: string;
  businessType: string;
  businessId: number;
  title: string;
  applicantUserId: number;
  applicantEmployeeId: number;
  status: string;
  currentNodeKey?: string;
  /** 当前节点名称（多级审批） */
  currentNodeName?: string;
  createdAt: string;
  updatedAt?: string;
  finishedAt?: string;
}

export interface WfTask {
  id: number;
  instanceId: number;
  nodeKey: string;
  /** 节点名称，如 部门主管审批、人事审批 */
  nodeName?: string;
  assigneeUserId: number;
  status: string;
  resultComment?: string;
  actedAt?: string;
  createdAt: string;
  updatedAt?: string;
}

/** 待办任务 + 业务详情（请假/报销） */
export interface TaskDetailVo extends WfTask {
  title?: string;
  businessType?: string;
  leaveType?: string;
  leaveDays?: number;
  leaveReason?: string;
  leaveStartTime?: string;
  leaveEndTime?: string;
  expenseAmount?: number;
  expenseType?: string;
  expenseDescription?: string;
}

/** 流程实例 + 业务详情 */
export interface InstanceDetailVo extends WfInstance {
  leaveType?: string;
  leaveDays?: number;
  leaveReason?: string;
  leaveStartTime?: string;
  leaveEndTime?: string;
  expenseAmount?: number;
  expenseType?: string;
  expenseDescription?: string;
}

/** 审批操作记录 */
export interface TaskLogVo {
  id: number;
  taskId: number;
  instanceId: number;
  nodeKey: string;
  operatorUserId: number;
  action: string;
  comment?: string;
  createdAt: string;
}

export const workflowApi = {
  leaveStart: (body: {
    leaveType: string;
    startTime: string;
    endTime: string;
    days: number;
    reason?: string;
    attachmentIds?: number[];
  }) => api.post<Result<WfInstance>>('/workflow/leave/start', body),
  expenseStart: (body: {
    totalAmount: number;
    expenseType?: string;
    description?: string;
    attachmentIds?: number[];
  }) => api.post<Result<WfInstance>>('/workflow/expense/start', body),
  pendingTasks: () => api.get<Result<TaskDetailVo[]>>('/workflow/tasks/pending'),
  /** 我的任务（待办+已处理），status: pending | approved | rejected | 不传为全部 */
  myTasks: (status?: string) =>
    api.get<Result<TaskDetailVo[]>>('/workflow/tasks/my', { params: status != null && status !== '' ? { status } : {} }),
  myInstances: () => api.get<Result<InstanceDetailVo[]>>('/workflow/instances/my'),
  getInstance: (id: number) => api.get<Result<InstanceDetailVo>>(`/workflow/instances/${id}`),
  getInstanceLogs: (id: number) => api.get<Result<TaskLogVo[]>>(`/workflow/instances/${id}/logs`),
  approve: (taskId: number, comment?: string) =>
    api.post<Result<null>>(`/workflow/tasks/${taskId}/approve`, { comment }),
  reject: (taskId: number, comment?: string) =>
    api.post<Result<null>>(`/workflow/tasks/${taskId}/reject`, { comment }),
  /** 请假统计，可选 startDate/endDate 格式 yyyy-MM-dd */
  leaveStats: (params?: { startDate?: string; endDate?: string }) =>
    api.get<Result<LeaveStatsVo>>('/workflow/stats/leave', { params: params ?? {} }),
  /** 报销统计，可选 startDate/endDate 格式 yyyy-MM-dd */
  expenseStats: (params?: { startDate?: string; endDate?: string }) =>
    api.get<Result<ExpenseStatsVo>>('/workflow/stats/expense', { params: params ?? {} }),
};

export interface LeaveStatsVo {
  byType: { leaveType: string; days: number; count: number }[];
  totalDays: number;
  totalCount: number;
}

export interface ExpenseStatsVo {
  byType: { expenseType: string; amount: number; count: number }[];
  totalAmount: number;
  totalCount: number;
}

export interface NoticeVo {
  id: number;
  title: string;
  content: string;
  publisherUserId?: number;
  isTop?: number;
  status?: string;
  publishAt?: string;
  createdAt?: string;
  updatedAt?: string;
  read?: boolean;
}

export const noticeApi = {
  list: () => api.get<Result<NoticeVo[]>>('/notices'),
  myDrafts: () => api.get<Result<NoticeVo[]>>('/notices/my-drafts'),
  getById: (id: number, markRead = false) =>
    api.get<Result<NoticeVo>>(`/notices/${id}`, { params: markRead ? { read: 1 } : {} }),
  publish: (body: { title: string; content: string; isTop?: boolean }) =>
    api.post<Result<unknown>>('/notices', body),
  saveDraft: (body: { title: string; content?: string; isTop?: boolean }) =>
    api.post<Result<NoticeVo>>('/notices/draft', body),
  update: (id: number, body: { title?: string; content?: string; isTop?: boolean }) =>
    api.put<Result<null>>(`/notices/${id}`, body),
  publishDraft: (id: number) => api.post<Result<null>>(`/notices/${id}/publish`),
  unpublish: (id: number) => api.post<Result<null>>(`/notices/${id}/unpublish`),
  delete: (id: number) => api.delete<Result<null>>(`/notices/${id}`),
  markRead: (id: number) => api.post<Result<null>>(`/notices/${id}/read`),
};

/** 站内通知（审批结果等） */
export interface NotificationVo {
  id: number;
  userId: number;
  type: string;
  title: string;
  content?: string;
  businessType?: string;
  businessId?: string;
  isRead: boolean;
  createdAt: string;
}

export const notificationApi = {
  list: () => api.get<Result<NotificationVo[]>>('/notifications'),
  unreadCount: () => api.get<Result<number>>('/notifications/unread-count'),
  markRead: (id: number) => api.post<Result<null>>(`/notifications/${id}/read`),
};

export interface AttachmentVo {
  id: number;
  fileName: string;
  filePath?: string;
  fileSize?: number;
  fileContentType?: string;
}

export const attachmentApi = {
  upload: async (file: File, businessType: 'leave' | 'expense' = 'leave') => {
    const form = new FormData();
    form.append('file', file);
    form.append('businessType', businessType);
    const token = localStorage.getItem('token');
    const res = await fetch('/api/workflow/attachments/upload', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form,
    });
    if (res.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
      throw new Error('未登录');
    }
    const data = await res.json();
    return { data } as { data: Result<AttachmentVo> };
  },
};

export interface DeptTreeNode {
  id: number;
  parentId?: number;
  deptName: string;
  deptCode?: string;
  sortOrder?: number;
  status?: number;
  children?: DeptTreeNode[];
}

export interface SysPost {
  id: number;
  postCode: string;
  postName: string;
  sortOrder?: number;
  status?: number;
}

export interface SysEmployee {
  id: number;
  deptId: number;
  postId?: number;
  employeeNo: string;
  name: string;
  gender?: number;
  phone?: string;
  email?: string;
  hireDate?: string;
  status?: number;
}

export const orgApi = {
  /** 拉取部门树；可选 params._t 用于防缓存，刷新后拿到最新完整列表 */
  deptTree: (params?: { _t?: number }) =>
    api.get<Result<DeptTreeNode[]>>('/org/depts/tree', { params: params ?? {} }),
  deptCreate: (body: { parentId?: number; deptName: string; deptCode?: string; sortOrder?: number }) =>
    api.post<Result<unknown>>('/org/depts', body),
  deptUpdate: (id: number, body: { deptName?: string; deptCode?: string; sortOrder?: number; status?: number }) =>
    api.put<Result<null>>(`/org/depts/${id}`, body),
  deptDelete: (id: number) => api.delete<Result<null>>(`/org/depts/${id}`),
  postList: (params?: { _t?: number }) =>
    api.get<Result<SysPost[]>>('/org/posts', { params: params ?? {} }),
  postCreate: (body: { postCode: string; postName: string; sortOrder?: number }) =>
    api.post<Result<SysPost>>('/org/posts', body),
  postUpdate: (id: number, body: { postCode?: string; postName?: string; sortOrder?: number; status?: number }) =>
    api.put<Result<null>>(`/org/posts/${id}`, body),
  postDelete: (id: number) => api.delete<Result<null>>(`/org/posts/${id}`),
  employeeList: (deptId?: number, params?: { _t?: number }) =>
    api.get<Result<SysEmployee[]>>('/org/employees', {
      params: { ...(deptId != null ? { deptId } : {}), ...(params ?? {}) },
    }),
  employeeCreate: (body: { deptId: number; postId?: number; employeeNo: string; name: string; gender?: number; phone?: string; email?: string; hireDate?: string }) =>
    api.post<Result<SysEmployee>>('/org/employees', body),
  employeeUpdate: (id: number, body: Partial<SysEmployee>) => api.put<Result<null>>(`/org/employees/${id}`, body),
  employeeDelete: (id: number) => api.delete<Result<null>>(`/org/employees/${id}`),
};
