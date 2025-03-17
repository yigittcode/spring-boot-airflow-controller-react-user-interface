import axios from 'axios';
import { AuditLog } from '../types';

const API_BASE_URL = 'http://localhost:8008/api/v1';

/**
 * Get all audit logs (filtered by role)
 * Admin users will see all logs, normal users will only see their own logs
 */
const getAuditLogs = async (): Promise<AuditLog[]> => {
  try {
    const response = await axios.get(`${API_BASE_URL}/audit-logs`);
    
    // The backend should already be filtering based on roles
    // But for extra security, we'll validate on the frontend too
    const logs = response.data;
    
    // If user is not admin, double-check that they only see their own logs
    if (!hasAdminRole()) {
      const currentUserId = getCurrentUserId();
      // Extra security: Filter logs by current user ID if not admin
      return logs.filter((log: AuditLog) => log.userId === currentUserId);
    }
    
    return logs;
  } catch (error) {
    throw error;
  }
};

/**
 * Get audit logs for a specific DAG (filtered by role)
 */
const getAuditLogsForDag = async (dagId: string): Promise<AuditLog[]> => {
  try {
    const response = await axios.get(`${API_BASE_URL}/audit-logs/dag/${dagId}`);
    
    // The backend should already be filtering based on roles
    // But for extra security, we'll validate on the frontend too
    const logs = response.data;
    
    // If user is not admin, double-check that they only see their own logs
    if (!hasAdminRole()) {
      const currentUserId = getCurrentUserId();
      // Extra security: Filter logs by current user ID if not admin
      return logs.filter((log: AuditLog) => log.userId === currentUserId);
    }
    
    return logs;
  } catch (error) {
    throw error;
  }
};

/**
 * Get the current user's ID from the JWT token
 */
const getCurrentUserId = (): string => {
  try {
    const authData = localStorage.getItem('auth');
    if (!authData) return '';
    
    const { user } = JSON.parse(authData);
    if (!user) return '';
    
    return user.sub || '';
  } catch (error) {
    return '';
  }
};

/**
 * Checks if the current user has admin role
 * (Uses decoded JWT token information)
 */
const hasAdminRole = (): boolean => {
  try {
    const authData = localStorage.getItem('auth');
    if (!authData) return false;
    
    const { user } = JSON.parse(authData);
    if (!user) return false;
    
    // Check for realm_access.roles in the JWT payload
    const roles = user.realm_access?.roles || [];
    return roles.includes('airflow-admin');
  } catch (error) {
    return false;
  }
};

const auditLogService = {
  getAuditLogs,
  getAuditLogsForDag,
  hasAdminRole,
  getCurrentUserId
};

export default auditLogService; 