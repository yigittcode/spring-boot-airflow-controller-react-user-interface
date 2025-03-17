import React, { createContext, useContext, useState, useEffect, ReactNode, useRef } from 'react';
import axios, { InternalAxiosRequestConfig } from 'axios';
import { resetApiClient } from '../utils/apiClient';

// Define auth state
interface AuthState {
  isAuthenticated: boolean;
  user: any | null;
  accessToken: string | null;
  refreshToken: string | null;
  tokenExpiry: number | null;
}

// Type definition for Context API
interface AuthContextType extends AuthState {
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => void;
  refreshAuth: () => Promise<boolean>;
}

// Default AuthContext values
const defaultAuthContext: AuthContextType = {
  isAuthenticated: false,
  user: null,
  accessToken: null,
  refreshToken: null,
  tokenExpiry: null,
  login: async () => false,
  logout: () => {},
  refreshAuth: async () => false,
};

// Create Auth Context
const AuthContext = createContext<AuthContextType>(defaultAuthContext);

// Set up axios interceptor
const setupAxiosInterceptors = (
  token: string,
  requestInterceptorRef: React.MutableRefObject<number | null>
) => {
  // Remove previous interceptor if it exists
  if (requestInterceptorRef.current !== null) {
    axios.interceptors.request.eject(requestInterceptorRef.current);
  }
  
  // Add a new request interceptor and save the reference
  requestInterceptorRef.current = axios.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      if (token && config.headers) {
        config.headers['Authorization'] = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );
};

// Create Context provider
export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>(() => {
    // Load auth state from localStorage
    const storedAuth = localStorage.getItem('auth');
    if (storedAuth) {
      try {
        const parsedAuth = JSON.parse(storedAuth);
        return {
          isAuthenticated: true,
          user: parsedAuth.user,
          accessToken: parsedAuth.accessToken,
          refreshToken: parsedAuth.refreshToken,
          tokenExpiry: parsedAuth.tokenExpiry,
        };
      } catch (error) {
        // Error parsing stored auth state
      }
    }
    
    // Return empty state if no data in localStorage or if there was an error
    return {
      isAuthenticated: false,
      user: null,
      accessToken: null,
      refreshToken: null,
      tokenExpiry: null,
    };
  });

  // Refs for interceptor references
  const requestInterceptorRef = useRef<number | null>(null);

  // Add reference for tokenExpiry to prevent unnecessary effect triggers
  const tokenExpiryRef = useRef<number | null>(null);

  // Update localStorage when auth state changes
  useEffect(() => {
    if (authState.isAuthenticated && authState.accessToken) {
      localStorage.setItem('auth', JSON.stringify({
        user: authState.user,
        accessToken: authState.accessToken,
        refreshToken: authState.refreshToken,
        tokenExpiry: authState.tokenExpiry,
      }));
      
      // Set up axios interceptor with token
      setupAxiosInterceptors(authState.accessToken, requestInterceptorRef);
    } else {
      localStorage.removeItem('auth');
      
      // Clear interceptors if session is closed
      if (requestInterceptorRef.current !== null) {
        axios.interceptors.request.eject(requestInterceptorRef.current);
        requestInterceptorRef.current = null;
      }
    }
  }, [authState]);

  // Login function
  const login = async (username: string, password: string): Promise<boolean> => {
    try {
      // Clear state before making a new request, especially for logout-login scenarios
      setAuthState(prevState => ({
        ...prevState,
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null,
        tokenExpiry: null,
      }));
      
      const response = await axios.post('http://localhost:8008/api/v1/auth/login', {
        username,
        password,
      }, {
        headers: {
          'Content-Type': 'application/json',
        }
      });
      
      if (response.data) {
        // API returns in snake_case format, we convert to camelCase
        const accessToken = response.data.access_token;
        const refreshToken = response.data.refresh_token;
        const expiresIn = response.data.expires_in;
        
        if (!accessToken) {
          return false;
        }
        
        const tokenExpiry = Date.now() + expiresIn * 1000;
        
        // Decode JWT and get user info
        const user = parseJwt(accessToken);
        
        setAuthState(prevState => ({
          ...prevState,  // Preserve any additional state properties
          isAuthenticated: true,
          user,
          accessToken,
          refreshToken,
          tokenExpiry,
        }));
        
        return true;
      } else {
        return false;
      }
    } catch (error) {
      return false;
    }
  };

  // Token refresh function - enhanced with better logging
  const refreshAuth = async (): Promise<boolean> => {
    if (!authState.refreshToken) {
      console.debug('No refresh token available, cannot refresh');
      return false;
    }
    
    try {
      console.debug('Starting token refresh');
      const response = await axios.post('http://localhost:8008/api/v1/auth/token', 
        authState.refreshToken,
        {
          headers: {
            'Content-Type': 'text/plain'
          }
        }
      );
      
      // API returns in snake_case format
      const accessToken = response.data.access_token;
      const refreshToken = response.data.refresh_token;
      const expiresIn = response.data.expires_in;
      const tokenExpiry = Date.now() + expiresIn * 1000;
      
      // Save to ref to prevent effect re-trigger
      tokenExpiryRef.current = tokenExpiry;
      
      console.debug(`Token refreshed successfully. Expires in ${expiresIn} seconds`);
      
      // Decode JWT and get user info
      const user = parseJwt(accessToken);
      
      // Update state with fresh data
      setAuthState(prevState => ({
        ...prevState,  // Preserve any additional state properties
        isAuthenticated: true,
        user,
        accessToken,
        refreshToken,
        tokenExpiry,
      }));
      
      return true;
    } catch (error) {
      console.error('Token refresh failed:', error);
      // Clean up on error
      logout();
      return false;
    }
  };

  // Logout function - enhanced with complete cleanup
  const logout = () => {
    console.debug('Logging out and cleaning up auth state');
    
    // Clear token expiry ref
    tokenExpiryRef.current = null;
    
    // Clear any token refresh timer
    if (refreshTimerIdRef.current !== null) {
      window.clearTimeout(refreshTimerIdRef.current);
      refreshTimerIdRef.current = null;
    }
    
    // Reset API client and abort any pending requests
    resetApiClient();
    
    // Clear state
    setAuthState(prevState => ({
      ...prevState,
      isAuthenticated: false,
      user: null,
      accessToken: null,
      refreshToken: null,
      tokenExpiry: null,
    }));
    
    // Remove from localStorage
    localStorage.removeItem('auth');
  };

  // Ref to track timer ID for cleanup
  const refreshTimerIdRef = useRef<number | null>(null);

  // JWT decode function
  const parseJwt = (token: string) => {
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
      return null;
    }
  };

  // Check token validity and refresh based on exact expiry from API
  useEffect(() => {
    const setupRefreshTimer = () => {
      // Clear any existing timer
      if (refreshTimerIdRef.current !== null) {
        window.clearTimeout(refreshTimerIdRef.current);
        refreshTimerIdRef.current = null;
      }
      
      // If authenticated and we have token expiry time
      if (authState.isAuthenticated && authState.tokenExpiry) {
        // Update ref
        tokenExpiryRef.current = authState.tokenExpiry;
        
        const timeToExpiry = authState.tokenExpiry - Date.now();
        
        // Only set timer if token will expire in the future
        if (timeToExpiry > 0) {
          // Calculate dynamic refresh margin (5% of total token lifetime)
          // From the API response, we know expiresIn is in seconds
          const expiresIn = authState.tokenExpiry ? (authState.tokenExpiry - Date.now()) / 1000 : 300;
          const refreshMargin = Math.max(10, Math.floor(expiresIn * 0.05) * 1000); // At least 10 seconds
          
          // Refresh before expiry using the dynamic margin
          const refreshDelay = Math.max(0, timeToExpiry - refreshMargin);
          
          console.debug(`Token expires in ${Math.round(timeToExpiry/1000)}s. Setting refresh timer for ${Math.round(refreshDelay/1000)}s from now (${refreshMargin/1000}s margin)`);
          
          // Set timer to refresh token
          refreshTimerIdRef.current = window.setTimeout(async () => {
            console.debug('Token refresh timer triggered');
            refreshTimerIdRef.current = null; // Reset ref
            await refreshAuth();
          }, refreshDelay);
        } else {
          // Token already expired, refreshing immediately
          console.debug('Token already expired, refreshing immediately');
          refreshAuth();
        }
      }
    };
    
    // Setup timer on initial load and when auth state changes
    setupRefreshTimer();
    
    // Cleanup timer on unmount
    return () => {
      if (refreshTimerIdRef.current !== null) {
        window.clearTimeout(refreshTimerIdRef.current);
        refreshTimerIdRef.current = null;
      }
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authState.isAuthenticated, authState.tokenExpiry]); // Include tokenExpiry but disable eslint check for refreshAuth

  return (
    <AuthContext.Provider
      value={{
        ...authState,
        login,
        logout,
        refreshAuth,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

// Export the useAuth hook
export const useAuth = () => useContext(AuthContext);

// Export the default context for direct imports
export default AuthContext; 