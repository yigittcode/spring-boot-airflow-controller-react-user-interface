// Common interfaces used across the application

// DAG entity definition
export interface Dag {
  dag_id: string;
  description?: string;
  is_active: boolean;
  is_paused: boolean;
  owners?: string[];
  root_dag_id?: string;
  schedule_interval?: {
    type: string;
    value: string;
  };
  tags?: {
    name: string;
  }[];
  fileloc?: string;
  last_parsed_time?: string;
  timetable_description?: string;
}

// Pagination response structure
export interface PageResponse {
  currentPage: number;
  totalPages: number;
  pageSize: number;
  totalElements: number;
}

// Task instance data structure
export interface TaskInstance {
  task_id: string;
  dag_id: string;
  dag_run_id: string;
  state: string;
  start_date: string | null;
  end_date: string | null;
  duration: number | null;
  try_number: number;
  max_tries: number;
  hostname?: string;
  pool?: string;
  pool_slots?: number;
  queue?: string;
  priority_weight?: number;
  operator?: string;
  queued_when?: string;
  pid?: number;
  executor_config?: string;
  sla_miss?: object;
  note?: string;
}

// DAG run data structure
export interface DagRun {
  dag_id: string;
  dag_run_id: string;
  logical_date: string;
  execution_date?: string;
  start_date: string | null;
  end_date: string | null;
  state: string;
  external_trigger: boolean;
  conf?: any;
  data_interval_start?: string;
  data_interval_end?: string;
  last_scheduling_decision?: string;
  run_type?: string;
  note?: string;
}

// API error response structure
export interface ApiError {
  status: number;
  message: string;
  detail?: string;
  type?: string;
}

// Service response types
export interface TaskInstancesResponse {
  task_instances?: TaskInstance[];
  total_entries?: number;
}

export interface DagRunsResponse {
  dag_runs?: DagRun[];
  total_entries?: number;
}

export interface DagsResponse {
  dags?: Dag[];
  total_entries?: number;
}

// For better error handling
export interface ErrorDetails {
  status: number;
  title: string;
  message: string;
  trace?: string;
}

// Common types for filter components
export interface FilterConfig<T> {
  value: T;
  onChange: (value: T) => void;
  options?: {label: string; value: any}[];
  placeholder?: string;
}

// Audit log operation types
export enum AuditLogOperation {
  PAUSE = 'PAUSE',
  UNPAUSE = 'UNPAUSE',
  DELETE = 'DELETE',
  TRIGGER = 'TRIGGER',
  CLEAR = 'CLEAR',
  UPDATE_STATE = 'UPDATE_STATE'
}

// Audit log data structure
export interface AuditLog {
  id: number;
  userId: string;
  username: string;
  dagId: string;
  dagRunId: string | null;
  operation: AuditLogOperation; 
  operationTime: string;
  details: string | null;
}

// Service response types
export interface AuditLogsResponse {
  audit_logs?: AuditLog[];
  total_entries?: number;
}