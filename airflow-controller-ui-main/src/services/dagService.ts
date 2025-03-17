import { getApiClient } from '../utils/apiClient';
import { Dag } from '../types';

// Types for API parameters
export interface DagQueryParams {
  page?: number;
  size?: number;
  search?: string;
  isActive?: boolean;
  isPaused?: boolean;
  orderBy?: string;
}

// Service for DAG-related operations
const dagService = {
  getDags: (params?: DagQueryParams) => 
    getApiClient().get('/dags', { params }),
  
  getDag: (dagId: string) => 
    getApiClient().get(`/dags/${dagId}`),
  
  getDagDetails: (dagId: string) =>
    getApiClient().get(`/dags/${dagId}/details`),
  
  getDagTasks: (dagId: string) =>
    getApiClient().get(`/dags/${dagId}/tasks`),
  
  togglePause: (dagId: string, isPaused: boolean) => 
    getApiClient().patch(`/dags/${dagId}`, { is_paused: isPaused }),
  
  deleteDag: (dagId: string) => 
    getApiClient().delete(`/dags/${dagId}`)
};

export default dagService; 