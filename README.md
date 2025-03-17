# Airflow Controller UI

A modern, responsive web interface for managing Apache Airflow workflows, built with React and TypeScript.

## 📋 Overview

The Airflow Controller UI provides an intuitive interface for monitoring and controlling Apache Airflow DAGs (Directed Acyclic Graphs) and their executions. This application allows users to:

- View and manage Airflow DAGs
- Monitor DAG runs and task instances
- View task logs
- Manage workflow executions
- View audit logs for tracking system changes
- Authenticate securely with JWT-based authentication

## 🚀 Technologies

- **Frontend Framework**: React 19
- **Language**: TypeScript
- **Build Tool**: Vite
- **UI Framework**: Material UI 6
- **Routing**: React Router 7
- **API Communication**: Axios
- **Authentication**: JWT (JSON Web Tokens)
- **State Management**: React Context API
- **Date Handling**: date-fns

## 🏗️ Project Architecture

The application follows a modern React architecture with a focus on reusable components, clean separation of concerns, and type safety.

### Directory Structure

```
airflow-controller-ui/
├── public/             # Static assets
├── src/
│   ├── assets/         # Images, fonts, etc.
│   ├── components/     # UI components
│   │   └── common/     # Shared UI components
│   ├── contexts/       # React context providers
│   ├── hooks/          # Custom React hooks
│   ├── pages/          # Page components
│   ├── services/       # API service layers
│   ├── types/          # TypeScript type definitions
│   ├── utils/          # Utility functions
│   ├── App.tsx         # Main application component
│   └── main.tsx        # Application entry point
├── index.html          # HTML entry point
├── package.json        # Project dependencies
└── tsconfig.json       # TypeScript configuration
```

### Key Features

#### Authentication System

- JWT-based authentication with automatic token refresh
- Protected routes with role-based access control
- Persistent sessions with secure storage
- Token expiration management
- Interceptor-based API authorization

#### Theme Support

- Light and dark mode support
- User preference persistence
- Dynamic theme switching

#### API Integration

- Centralized API client with interceptors
- Service-based API organization
- Error handling and retry mechanisms
- Request cancellation support

## 💻 Getting Started

### Prerequisites

- Node.js (v18.0.0 or later)
- npm or yarn
- Apache Airflow backend service

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yigittq/spring-boot-airflow-controller-react-user-interface.git
   cd airflow-controller-ui
   ```

2. Install dependencies:
   ```bash
   npm install
   # or
   yarn install
   ```

3. Configure the API endpoint:
   - Open `src/utils/apiClient.ts`
   - Update the `baseURL` to point to your Airflow API

4. Start the development server:
   ```bash
   npm run dev
   # or
   yarn dev
   ```

5. Build for production:
   ```bash
   npm run build
   # or
   yarn build
   ```

## 🛠️ Development

### Code Quality

The project uses ESLint for code quality and TypeScript for type checking. Run linting using:

```bash
npm run lint
# or
yarn lint
```

### Project Structure Guidelines

- Components should be placed in the `components` directory
- Pages/routes should be placed in the `pages` directory
- Shared types should be defined in the `types` directory
- API interactions should be isolated in the `services` directory
- Context providers should be in the `contexts` directory

## 🔒 Security Features

- JWT token-based authentication
- Automatic token refresh before expiration
- Token storage in localStorage with proper state management
- Request authorization via Axios interceptors
- API request cancellation to prevent race conditions

## 🔄 API Integration

The application communicates with the Airflow backend API via RESTful endpoints:

- `/api/v1/auth/*` - Authentication endpoints
- `/api/v1/dags/*` - DAG management endpoints
- `/api/v1/dags/{dagId}/runs/*` - DAG run endpoints
- `/api/v1/dags/{dagId}/runs/{runId}/tasks/*` - Task instance endpoints
- `/api/v1/audit-logs/*` - Audit log endpoints

## 📊 Performance Considerations

- Efficient token refresh mechanism
- Appropriate caching strategies
- Abortable requests to prevent stale data
- Optimized rendering with proper React patterns


