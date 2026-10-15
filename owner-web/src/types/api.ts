/**
 * 与后端 com.owner.common.core.domain 对应的类型定义。
 */

/** 统一响应结构：com.owner.common.core.domain.R */
export interface R<T = unknown> {
  code: number;
  msg: string;
  data: T;
  timestamp: number;
}

/** 分页结构：com.owner.common.core.domain.PageResult */
export interface PageResult<T> {
  rows: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

/** 业务状态码：com.owner.common.core.enums.ResultCode */
export const ResultCode = {
  SUCCESS: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  ERROR: 500,
  BUSINESS_ERROR: 600,
  DATA_EXISTS: 601,
  DATA_NOT_FOUND: 602,
} as const;

/** 门户用户出参：SysUserVO */
export interface SysUser {
  /**
   * 用户 ID（雪花算法，19 位）。
   * 已超过 JS 的 Number.MAX_SAFE_INTEGER，后端会序列化为字符串，
   * 前端必须按字符串收发，否则会丢精度。
   */
  id: string;
  username: string;
  nickname: string;
  /** 已脱敏，形如 138****0001 */
  mobile?: string;
  email?: string;
  deptName?: string;
  /** 0 正常，1 停用 */
  status?: number;
  remark?: string;
  /** 乐观锁版本号 */
  version?: number;
  createTime?: string;
  updateTime?: string;
}

/** 分页查询条件：SysUserQuery */
export interface SysUserQuery {
  pageNum?: number;
  pageSize?: number;
  username?: string;
  nickname?: string;
  mobile?: string;
  deptName?: string;
  status?: number;
}

/** 新增入参：SysUserCreateDTO */
export interface SysUserCreatePayload {
  username: string;
  nickname: string;
  mobile?: string;
  email?: string;
  deptName?: string;
  remark?: string;
}

/** 修改入参：SysUserUpdateDTO */
export interface SysUserUpdatePayload {
  /** 字符串形式的雪花 ID */
  id: string;
  nickname?: string;
  mobile?: string;
  email?: string;
  deptName?: string;
  status?: number;
  remark?: string;
  version?: number;
}

/** 运行状态：GET /api/portal/info */
export interface PortalInfo {
  applicationName: string;
  port: number;
  activeProfiles: string;
  serverTime: string;
  javaVersion: string;
  websocketSendMode: string;
  websocketOnlineCount: number;
  websocketOnlineClients: string[];
  quartzHeartbeatExecutions: number;
  aiAvailable: boolean;
}

/** WebSocket 消息体：com.owner.framework.websocket.WebSocketMessage */
export interface WsMessage {
  type: string;
  clientId?: string;
  payload?: string;
  timestamp?: number;
}

export type WsStatus = 'connecting' | 'open' | 'closed' | 'error';
