import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Layout from './components/Layout';
import DagsList from './components/DagsList';
import DagRuns from './components/DagRuns';
import DagDetails from './components/DagDetails';
import TaskInstances from './components/TaskInstances';
import AuditLogs from './components/AuditLogs';
import DagAuditLogs from './components/DagAuditLogs';
import Login from './components/Login';
import ProtectedRoute from './components/ProtectedRoute';
import { ThemeProvider as MuiThemeProvider, createTheme } from '@mui/material';
import { ThemeProvider, useTheme } from './contexts/ThemeContext';
import { AuthProvider } from './contexts/AuthContext';

function AppContent() {
  const { mode } = useTheme();

  // Create theme based on current mode
  const theme = createTheme({
    palette: {
      mode: mode,
    },
  });

  return (
    <MuiThemeProvider theme={theme}>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          
          {/* Protected Routes */}
          <Route path="/" element={
            <ProtectedRoute>
              <Layout>
                <DagsList />
              </Layout>
            </ProtectedRoute>
          } />
          
          <Route path="/dags/:dagId/details" element={
            <ProtectedRoute>
              <Layout>
                <DagDetails />
              </Layout>
            </ProtectedRoute>
          } />
          
          <Route path="/dags/:dagId/runs" element={
            <ProtectedRoute>
              <Layout>
                <DagRuns />
              </Layout>
            </ProtectedRoute>
          } />
          
          <Route path="/dags/:dagId/runs/:runId/tasks" element={
            <ProtectedRoute>
              <Layout>
                <TaskInstances />
              </Layout>
            </ProtectedRoute>
          } />
          
          <Route path="/audit-logs" element={
            <ProtectedRoute>
              <Layout>
                <AuditLogs />
              </Layout>
            </ProtectedRoute>
          } />
          
          {/* New route for DAG-specific audit logs */}
          <Route path="/dags/:dagId/audit-logs" element={
            <ProtectedRoute>
              <Layout>
                <DagAuditLogs />
              </Layout>
            </ProtectedRoute>
          } />
          
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </MuiThemeProvider>
  );
}

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
