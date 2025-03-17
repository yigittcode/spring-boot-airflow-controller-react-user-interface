import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Typography, Paper, Table, TableBody, TableCell, TableContainer, 
  TableHead, TableRow, Chip, Box, CircularProgress, TablePagination,
  Card, CardContent, Alert, Divider, Grid, IconButton, Tooltip, Button
} from '@mui/material';
import { AuditLog, AuditLogOperation } from '../types';
import { getAuditLogService } from '../services';
import { format } from 'date-fns';
import { ArrowBack, InfoOutlined } from '@mui/icons-material';

// Operation types and their colors
const OPERATION_COLORS: Record<AuditLogOperation, string> = {
  [AuditLogOperation.PAUSE]: 'warning',
  [AuditLogOperation.UNPAUSE]: 'success',
  [AuditLogOperation.DELETE]: 'error',
  [AuditLogOperation.TRIGGER]: 'primary',
  [AuditLogOperation.CLEAR]: 'secondary',
  [AuditLogOperation.UPDATE_STATE]: 'info',
};

const DagAuditLogs: React.FC = () => {
  const { dagId } = useParams<{ dagId: string }>();
  const navigate = useNavigate();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState<boolean>(false);
  const auditLogService = getAuditLogService();
  
  // Pagination
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  
  // Extract user info from auth
  useEffect(() => {
    setIsAdmin(auditLogService.hasAdminRole());
  }, []);
  
  // Load logs for specific DAG
  useEffect(() => {
    const fetchLogs = async () => {
      if (!dagId) {
        setError('DAG ID is missing');
        setLoading(false);
        return;
      }
      
      try {
        setLoading(true);
        const data = await auditLogService.getAuditLogsForDag(dagId);
        
        // Sort logs by operationTime in descending order (newest first)
        const sortedLogs = [...data].sort((a, b) => 
          new Date(b.operationTime).getTime() - new Date(a.operationTime).getTime()
        );
        
        setLogs(sortedLogs);
        setError(null);
      } catch (err) {
        setError('Failed to load audit logs. Please try again later.');
      } finally {
        setLoading(false);
      }
    };
    
    fetchLogs();
  }, [dagId]);
  
  // Format the date in a user-friendly way
  const formatDate = (dateString: string) => {
    try {
      return format(new Date(dateString), 'yyyy-MM-dd HH:mm:ss');
    } catch (e) {
      return dateString;
    }
  };
  
  // Handle page change
  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };
  
  // Handle rows per page change
  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  // Navigate back to DAG details
  const handleBackToDagDetails = () => {
    navigate(`/dags/${dagId}/details`);
  };
  
  // Paginate the logs
  const paginatedLogs = logs.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);

  return (
    <div>
      <Box sx={{ mb: 4, display: 'flex', alignItems: 'center' }}>
        <Button 
          startIcon={<ArrowBack />} 
          onClick={handleBackToDagDetails}
          sx={{ mr: 2 }}
          variant="outlined"
        >
          Back to DAG
        </Button>
        <Typography variant="h4" gutterBottom sx={{ mb: 0 }}>
          Audit Logs for DAG: {dagId}
        </Typography>
      </Box>
      
      <Typography variant="body1" color="text.secondary" paragraph>
        {isAdmin 
          ? 'View all operations performed on this DAG by all users.'
          : 'View your recent operations on this DAG.'
        }
      </Typography>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      
      <Card variant="outlined" sx={{ mb: 4 }}>
        <CardContent>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : logs.length === 0 ? (
            <Alert severity="info">No audit logs found for this DAG.</Alert>
          ) : (
            <>
              <TableContainer component={Paper} variant="outlined">
                <Table sx={{ minWidth: 650 }} size="medium">
                  <TableHead>
                    <TableRow>
                      <TableCell>Operation</TableCell>
                      <TableCell>Time</TableCell>
                      {isAdmin && <TableCell>User</TableCell>}
                      <TableCell>DAG Run ID</TableCell>
                      <TableCell>Details</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {paginatedLogs.map((log) => (
                      <TableRow key={log.id} hover>
                        <TableCell>
                          <Chip 
                            label={log.operation} 
                            color={OPERATION_COLORS[log.operation] as any} 
                            size="small" 
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>{formatDate(log.operationTime)}</TableCell>
                        {isAdmin && (
                          <TableCell>{log.username}</TableCell>
                        )}
                        <TableCell>
                          {log.dagRunId ? (
                            <Box sx={{ display: 'flex', alignItems: 'center' }}>
                              {log.dagRunId}
                              <Tooltip title="View DAG Run details">
                                <IconButton 
                                  size="small" 
                                  onClick={() => navigate(`/dags/${log.dagId}/runs`)}
                                >
                                  <InfoOutlined fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            </Box>
                          ) : (
                            <Typography variant="body2" color="text.secondary">
                              N/A
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          {log.details || (
                            <Typography variant="body2" color="text.secondary">
                              No details
                            </Typography>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              
              <TablePagination
                rowsPerPageOptions={[5, 10, 25, 50]}
                component="div"
                count={logs.length}
                rowsPerPage={rowsPerPage}
                page={page}
                onPageChange={handleChangePage}
                onRowsPerPageChange={handleChangeRowsPerPage}
              />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default DagAuditLogs; 