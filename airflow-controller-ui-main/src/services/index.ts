import dagService from './dagService';
import dagRunService from './dagRunService';
import taskInstanceService from './taskInstanceService';
import auditLogService from './auditLogService';

// Service accessor functions
export const getDagService = () => dagService;
export const getDagRunService = () => dagRunService;
export const getTaskInstanceService = () => taskInstanceService;
export const getAuditLogService = () => auditLogService;

export { dagService, dagRunService, taskInstanceService, auditLogService }; 