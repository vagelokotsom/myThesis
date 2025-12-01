import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Alert, Badge } from 'react-bootstrap';
import { FaServer, FaUsers, FaDocker, FaCube, FaPlus } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';

const Dashboard = () => {
  const { user, isAuthenticated } = useAuth();
  // Get user role from auth context
  const userRole = user?.role || 'STUDENT';

  const fetchDashboardData = React.useCallback(async () => {
    try {
      setLoading(true);
      if (userRole === 'TEACHER') {
        const containers = await fetchTeacherStatistics();
        const recent = (containers || [])
          .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
          .slice(0, 5);
        setDashboardData(prev => ({
          ...prev,
          recentActivities: recent.map(c => ({
            action: c.name,
            description: c.kubernetesPodName,
            timestamp: c.createdAt,
            status: c.status
          })),
          recentContainers: recent,
          systemStatus: { kubernetes: 'healthy', database: 'healthy', ssh: 'healthy' }
        }));
      } else {
        const containers = await fetchStudentStatistics();
        const recent = (containers || [])
          .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
          .slice(0, 5);
        setDashboardData(prev => ({
          ...prev,
          recentActivities: recent.map(c => ({
            action: c.name,
            description: c.kubernetesPodName,
            timestamp: c.createdAt,
            status: c.status
          })),
          recentContainers: recent
        }));
      }
      setError(null);
    } catch (err) {
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [userRole, isAuthenticated, user]);
  const [dashboardData, setDashboardData] = useState({
    statistics: {
      totalPods: 0,
      runningPods: 0,
      totalContainers: 0,
      activeContainers: 0,
      totalUsers: 0,
      activeUsers: 0,
      templates: 0
    },
    recentActivities: [],
    recentContainers: [],
    systemStatus: {
      kubernetes: 'unknown',
      database: 'unknown',
      ssh: 'unknown'
    },
    quickActions: []
  });
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  // Redirect admin to superadmin dashboard
  useEffect(() => {
    if (isAuthenticated && user?.role === 'ROLE_ADMIN') {
      navigate('/superadmin', { replace: true });
      return;
    }
  }, [isAuthenticated, user, navigate]);

  useEffect(() => {
    if (isAuthenticated && user) {
      fetchDashboardData();
    }
    const interval = setInterval(() => {
      if (isAuthenticated && user) {
        fetchDashboardData();
      }
    }, 30000);
    return () => clearInterval(interval);
  }, [isAuthenticated, user, fetchDashboardData]);

  // ...existing code...

  const fetchTeacherStatistics = async () => {
    try {
      api.setToken(user?.token);
      const [students, containers, containerTemplates, imageTemplates] = await Promise.all([
        api.getAllStudents(),
        api.getAllContainers(),
        api.getContainerTemplates(),
        api.getImageTemplates()
      ]);

      const newStatistics = {
        totalPods: containers.length,
        runningPods: containers.filter(pod => pod.status === 'Running').length,
        totalContainers: containers.length,
        activeContainers: containers.filter(c => c.status === 'Running').length,
        totalUsers: students.length,
        activeUsers: students.length,
        templates: (containerTemplates?.length || 0) + (imageTemplates?.length || 0)
      };

      setDashboardData(prev => ({
        ...prev,
        statistics: newStatistics,
        quickActions: [
          { label: 'Manage Templates', icon: FaDocker, action: () => navigate('/container-templates'), color: 'success' },
          { label: 'Student Containers', icon: FaCube, action: () => navigate('/student-containers'), color: 'primary' }
        ]
      }));

      return containers;
    } catch (error) {
      setDashboardData(prev => ({
        ...prev,
        statistics: {
          totalPods: 0,
          runningPods: 0,
          totalContainers: 0,
          activeContainers: 0,
          totalUsers: 0,
          activeUsers: 0,
          templates: 0
        }
      }));
      return [];
    }
  };

  const fetchStudentStatistics = async () => {
    try {
      api.setToken(user?.token);
      const containers = await api.getMyContainers();

      setDashboardData(prev => ({
        ...prev,
        statistics: {
          totalContainers: containers.length,
          activeContainers: containers.filter(c => c.status === 'running').length,
          templates: 0,
          totalPods: 0,
          runningPods: 0,
          totalUsers: 0,
          activeUsers: 0
        },
        quickActions: [
          { label: 'My Containers', icon: FaDocker, action: () => navigate('/student-containers'), color: 'primary' },
          { label: 'Create Container', icon: FaPlus, action: () => navigate('/student-containers'), color: 'success' }
        ]
      }));
    } catch (error) {
      console.error('Failed to fetch student statistics:', error);
      setDashboardData(prev => ({
        ...prev,
        statistics: {
          totalContainers: 0,
          activeContainers: 0,
          templates: 0,
          totalPods: 0,
          runningPods: 0,
          totalUsers: 0,
          activeUsers: 0
        }
      }));
    }
  };

  const fetchRecentActivities = async () => {
    try {
      const userToken = user?.token;
      const response = await api.getWithToken('/activities/recent', userToken);
      setDashboardData(prev => ({
        ...prev,
        recentActivities: response || []
      }));
    } catch (error) {
      console.error('Failed to fetch recent activities:', error);
      setDashboardData(prev => ({
        ...prev,
        recentActivities: []
      }));
    }
  };

  const fetchStudentActivities = async () => {
    try {
      const userToken = user?.token;
      const response = await api.getWithToken('/activities/my-activities', userToken);
      setDashboardData(prev => ({
        ...prev,
        recentActivities: response || []
      }));
    } catch (error) {
      console.error('Failed to fetch student activities:', error);
      setDashboardData(prev => ({
        ...prev,
        recentActivities: []
      }));
    }
  };

  const fetchSystemStatus = async () => {
    try {
      const userToken = user?.token;
      const response = await api.getWithToken('/system/status', userToken);
      setDashboardData(prev => ({
        ...prev,
        systemStatus: response || {
          kubernetes: 'unknown',
          database: 'unknown',
          ssh: 'unknown'
        }
      }));
    } catch (error) {
      console.error('Failed to fetch system status:', error);
      setDashboardData(prev => ({
        ...prev,
        systemStatus: {
          kubernetes: 'unknown',
          database: 'unknown',
          ssh: 'unknown'
        }
      }));
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'healthy':
      case 'running':
      case 'active':
        return 'success';
      case 'warning':
      case 'degraded':
        return 'warning';
      case 'error':
      case 'failed':
      case 'inactive':
        return 'danger';
      default:
        return 'secondary';
    }
  };

  // Removed unused getResourceUsageColor

  const StatCard = ({ title, value, icon: Icon, subtitle, color = 'primary' }) => (
    <Card className="h-100 border-0 shadow-sm">
      <Card.Body className="d-flex align-items-center">
        <div className={`bg-${color} text-white rounded-circle p-3 me-3`}>
          <Icon size={24} />
        </div>
        <div className="flex-grow-1">
          <h3 className="mb-1 fw-bold">{value}</h3>
          <p className="mb-0 text-muted">{title}</p>
          {subtitle && <small className="text-muted">{subtitle}</small>}
        </div>
      </Card.Body>
    </Card>
  );

  if (loading) {
    return (
      <Container fluid className="py-4">
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-2">Loading dashboard...</p>
        </div>
      </Container>
    );
  }

  return (
    <Container fluid className="py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1 className="h3 mb-0">
          {userRole === 'TEACHER' ? 'Teacher Dashboard' : 'Student Dashboard'}
        </h1>
        <Button variant="outline-primary" onClick={fetchDashboardData} disabled={loading}>
          <i className="fas fa-sync-alt me-2"></i>
          Refresh
        </Button>
      </div>

      {error && (
        <Alert variant="danger" dismissible onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Statistics Cards */}
      <Row className="g-4 mb-4">
        {userRole === 'TEACHER' ? (
          <>
            <Col md={6} lg={3}>
              <StatCard
                title="Total Pods"
                value={dashboardData.statistics.totalPods}
                subtitle={`${dashboardData.statistics.runningPods} running`}
                icon={FaCube}
                color="primary"
              />
            </Col>
            <Col md={6} lg={3}>
              <StatCard
                title="Containers"
                value={dashboardData.statistics.totalContainers}
                subtitle={`${dashboardData.statistics.activeContainers} active`}
                icon={FaDocker}
                color="success"
              />
            </Col>
            <Col md={6} lg={3}>
              <StatCard
                title="Users"
                value={dashboardData.statistics.totalUsers}
                subtitle={`${dashboardData.statistics.activeUsers} active`}
                icon={FaUsers}
                color="info"
              />
            </Col>
            <Col md={6} lg={3}>
              <StatCard
                title="Templates"
                value={dashboardData.statistics.templates}
                subtitle="Available"
                icon={FaServer}
                color="warning"
              />
            </Col>
          </>
        ) : (
          <>
            <Col md={6}>
              <StatCard
                title="My Containers"
                value={dashboardData.statistics.totalContainers}
                subtitle={`${dashboardData.statistics.activeContainers} running`}
                icon={FaDocker}
                color="primary"
              />
            </Col>
            <Col md={6}>
              <StatCard
                title="SSH Sessions"
                value="0"
                subtitle="Active connections"
                icon={FaServer}
                color="success"
              />
            </Col>
          </>
        )}
      </Row>

      <Row className="g-4">
        {/* Quick Actions */}
        <Col lg={userRole === 'TEACHER' ? 8 : 12}>
          <Card className="border-0 shadow-sm">
            <Card.Header className="bg-white border-bottom">
              <h5 className="mb-0">Quick Actions</h5>
            </Card.Header>
            <Card.Body>
              <Row className="g-3">
                {dashboardData.quickActions.map((action, index) => (
                  <Col md={6} lg={userRole === 'TEACHER' ? 3 : 6} key={index}>
                    <Button
                      variant={`outline-${action.color}`}
                      className="w-100 p-3 h-100 d-flex flex-column align-items-center justify-content-center"
                      onClick={action.action}
                    >
                      <action.icon size={24} className="mb-2" />
                      {action.label}
                    </Button>
                  </Col>
                ))}
              </Row>
            </Card.Body>
          </Card>
        </Col>

        {/* System Status - Teacher Only */}
        {userRole === 'TEACHER' && (
          <Col lg={4}>
            <Card className="border-0 shadow-sm">
              <Card.Header className="bg-white border-bottom">
                <h5 className="mb-0">System Status</h5>
              </Card.Header>
              <Card.Body>
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <span>Kubernetes</span>
                  <Badge bg={getStatusColor(dashboardData.systemStatus.kubernetes)}>
                    {dashboardData.systemStatus.kubernetes}
                  </Badge>
                </div>
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <span>Database</span>
                  <Badge bg={getStatusColor(dashboardData.systemStatus.database)}>
                    {dashboardData.systemStatus.database}
                  </Badge>
                </div>
                <div className="d-flex justify-content-between align-items-center">
                  <span>SSH Server</span>
                  <Badge bg={getStatusColor(dashboardData.systemStatus.ssh)}>
                    {dashboardData.systemStatus.ssh}
                  </Badge>
                </div>
              </Card.Body>
            </Card>
          </Col>
        )}
      </Row>

      {/* Recent Activities */}
      <Row className="mt-4">
        <Col>
          <Card className="border-0 shadow-sm">
            <Card.Header className="bg-white border-bottom">
              <h5 className="mb-0">Recent Containers</h5>
            </Card.Header>
            <Card.Body>
              {dashboardData.recentContainers.length > 0 ? (
                <div className="list-group list-group-flush">
                  {dashboardData.recentContainers.slice(0, 5).map((container, index) => (
                    <div key={index} className="list-group-item border-0 px-0">
                      <div className="d-flex justify-content-between align-items-start">
                        <div>
                          <h6 className="mb-1">{container.name}</h6>
                          <p className="mb-1 text-muted">{container.kubernetesPodName || 'Pod not assigned'}</p>
                          <small className="text-muted">{container.createdAt ? new Date(container.createdAt).toLocaleString() : ''}</small>
                        </div>
                        <Badge bg={getStatusColor(container.status)}>
                          {container.status}
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center text-muted py-4">
                  <p>No recent activities</p>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default Dashboard;