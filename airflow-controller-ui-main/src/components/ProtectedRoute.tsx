import React, { useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { resetApiClient } from '../utils/apiClient';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  // Reset API client on page changes (clear cache)
  useEffect(() => {
    resetApiClient();
  }, [location.pathname]);

  // Check user credentials
  if (!isAuthenticated || !user) {
    // Remember where user wanted to go
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  
  return <>{children}</>;
};

export default ProtectedRoute; 