export interface TimeDelta {
  __type: string;
  days?: number;
  seconds?: number;
  microseconds?: number;
  value?: string;
  [key: string]: any; // Additional fields that might be present
}

export interface Tag {
  name: string;
}

export interface DagDetail {
  dag_id: string;
  dag_display_name?: string;
  root_dag_id?: string;
  is_paused: boolean;
  is_active: boolean;
  is_subdag: boolean;
  last_parsed_time?: string;
  last_pickled?: string;
  last_expired?: string;
  scheduler_lock?: boolean;
  pickle_id?: string;
  default_view?: string;
  fileloc: string;
  file_token?: string;
  owners: string[];
  description?: string;
  schedule_interval?: TimeDelta;
  timetable_description?: string;
  tags: Tag[];
  max_active_tasks?: number;
  max_active_runs?: number;
  has_task_concurrency_limits?: boolean;
  has_import_errors?: boolean;
  next_dagrun?: string;
  next_dagrun_data_interval_start?: string;
  next_dagrun_data_interval_end?: string;
} 