import { Box, CssBaseline, AppBar, Toolbar, Typography, Drawer, Stack, IconButton, Tooltip, Button, List, ListItem, ListItemIcon, ListItemText, Divider } from '@mui/material';
import { Brightness4, Brightness7, Logout, Dashboard, History, PlayArrow } from '@mui/icons-material';
import { ReactNode } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import { useAuth } from '../contexts/AuthContext';
import { getAuditLogService } from '../services';

// Constants
const DRAWER_WIDTH = 240;

// Styles
const LAYOUT_STYLES = {
  container: { 
    display: 'flex', 
    width: '100vw', 
    height: '100vh' 
  },
  appBar: { 
    zIndex: (theme: any) => theme.zIndex.drawer + 1 
  },
  appTitle: { 
    cursor: 'pointer' 
  },
  drawer: {
    width: DRAWER_WIDTH,
    flexShrink: 0,
    '& .MuiDrawer-paper': { 
      width: DRAWER_WIDTH, 
      boxSizing: 'border-box' 
    },
  },
  main: { 
    flexGrow: 1, 
    p: 3, 
    width: `calc(100% - ${DRAWER_WIDTH}px)`,
    overflowX: 'auto'
  }
};

interface LayoutProps {
  children: ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { mode, toggleTheme } = useTheme();
  const { logout, user } = useAuth();
  const auditLogService = getAuditLogService();
  const isAdmin = auditLogService.hasAdminRole();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // For sidebar menu active state
  const isActive = (path: string) => {
    return location.pathname === path;
  };

  return (
    <Box sx={{ display: 'flex', width: '100vw', height: '100vh' }}>
      <CssBaseline />
      
      {/* Top App Bar */}
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
          <Typography 
            variant="h6" 
            noWrap 
            component="div" 
            onClick={() => navigate('/')} 
            sx={{ cursor: 'pointer' }}
          >
            Airflow Controller
          </Typography>
          
          {/* User info & controls */}
          <Stack direction="row" spacing={2} alignItems="center">
            {user && (
              <Typography variant="body2" color="inherit">
                {user.preferred_username || user.sub}
              </Typography>
            )}
            
            {/* Theme control */}
            <Tooltip title={`Switch to ${mode === 'dark' ? 'light' : 'dark'} mode`}>
              <IconButton color="inherit" onClick={toggleTheme} size="small">
                {mode === 'dark' ? <Brightness7 /> : <Brightness4 />}
              </IconButton>
            </Tooltip>
            
            {/* Logout button */}
            <Tooltip title="Logout">
              <IconButton color="inherit" onClick={handleLogout} size="small">
                <Logout />
              </IconButton>
            </Tooltip>
          </Stack>
        </Toolbar>
      </AppBar>
      
      {/* Left Sidebar */}
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar /> {/* Spacer to push content below app bar */}
        <Box sx={{ overflow: 'auto' }}>
          <List>
            <ListItem 
              button 
              onClick={() => navigate('/')}
              selected={isActive('/')}
            >
              <ListItemIcon>
                <Dashboard />
              </ListItemIcon>
              <ListItemText primary="DAGs" />
            </ListItem>
            
            <ListItem 
              button 
              onClick={() => navigate('/audit-logs')}
              selected={isActive('/audit-logs')}
            >
              <ListItemIcon>
                <History />
              </ListItemIcon>
              <ListItemText primary="Audit Logs" />
            </ListItem>
          </List>
          <Divider />
          
          {/* Admin section */}
          {isAdmin && (
            <Box sx={{ mt: 2 }}>
              <Typography 
                variant="caption" 
                color="text.secondary" 
                sx={{ pl: 2, textTransform: 'uppercase' }}
              >
                Admin
              </Typography>
              <List>
                {/* If there are any admin-only features, add them here */}
              </List>
            </Box>
          )}
        </Box>
      </Drawer>
      
      {/* Main Content Area */}
      <Box component="main" sx={{ 
        flexGrow: 1, 
        p: 3, 
        width: `calc(100% - ${DRAWER_WIDTH}px)`,
        overflowX: 'auto'
      }}>
        <Toolbar /> {/* Spacer to push content below app bar */}
        {children}
      </Box>
    </Box>
  );
} 