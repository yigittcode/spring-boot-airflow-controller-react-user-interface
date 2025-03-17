import axios, { AxiosInstance, AxiosRequestConfig, AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { logApiError } from './errorHandling';

// API Client özellikleri için tip tanımlaması
interface ApiClientOptions {
  baseURL: string;
  timeout?: number;
  withCredentials?: boolean;
}

// Default ayarlar
const defaultOptions: ApiClientOptions = {
  baseURL: 'http://localhost:8008/api/v1',
  timeout: 30000,
  withCredentials: true,
};

// A map to track all pending requests
const pendingRequests = new Map<string, any>();

// Create an axios instance with default configuration
const createApiClient = (options: ApiClientOptions = defaultOptions): AxiosInstance => {
  const client = axios.create({
    baseURL: options.baseURL,
    timeout: options.timeout,
    withCredentials: options.withCredentials,
    headers: {
      'Content-Type': 'application/json'
    }
  });
  
  // Map to track active requests
  const activeRequests = new Map<string, AbortController>();
  
  // Unique request ID generator
  const getRequestId = (config: AxiosRequestConfig): string => {
    const { method, url, params } = config;
    return `${method}:${url}:${JSON.stringify(params || {})}`;
  };
  
  // Request interceptor to handle caching and abort control
  client.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      // Create a new abort controller for this request
      const controller = new AbortController();
      
      // Add the signal to the request config
      config.signal = controller.signal;
      
      // Generate a unique ID for this request
      const requestId = getRequestId(config);
      
      // If there's already an active request with this ID, abort it
      if (activeRequests.has(requestId)) {
        const existingController = activeRequests.get(requestId);
        if (existingController) {
          existingController.abort();
        }
      }
      
      // Store the new controller
      activeRequests.set(requestId, controller);
      
      // Add cleanup when the request is complete
      const originalSuccess = config.onSuccess;
      config.onSuccess = (response: AxiosResponse) => {
        // Mark the request as completed
        activeRequests.delete(requestId);
        if (originalSuccess) {
          return originalSuccess(response);
        }
        return response;
      };
      
      const originalError = config.onError;
      config.onError = (error: AxiosError) => {
        // Mark the request as completed
        activeRequests.delete(requestId);
        if (originalError) {
          return originalError(error);
        }
        throw error;
      };
      
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );
  
  // Handle errors but leave token refresh to AuthContext
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      // Just log and pass the error
      if (error.response?.status === 401) {
        console.debug('401 Unauthorized error - will be handled by AuthContext if needed');
      }
      
      return Promise.reject(error);
    }
  );
  
  return client;
};

// Lazily initialize API client when needed
let apiClient: AxiosInstance | null = null;

// Get or create an API client instance
export const getApiClient = (): AxiosInstance => {
  if (!apiClient) {
    apiClient = createApiClient();
    setupInterceptors();
  }
  return apiClient;
};

// Set up the interceptors for request and response
const setupInterceptors = () => {
  // Add a request interceptor for authentication and caching
  apiClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      // Get auth token from localStorage
      const authData = localStorage.getItem('auth');
      if (authData) {
        try {
          const { accessToken } = JSON.parse(authData);
          if (accessToken && config.headers) {
            config.headers['Authorization'] = `Bearer ${accessToken}`;
          }
        } catch (error) {
          // Error parsing auth data
        }
      }
      
      // Generate a unique request ID
      const requestId = `${config.method}-${config.url}-${Date.now()}`;
      if (config.headers) {
        config.headers['X-Request-ID'] = requestId;
      }
      
      // Mark request as pending
      pendingRequests.set(requestId, new Date().getTime());
      
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );
  
  // Add a response interceptor for error handling and cache management
  apiClient.interceptors.response.use(
    (response) => {
      // Mark request as completed
      if (response.config.headers?.['X-Request-ID']) {
        const requestId = response.config.headers['X-Request-ID'] as string;
        pendingRequests.delete(requestId);
      }
      
      return response;
    },
    (error: AxiosError) => {
      // Mark request as completed
      if (error.config?.headers?.['X-Request-ID']) {
        const requestId = error.config.headers['X-Request-ID'] as string;
        pendingRequests.delete(requestId);
      }
      
      logApiError(error, 'API Request');
      
      // Let AuthContext handle token refresh, just pass the error
      return Promise.reject(error);
    }
  );
};

// Reset the API client and clean up all resources
export const resetApiClient = (): void => {
  console.debug('Resetting API client and cleaning all resources');
  
  // Abort all pending requests
  pendingRequests.forEach((value, key) => {
    try {
      if (value instanceof AbortController) {
        // If value is an AbortController, abort it
        value.abort();
        console.debug(`Aborted request: ${key}`);
      } else if (typeof value === 'number') {
        // If value is a timestamp, just log it
        const duration = Date.now() - value;
        console.debug(`Pending request was active for ${duration}ms: ${key}`);
      }
    } catch (error) {
      // Just log, don't throw
      console.debug(`Error cleaning up request ${key}:`, error);
    }
  });
  
  // Clear all pending requests tracking
  pendingRequests.clear();
  
  // Reset the API client instance
  if (apiClient) {
    // Remove all interceptors
    const axiosInst = axios.create();
    const reqInterceptorsCount = (axiosInst.interceptors.request as any).handlers?.length || 0;
    const resInterceptorsCount = (axiosInst.interceptors.response as any).handlers?.length || 0;
    
    // Eject all request interceptors
    for (let i = 0; i < reqInterceptorsCount; i++) {
      try {
        apiClient.interceptors.request.eject(i);
      } catch (e) {
        // Ignore ejection errors
      }
    }
    
    // Eject all response interceptors
    for (let i = 0; i < resInterceptorsCount; i++) {
      try {
        apiClient.interceptors.response.eject(i);
      } catch (e) {
        // Ignore ejection errors
      }
    }
    
    // Set to null for garbage collection
    apiClient = null;
  }
  
  console.debug('API client reset and resources cleaned up');
};

// Default export for backward compatibility
export default getApiClient;