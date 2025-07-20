import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Alert, Badge, ProgressBar } from 'react-bootstrap';
import { FaServer, FaUsers, FaDocker, FaCube, FaPlay, FaStop, FaPlus } from 'react-icons/fa';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';

const Dashboard = () => {
  const { user, isAuthenticated } = useAuth();
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

  // Get user role from auth context
  const userRole = user?.role || 'STUDENT';

  useEffect(() => {
    console.log('=== Dashboard useEffect Debug ===');
    console.log('localStorage user:', localStorage.getItem('user'));
    console.log('localStorage authToken:', localStorage.getItem('authToken'));
    console.log('user from context:', user);
    console.log('isAuthenticated from context:', isAuthenticated);
    
    if (isAuthenticated && user) {
      fetchDashboardData();
    }
    const interval = setInterval(() => {
      if (isAuthenticated && user) {
        fetchDashboardData();
      }
    }, 30000); // Refresh every 30 seconds
    return () => clearInterval(interval);
  }, [isAuthenticated, user]);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      console.log('=== Dashboard Data Fetch Started ===');
      console.log('User object:', user);
      console.log('User role:', userRole);
      console.log('Is authenticated:', isAuthenticated);
      
      // Fetch different data based on user role
      if (userRole === 'TEACHER') {
        console.log('Fetching teacher data...');
        await Promise.all([
          fetchTeacherStatistics(),
          fetchRecentActivities(),
          fetchSystemStatus()
        ]);
      } else {
        console.log('Fetching student data...');
        await Promise.all([
          fetchStudentStatistics(),
          fetchStudentActivities()
        ]);
      }
      
      setError(null);
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const fetchTeacherStatistics = async () => {
    try {
      console.log('=== Teacher Statistics Fetch ===');
      console.log('User role:', userRole);
      console.log('User object:', user);
      
      console.log('Making API calls...');
      const userToken = user?.token;
      console.log('Using token:', userToken);
      
      const [podsRes, containersRes, usersRes, templatesRes] = await Promise.all([
        api.getWithToken('/kubernetes/pods', userToken),
        api.getWithToken('/containers', userToken),
        api.getWithToken('/users', userToken),
        api.getWithToken('/images', userToken)
      ]);

      console.log('Raw API responses:');
      console.log('- Pods:', podsRes);
      console.log('- Containers:', containersRes);
      console.log('- Users:', usersRes);
      console.log('- Templates:', templatesRes);

      const pods = podsRes || [];
      const containers = containersRes || [];
      const users = usersRes || [];
      const templates = templatesRes || [];

      console.log('Processed data:');
      console.log('- Pods length:', pods.length);
      console.log('- Containers length:', containers.length);
      console.log('- Users length:', users.length);
      console.log('- Templates length:', templates.length);

      const newStatistics = {
        totalPods: pods.length,
        runningPods: pods.filter(pod => pod.status === 'Running').length,
        totalContainers: containers.length,
        activeContainers: containers.filter(c => c.status === 'running').length,
        totalUsers: users.length,
        activeUsers: users.filter(u => u.status === 'active').length,
        templates: templates.length
      };

      console.log('New statistics object:', newStatistics);

      setDashboardData(prev => ({
        ...prev,
        statistics: newStatistics,
        quickActions: [
          { label: 'Create Pod', icon: FaPlus, action: () => navigate('/kubernetes-management'), color: 'primary' },
          { label: 'Manage Templates', icon: FaDocker, action: () => navigate('/container-templates'), color: 'success' },
          { label: 'View Users', icon: FaUsers, action: () => navigate('/user-management'), color: 'info' },
          { label: 'Pod Management', icon: FaCube, action: () => navigate('/pod-management'), color: 'warning' }
        ]
      }));

      console.log('Statistics updated successfully');
    } catch (error) {
      console.error('=== Teacher Statistics Error ===');
      console.error('Error details:', error);
      console.error('Error message:', error.message);
      console.error('Error stack:', error.stack);
      
      // Set default values if API calls fail
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
    }
  };

  const fetchStudentStatistics = async () => {
    try {
      const userToken = user?.token;
      const containersRes = await api.getWithToken('/containers/my-containers', userToken);
      const containers = containersRes || [];

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
      case 'healthy': case 'running': case 'active': return 'success';
      case 'warning': case 'degraded': return 'warning';
      case 'error': case 'failed': case 'inactive': return 'danger';
      default: return 'secondary';
    }
  };

  const getResourceUsageColor = (usage) => {
    if (usage < 50) return 'success';
    if (usage < 80) return 'warning';
    return 'danger';
  };

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
              <h5 className="mb-0">Recent Activities</h5>
            </Card.Header>
            <Card.Body>
              {dashboardData.recentActivities.length > 0 ? (
                <div className="list-group list-group-flush">
                  {dashboardData.recentActivities.slice(0, 5).map((activity, index) => (
                    <div key={index} className="list-group-item border-0 px-0">
                      <div className="d-flex justify-content-between align-items-start">
                        <div>
                          <h6 className="mb-1">{activity.action}</h6>
                          <p className="mb-1 text-muted">{activity.description}</p>
                          <small className="text-muted">{activity.timestamp}</small>
                        </div>
                        <Badge bg={getStatusColor(activity.status)}>
                          {activity.status}
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