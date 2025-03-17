import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Paper,
  Typography,
  Box,
  Grid,
  Chip,
  Divider,
  CircularProgress,
  Alert,
  Button,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableRow,
  IconButton,
  Tooltip
} from '@mui/material';
import { 
  ArrowBack, 
  PlayArrow, 
  Pause, 
  Schedule, 
  Timeline, 
  CalendarToday,
  Person,
  Code,
  List,
  InfoOutlined,
  History
} from '@mui/icons-material';
import { getDagService } from '../utils/api';
import { DagDetail, TimeDelta } from '../types';
import { extractErrorMessage, logApiError } from '../utils/errorHandling';
import BackButton from './common/BackButton';

/**
 * DagDetails component - displays detailed information about a DAG
 */
export default function DagDetails() {
  const { dagId } = useParams<{ dagId: string }>();
  const navigate = useNavigate();
  const [dagDetails, setDagDetails] = useState<DagDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!dagId) return;
    
    const fetchDagDetails = async () => {
      setLoading(true);
      try {
        const dagService = getDagService();
        const response = await dagService.getDagDetails(dagId);
        setDagDetails(response.data);
        setError(null);
      } catch (error: any) {
        const errorMessage = extractErrorMessage(error);
        setError(`Error fetching DAG details: ${errorMessage}`);
        logApiError(error, 'Fetch DAG Details');
      } finally {
        setLoading(false);
      }
    };

    fetchDagDetails();
  }, [dagId]);

  const formatScheduleInterval = (scheduleInterval?: TimeDelta): string => {
    if (!scheduleInterval) return 'Not scheduled';
    
    // Handle cron expressions
    if (scheduleInterval.__type === 'CronExpression' || scheduleInterval.__type === 'cron') {
      // Try to extract cron value if available
      if (typeof scheduleInterval === 'object' && scheduleInterval.value) {
        return `Cron: ${scheduleInterval.value}`;
      }
      return `Cron expression`;
    }

    // Handle delta with days
    if (scheduleInterval.days) {
      return `${scheduleInterval.days} day(s)`;
    }
    
    // Handle delta with seconds
    if (scheduleInterval.seconds) {
      const hours = Math.floor(scheduleInterval.seconds / 3600);
      const minutes = Math.floor((scheduleInterval.seconds % 3600) / 60);
      const seconds = scheduleInterval.seconds % 60;
      
      const parts = [];
      if (hours) parts.push(`${hours} hour(s)`);
      if (minutes) parts.push(`${minutes} minute(s)`);
      if (seconds && !hours && !minutes) parts.push(`${seconds} second(s)`);
      
      return parts.join(', ');
    }
    
    // Default to showing the type
    if (scheduleInterval.__type) {
      return `${scheduleInterval.__type}`;
    }
    
    // Last resort: try JSON.stringify
    try {
      return JSON.stringify(scheduleInterval);
    } catch (e) {
      return 'Custom schedule';
    }
  };

  const formatDateTime = (dateTime?: string): string => {
    if (!dateTime) return 'Not available';
    try {
      // Create options for date formatting with time zone
      const options: Intl.DateTimeFormatOptions = {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        timeZoneName: 'short'
      };
      
      // Convert to Date object and format
      return new Date(dateTime).toLocaleString(undefined, options);
    } catch (error) {
      return 'Invalid date format';
    }
  };

  const handleTogglePause = async () => {
    if (!dagId || !dagDetails) return;

    try {
      const dagService = getDagService();
      await dagService.togglePause(dagId, !dagDetails.is_paused);
      
      // Update local state to provide immediate feedback
      setDagDetails({
        ...dagDetails,
        is_paused: !dagDetails.is_paused
      });
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      setError(`Error toggling DAG state: ${errorMessage}`);
      logApiError(error, 'Toggle DAG State');
    }
  };

  const handleViewDagRuns = () => {
    navigate(`/dags/${dagId}/runs`);
  };

  const handleViewAuditLogs = () => {
    navigate(`/dags/${dagId}/audit-logs`);
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 2 }}>
        <BackButton to="/" />
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      </Box>
    );
  }

  if (!dagDetails) {
    return (
      <Box sx={{ p: 2 }}>
        <BackButton to="/" />
        <Alert severity="warning" sx={{ mt: 2 }}>
          No DAG details available for {dagId}
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 2 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <BackButton to="/" />
        <Typography variant="h5" sx={{ ml: 2 }}>
          {dagDetails.dag_display_name || dagDetails.dag_id}
        </Typography>
        <Box sx={{ flexGrow: 1 }} />
        <Button
          variant="outlined"
          color="primary"
          startIcon={<History />}
          onClick={handleViewAuditLogs}
          sx={{ mr: 2 }}
        >
          View Audit Logs
        </Button>
        <Button
          variant="outlined"
          color="primary"
          startIcon={<List />}
          onClick={handleViewDagRuns}
          sx={{ mr: 2 }}
        >
          View DAG Runs
        </Button>
        <Button
          variant="contained"
          color={dagDetails.is_paused ? "primary" : "secondary"}
          startIcon={dagDetails.is_paused ? <PlayArrow /> : <Pause />}
          onClick={handleTogglePause}
        >
          {dagDetails.is_paused ? "Unpause" : "Pause"}
        </Button>
      </Box>

      {/* DAG Status */}
      <Box sx={{ mb: 4 }}>
        <Grid container spacing={2}>
          <Grid item>
            <Chip 
              label={dagDetails.is_active ? "Active" : "Inactive"} 
              color={dagDetails.is_active ? "success" : "default"} 
            />
          </Grid>
          <Grid item>
            <Chip 
              label={dagDetails.is_paused ? "Paused" : "Running"} 
              color={dagDetails.is_paused ? "warning" : "info"} 
            />
          </Grid>
          {dagDetails.is_subdag && (
            <Grid item>
              <Chip label="Subdag" color="secondary" />
            </Grid>
          )}
          {dagDetails.tags && Array.isArray(dagDetails.tags) && dagDetails.tags.length > 0 && 
            dagDetails.tags.map((tag, index) => (
              <Grid item key={tag.name || `tag-${index}`}>
                <Chip label={tag.name || `Tag ${index + 1}`} />
              </Grid>
            ))
          }
        </Grid>
      </Box>

      <Grid container spacing={3}>
        {/* Basic Info */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom display="flex" alignItems="center">
                <InfoOutlined sx={{ mr: 1 }} /> Basic Information
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableBody>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        DAG ID
                      </TableCell>
                      <TableCell>{dagDetails.dag_id}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Description
                      </TableCell>
                      <TableCell>{dagDetails.description || 'No description'}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        File Location
                      </TableCell>
                      <TableCell sx={{ wordBreak: 'break-all' }}>{dagDetails.fileloc}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Owners
                      </TableCell>
                      <TableCell>
                        {dagDetails.owners && Array.isArray(dagDetails.owners) && dagDetails.owners.length > 0 
                          ? dagDetails.owners.join(', ') 
                          : 'No owners specified'}
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Schedule Info */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom display="flex" alignItems="center">
                <Schedule sx={{ mr: 1 }} /> Schedule Information
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableBody>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Schedule Interval
                      </TableCell>
                      <TableCell>
                        {formatScheduleInterval(dagDetails.schedule_interval)}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Timetable Description
                      </TableCell>
                      <TableCell>
                        {dagDetails.timetable_description || 'Not available'}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Next DAG Run
                      </TableCell>
                      <TableCell>
                        {formatDateTime(dagDetails.next_dagrun)}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Last Parsed
                      </TableCell>
                      <TableCell>
                        {formatDateTime(dagDetails.last_parsed_time)}
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Concurrency Settings */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom display="flex" alignItems="center">
                <Timeline sx={{ mr: 1 }} /> Concurrency Settings
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableBody>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Max Active Runs
                      </TableCell>
                      <TableCell>
                        {dagDetails.max_active_runs !== undefined && dagDetails.max_active_runs !== null
                          ? dagDetails.max_active_runs 
                          : 'Default'}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Max Active Tasks
                      </TableCell>
                      <TableCell>
                        {dagDetails.max_active_tasks !== undefined && dagDetails.max_active_tasks !== null
                          ? dagDetails.max_active_tasks 
                          : 'Default'}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Has Task Concurrency Limits
                      </TableCell>
                      <TableCell>
                        {dagDetails.has_task_concurrency_limits ? 'Yes' : 'No'}
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Additional Info */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom display="flex" alignItems="center">
                <Code sx={{ mr: 1 }} /> Additional Information
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableBody>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Has Import Errors
                      </TableCell>
                      <TableCell>
                        {dagDetails.has_import_errors ? 'Yes' : 'No'}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                        Default View
                      </TableCell>
                      <TableCell>
                        {dagDetails.default_view || 'Grid'}
                      </TableCell>
                    </TableRow>
                    {dagDetails.root_dag_id && (
                      <TableRow>
                        <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                          Root DAG ID
                        </TableCell>
                        <TableCell>
                          {dagDetails.root_dag_id}
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
} 