import React, { useState, useEffect, useMemo } from 'react';
import { 
  Typography, Paper, Table, TableBody, TableCell, TableContainer, 
  TableHead, TableRow, Chip, Box, CircularProgress, TablePagination,
  Card, CardContent, Alert, Divider, TextField, MenuItem, Select, 
  InputLabel, FormControl, Grid, IconButton, InputAdornment, SelectChangeEvent
} from '@mui/material';
import { AuditLog, AuditLogOperation } from '../types';
import { getAuditLogService } from '../services';
import { format } from 'date-fns';
import { Search, Clear } from '@mui/icons-material';

// Operation types and their colors
const OPERATION_COLORS: Record<AuditLogOperation, string> = {
  [AuditLogOperation.PAUSE]: 'warning',
  [AuditLogOperation.UNPAUSE]: 'success',
  [AuditLogOperation.DELETE]: 'error',
  [AuditLogOperation.TRIGGER]: 'primary',
  [AuditLogOperation.CLEAR]: 'secondary',
  [AuditLogOperation.UPDATE_STATE]: 'info',
};

const AuditLogs: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState<boolean>(false);
  const auditLogService = getAuditLogService();
  
  // User info
  const [currentUserId, setCurrentUserId] = useState<string>('');
  
  // Pagination
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  
  // Filters
  const [operationFilter, setOperationFilter] = useState<string>('');
  const [dagIdFilter, setDagIdFilter] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState<string>('');
  
  // Get unique operation types for the dropdown
  const operationTypes = useMemo(() => {
    const types = Array.from(new Set(logs.map(log => log.operation)));
    return types.sort();
  }, [logs]);
  
  // Get unique DAG IDs for the dropdown
  const dagIds = useMemo(() => {
    const ids = Array.from(new Set(logs.map(log => log.dagId)));
    return ids.sort();
  }, [logs]);
  
  // Extract user info from auth
  useEffect(() => {
    try {
      const authData = localStorage.getItem('auth');
      if (authData) {
        const { user } = JSON.parse(authData);
        if (user && user.sub) {
          setCurrentUserId(user.sub);
        }
      }
    } catch (error) {
      console.error('Error accessing user info:', error);
    }
    
    setIsAdmin(auditLogService.hasAdminRole());
  }, []);
  
  // Load logs
  useEffect(() => {
    const fetchLogs = async () => {
      try {
        setLoading(true);
        const data = await auditLogService.getAuditLogs();
        
        // Sort logs by operationTime in descending order (newest first)
        const sortedLogs = [...data].sort((a, b) => 
          new Date(b.operationTime).getTime() - new Date(a.operationTime).getTime()
        );
        
        setLogs(sortedLogs);
        setError(null);
      } catch (err) {
        console.error('Error loading audit logs:', err);
        setError('Failed to load audit logs. Please try again later.');
      } finally {
        setLoading(false);
      }
    };
    
    fetchLogs();
  }, []);
  
  // Apply filters to logs
  const filteredLogs = useMemo(() => {
    return logs.filter(log => {
      // Filter by operation
      if (operationFilter && log.operation !== operationFilter) {
        return false;
      }
      
      // Filter by DAG ID
      if (dagIdFilter && log.dagId !== dagIdFilter) {
        return false;
      }
      
      // Filter by search term (search username only if admin, otherwise ignore)
      if (searchTerm && isAdmin) {
        const searchTermLower = searchTerm.toLowerCase();
        if (!log.username.toLowerCase().includes(searchTermLower)) {
          return false;
        }
      }
      
      return true;
    });
  }, [logs, operationFilter, dagIdFilter, searchTerm, isAdmin]);
  
  // Format the date in a user-friendly way
  const formatDate = (dateString: string) => {
    try {
      return format(new Date(dateString), 'yyyy-MM-dd HH:mm:ss');
    } catch (e) {
      return dateString;
    }
  };
  
  // Reset filters
  const handleResetFilters = () => {
    setOperationFilter('');
    setDagIdFilter('');
    setSearchTerm('');
    setPage(0);
  };
  
  // Handle operation filter change
  const handleOperationFilterChange = (event: SelectChangeEvent) => {
    setOperationFilter(event.target.value);
    setPage(0);
  };
  
  // Handle DAG ID filter change
  const handleDagIdFilterChange = (event: SelectChangeEvent) => {
    setDagIdFilter(event.target.value);
    setPage(0);
  };
  
  // Handle search term change
  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
    setPage(0);
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
  
  // Paginate the filtered logs
  const paginatedLogs = filteredLogs.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);

  return (
    <div>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" gutterBottom>
          Audit Logs
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
          {isAdmin 
            ? 'View all operations performed on DAGs by all users.'
            : 'View your recent operations on DAGs.'
          }
        </Typography>
        {isAdmin && (
          <Alert severity="info" sx={{ mt: 2, mb: 2 }}>
            As an admin, you can see logs from all users in the system.
          </Alert>
        )}
      </Box>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      
      <Card variant="outlined" sx={{ mb: 2 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            {/* Username search field - only visible to admins */}
            {isAdmin && (
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  label="Search by Username"
                  value={searchTerm}
                  onChange={handleSearchChange}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Search />
                      </InputAdornment>
                    ),
                    endAdornment: searchTerm && (
                      <InputAdornment position="end">
                        <IconButton onClick={() => setSearchTerm('')} size="small">
                          <Clear />
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                  size="small"
                />
              </Grid>
            )}
            
            {/* Adjust grid sizing based on whether admin search is visible */}
            <Grid item xs={12} sm={isAdmin ? 3 : 5}>
              <FormControl fullWidth size="small">
                <InputLabel>Operation</InputLabel>
                <Select
                  value={operationFilter}
                  onChange={handleOperationFilterChange}
                  label="Operation"
                >
                  <MenuItem value="">All Operations</MenuItem>
                  {operationTypes.map(type => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={isAdmin ? 3 : 5}>
              <FormControl fullWidth size="small">
                <InputLabel>DAG ID</InputLabel>
                <Select
                  value={dagIdFilter}
                  onChange={handleDagIdFilterChange}
                  label="DAG ID"
                >
                  <MenuItem value="">All DAGs</MenuItem>
                  {dagIds.map(id => (
                    <MenuItem key={id} value={id}>
                      {id}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={2}>
              <Box display="flex" justifyContent="flex-end">
                <IconButton onClick={handleResetFilters} title="Clear filters">
                  <Clear />
                </IconButton>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      
      <Card variant="outlined">
        <CardContent sx={{ p: 0 }}>
          {loading ? (
            <Box display="flex" justifyContent="center" alignItems="center" height="200px">
              <CircularProgress />
            </Box>
          ) : filteredLogs.length === 0 ? (
            <Box p={4} textAlign="center">
              <Typography variant="body1" color="text.secondary">
                No audit logs found matching the criteria.
              </Typography>
            </Box>
          ) : (
            <>
              <TableContainer component={Paper} elevation={0}>
                <Table sx={{ minWidth: 650 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>ID</TableCell>
                      {isAdmin && <TableCell>User</TableCell>}
                      <TableCell>DAG ID</TableCell>
                      <TableCell>DAG Run ID</TableCell>
                      <TableCell>Operation</TableCell>
                      <TableCell>Time</TableCell>
                      <TableCell>Details</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {paginatedLogs.map((log) => (
                      <TableRow key={log.id} hover>
                        <TableCell>{log.id}</TableCell>
                        {isAdmin && <TableCell>{log.username}</TableCell>}
                        <TableCell>{log.dagId}</TableCell>
                        <TableCell>{log.dagRunId || '-'}</TableCell>
                        <TableCell>
                          <Chip 
                            label={log.operation} 
                            color={(OPERATION_COLORS[log.operation] as any) || 'default'} 
                            size="small" 
                          />
                        </TableCell>
                        <TableCell>{formatDate(log.operationTime)}</TableCell>
                        <TableCell 
                          sx={{ 
                            maxWidth: '300px', 
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis'
                          }}
                        >
                          {log.details || '-'}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <Divider />
              <TablePagination
                rowsPerPageOptions={[10, 25, 50]}
                component="div"
                count={filteredLogs.length}
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

export default AuditLogs; 