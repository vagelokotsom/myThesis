import { useState, useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Button } from "../components/ui/button";
import api from "../services/api";
import { toast } from "react-hot-toast";

export default function Dashboard() {
  const { user, isTeacher, isStudent } = useAuth();
  const [stats, setStats] = useState({
    containers: 0,
    templates: 0,
    runningContainers: 0,
    sshConnections: 0
  });
  const [recentContainers, setRecentContainers] = useState([]);
  const [recentTemplates, setRecentTemplates] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      
      if (isTeacher()) {
        // Teacher dashboard data
        const [containers, templates] = await Promise.all([
          api.getAllContainers(),
          api.getMyTemplates()
        ]);
        
        const runningCount = containers.filter(c => c.status === 'Running').length;
        
        setStats({
          containers: containers.length,
          templates: templates.length,
          runningContainers: runningCount,
          students: new Set(containers.map(c => c.owner?.id)).size
        });
        
        setRecentContainers(containers.slice(0, 5));
        setRecentTemplates(templates.slice(0, 5));
        
      } else {
        // Student dashboard data
        const [containers, sshConnections] = await Promise.all([
          api.getMyContainers(),
          api.getSshConnections().catch(() => [])
        ]);
        
        const runningCount = containers.filter(c => c.status === 'Running').length;
        
        setStats({
          containers: containers.length,
          runningContainers: runningCount,
          sshConnections: sshConnections.length,
          availableTemplates: 0 // Will load if needed
        });
        
        setRecentContainers(containers.slice(0, 5));
      }
    } catch (error) {
      console.error("Failed to load dashboard data:", error);
      toast.error("Failed to load dashboard data");
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      'Running': 'text-green-600',
      'Pending': 'text-yellow-600',
      'Stopped': 'text-red-600',
      'Creating': 'text-blue-600'
    };
    return colors[status] || 'text-gray-600';
  };

  const getStatusIcon = (status) => {
    const icons = {
      'Running': '🟢',
      'Pending': '🟡',
      'Stopped': '🔴',
      'Creating': '🔵'
    };
    return icons[status] || '❓';
  };

  if (loading) {
    return (
      <div className="p-6 flex justify-center">
        <div className="text-lg">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">Welcome back, {user?.username}!</h1>
          <p className="text-gray-600 mt-1">
            {isTeacher() ? "Teacher Dashboard" : "Student Dashboard"}
          </p>
        </div>
        <Button 
          onClick={loadDashboardData}
          className="bg-blue-600 hover:bg-blue-700"
        >
          Refresh
        </Button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              {isTeacher() ? "Total Containers" : "My Containers"}
            </CardTitle>
            <div className="text-2xl">📦</div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.containers}</div>
            <p className="text-xs text-gray-600">
              {isTeacher() ? "Across all students" : "Created by you"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Running Containers</CardTitle>
            <div className="text-2xl">🟢</div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.runningContainers}</div>
            <p className="text-xs text-gray-600">Currently active</p>
          </CardContent>
        </Card>

        {isTeacher() && (
          <>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Templates</CardTitle>
                <div className="text-2xl">🎨</div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.templates}</div>
                <p className="text-xs text-gray-600">Created by you</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Active Students</CardTitle>
                <div className="text-2xl">👥</div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.students || 0}</div>
                <p className="text-xs text-gray-600">With containers</p>
              </CardContent>
            </Card>
          </>
        )}

        {isStudent() && (
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">SSH Connections</CardTitle>
              <div className="text-2xl">🔑</div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.sshConnections}</div>
              <p className="text-xs text-gray-600">Active connections</p>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {isTeacher() ? (
              <>
                <Button 
                  onClick={() => window.location.href = '/templates'}
                  className="bg-purple-600 hover:bg-purple-700"
                >
                  Manage Templates
                </Button>
                <Button 
                  onClick={() => window.location.href = '/kubernetes'}
                  className="bg-indigo-600 hover:bg-indigo-700"
                >
                  Kubernetes Console
                </Button>
                <Button 
                  onClick={() => window.location.href = '/pods'}
                  className="bg-green-600 hover:bg-green-700"
                >
                  View All Containers
                </Button>
                <Button 
                  onClick={() => window.location.href = '/courses'}
                  className="bg-orange-600 hover:bg-orange-700"
                >
                  Manage Courses
                </Button>
              </>
            ) : (
              <>
                <Button 
                  onClick={() => window.location.href = '/containers'}
                  className="bg-blue-600 hover:bg-blue-700"
                >
                  Create Container
                </Button>
                <Button 
                  onClick={() => window.location.href = '/pods'}
                  className="bg-green-600 hover:bg-green-700"
                >
                  My Containers
                </Button>
              </>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Containers */}
        <Card>
          <CardHeader>
            <CardTitle>
              {isTeacher() ? "Recent Student Containers" : "My Recent Containers"}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {recentContainers.length > 0 ? (
              <div className="space-y-3">
                {recentContainers.map((container) => (
                  <div key={container.id} className="flex justify-between items-center border-b pb-2 last:border-b-0">
                    <div>
                      <div className="font-medium">{container.name}</div>
                      {isTeacher() && container.owner && (
                        <div className="text-sm text-gray-600">Owner: {container.owner.username}</div>
                      )}
                    </div>
                    <div className={`flex items-center space-x-1 ${getStatusColor(container.status)}`}>
                      <span>{getStatusIcon(container.status)}</span>
                      <span className="text-sm">{container.status}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-4 text-gray-500">
                {isTeacher() ? "No student containers found" : "No containers found"}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Recent Templates (Teachers only) */}
        {isTeacher() && (
          <Card>
            <CardHeader>
              <CardTitle>Recent Templates</CardTitle>
            </CardHeader>
            <CardContent>
              {recentTemplates.length > 0 ? (
                <div className="space-y-3">
                  {recentTemplates.map((template) => (
                    <div key={template.id} className="flex justify-between items-center border-b pb-2 last:border-b-0">
                      <div>
                        <div className="font-medium">{template.name}</div>
                        <div className="text-sm text-gray-600">{template.dockerImage}</div>
                      </div>
                      <div className="text-xs">
                        {template.category && (
                          <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded">
                            {template.category}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-4 text-gray-500">
                  No templates found
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* System Status for Students */}
        {isStudent() && (
          <Card>
            <CardHeader>
              <CardTitle>System Status</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span>Platform Status</span>
                  <span className="text-green-600">🟢 Operational</span>
                </div>
                <div className="flex justify-between items-center">
                  <span>SSH Service</span>
                  <span className="text-green-600">🟢 Available</span>
                </div>
                <div className="flex justify-between items-center">
                  <span>Container Service</span>
                  <span className="text-green-600">🟢 Running</span>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
