import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import { toast } from "react-hot-toast";
import { Card, CardContent, CardTitle } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { useAuth } from "../contexts/AuthContext";
import ApiService from "../services/api";

export default function Dashboard() {
  const { user, isTeacher, isStudent } = useAuth();
  const [containerTemplates, setContainerTemplates] = useState([]);
  const [myContainers, setMyContainers] = useState([]);
  const [sshConnections, setSshConnections] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      
      if (isTeacher()) {
        // Load templates for teachers
        const templates = await ApiService.getMyTemplates();
        setContainerTemplates(templates || []);
      } else if (isStudent()) {
        // Load containers and SSH connections for students
        const [containers, connections] = await Promise.all([
          ApiService.getMyContainers(),
          ApiService.getSshConnections()
        ]);
        setMyContainers(containers || []);
        setSshConnections(connections || []);
      }
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const getGreeting = () => {
    const time = new Date().getHours();
    if (time < 12) return "Good morning";
    if (time < 18) return "Good afternoon";
    return "Good evening";
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6 p-4">
      <h1 className="text-2xl font-bold">
        {getGreeting()}, {user?.username}! {isTeacher() ? "👨‍🏫" : "👨‍🎓"}
      </h1>

      {isTeacher() && (
        <>
          <Card>
            <CardTitle className="mb-2">Your Container Templates</CardTitle>
            <CardContent>
              {containerTemplates.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <p>No container templates yet.</p>
                  <p>Create your first template to get started!</p>
                </div>
              ) : (
                <div className="grid md:grid-cols-2 gap-4">
                  {containerTemplates.map((template) => (
                    <div key={template.id} className="border rounded p-4 shadow-sm bg-gray-50">
                      <h3 className="font-semibold text-lg">{template.name}</h3>
                      <p className="text-sm text-gray-600 mb-2">{template.description}</p>
                      <div className="flex items-center justify-between">
                        <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
                          {template.category}
                        </span>
                        <span className="text-xs text-gray-500">
                          {template.sshEnabled ? "SSH Enabled" : "No SSH"}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardTitle className="mb-2">Quick Actions</CardTitle>
            <CardContent className="flex gap-4 flex-wrap">
              <Link to="/courses">
                <Button>➕ Create Template</Button>
              </Link>
              <Link to="/pods">
                <Button>📋 Manage Templates</Button>
              </Link>
              <Button disabled>📊 Monitor Usage (Coming soon)</Button>
            </CardContent>
          </Card>
        </>
      )}

      {isStudent() && (
        <>
          <Card>
            <CardTitle className="mb-2">My Containers</CardTitle>
            <CardContent>
              {myContainers.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <p>No containers yet.</p>
                  <p>Create your first container to get started!</p>
                </div>
              ) : (
                <div className="grid md:grid-cols-2 gap-4">
                  {myContainers.map((container) => (
                    <div key={container.id} className="border rounded p-4 shadow-sm bg-gray-50">
                      <h3 className="font-semibold text-lg">{container.instanceName}</h3>
                      <p className="text-sm text-gray-600 mb-2">
                        Based on: {container.template?.name}
                      </p>
                      <div className="flex items-center justify-between">
                        <span className={`text-xs px-2 py-1 rounded ${
                          container.status === 'Running' ? 'bg-green-100 text-green-800' :
                          container.status === 'Stopped' ? 'bg-red-100 text-red-800' :
                          'bg-yellow-100 text-yellow-800'
                        }`}>
                          {container.status}
                        </span>
                        <span className="text-xs text-gray-500">
                          {new Date(container.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardTitle className="mb-2">SSH Connections</CardTitle>
            <CardContent>
              {sshConnections.length === 0 ? (
                <div className="text-center py-4 text-gray-500">
                  <p>No active SSH connections</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {sshConnections.map((connection) => (
                    <div key={connection.id} className="flex items-center justify-between p-3 bg-gray-50 rounded">
                      <div>
                        <span className="font-medium">{connection.containerName}</span>
                        <span className="text-sm text-gray-500 ml-2">
                          Port: {connection.sshPort}
                        </span>
                      </div>
                      <span className="text-xs text-green-600">Active</span>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardTitle className="mb-2">Quick Actions</CardTitle>
            <CardContent className="flex gap-4 flex-wrap">
              <Link to="/pods">
                <Button>➕ Create Container</Button>
              </Link>
              <Link to="/pods">
                <Button>📋 Manage Containers</Button>
              </Link>
              <Button disabled>📚 Browse Templates (Coming soon)</Button>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
