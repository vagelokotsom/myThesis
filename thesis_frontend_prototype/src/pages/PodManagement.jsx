import { useState, useEffect } from "react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { toast } from "react-hot-toast";
import api from "../services/api";
import { useAuth } from "../contexts/AuthContext";

export default function PodManagement() {
  const { isTeacher, isStudent } = useAuth();
  const [pods, setPods] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [containers, setContainers] = useState([]);
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [showCreateContainer, setShowCreateContainer] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      
      if (isTeacher()) {
        // Teachers see all containers and can manage them
        const [containersResponse, templatesResponse] = await Promise.all([
          api.getAllContainers(),
          api.getContainerTemplates()
        ]);
        setContainers(containersResponse || []);
        setTemplates(templatesResponse || []);
      } else {
        // Students see only their containers and SSH-enabled templates
        const [containersResponse, templatesResponse] = await Promise.all([
          api.getMyContainers(),
          api.getSshEnabledTemplates()
        ]);
        setContainers(containersResponse || []);
        setTemplates(templatesResponse || []);
      }
    } catch (error) {
      console.error("Failed to load data:", error);
      toast.error("Failed to load container data");
    } finally {
      setLoading(false);
    }
  };

  const handleCreateContainer = async () => {
    if (!selectedTemplate) {
      toast.error("Please select a template");
      return;
    }

    try {
      await api.createContainerFromTemplate(selectedTemplate);
      toast.success("Container created successfully");
      setShowCreateContainer(false);
      setSelectedTemplate("");
      loadData();
    } catch (error) {
      console.error("Failed to create container:", error);
      toast.error("Failed to create container");
    }
  };

  const handleStartContainer = async (containerId) => {
    try {
      await api.startContainer(containerId);
      toast.success("Container started");
      loadData();
    } catch (error) {
      console.error("Failed to start container:", error);
      toast.error("Failed to start container");
    }
  };

  const handleStopContainer = async (containerId) => {
    try {
      await api.stopContainer(containerId);
      toast.success("Container stopped");
      loadData();
    } catch (error) {
      console.error("Failed to stop container:", error);
      toast.error("Failed to stop container");
    }
  };

  const handleDeleteContainer = async (containerId) => {
    if (!window.confirm("Are you sure you want to delete this container?")) {
      return;
    }

    try {
      await api.deleteContainer(containerId);
      toast.success("Container deleted");
      loadData();
    } catch (error) {
      console.error("Failed to delete container:", error);
      toast.error("Failed to delete container");
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      'Running': 'text-green-600',
      'Pending': 'text-yellow-600',
      'Stopped': 'text-red-600',
      'Creating': 'text-blue-600',
      'Error': 'text-red-800'
    };
    return colors[status] || 'text-gray-600';
  };

  const getStatusIcon = (status) => {
    const icons = {
      'Running': '🟢',
      'Pending': '🟡', 
      'Stopped': '🔴',
      'Creating': '🔵',
      'Error': '❌'
    };
    return icons[status] || '❓';
  };

  if (loading) {
    return (
      <div className="p-6 flex justify-center">
        <div className="text-lg">Loading containers...</div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">
          {isTeacher() ? "Pod & Container Management" : "My Containers"}
        </h1>
        {!isTeacher() && (
          <Button 
            onClick={() => setShowCreateContainer(true)}
            className="bg-blue-600 hover:bg-blue-700"
          >
            Create New Container
          </Button>
        )}
      </div>

      {/* Create Container Modal for Students */}
      {showCreateContainer && (
        <Card>
          <CardTitle className="mb-4 p-4">Create New Container</CardTitle>
          <CardContent>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-2">Select Template</label>
                <select
                  value={selectedTemplate}
                  onChange={(e) => setSelectedTemplate(e.target.value)}
                  className="w-full border rounded-md px-3 py-2"
                >
                  <option value="">Choose a template...</option>
                  {templates.map((template) => (
                    <option key={template.id} value={template.id}>
                      {template.name} ({template.dockerImage})
                    </option>
                  ))}
                </select>
              </div>
              
              <div className="flex space-x-2">
                <Button onClick={handleCreateContainer} className="bg-green-600 hover:bg-green-700">
                  Create Container
                </Button>
                <Button 
                  onClick={() => setShowCreateContainer(false)}
                  className="bg-gray-500 hover:bg-gray-600"
                >
                  Cancel
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Containers/Pods List */}
      <Card>
        <CardTitle className="mb-4 p-4">
          {isTeacher() ? "All Student Containers" : "My Containers"}
        </CardTitle>
        <CardContent>
          <div className="space-y-4">
            {containers.map((container) => (
              <div
                key={container.id}
                className="flex justify-between items-center border-b py-4 last:border-b-0"
              >
                <div className="flex-1">
                  <div className="flex items-center space-x-3">
                    <span className="font-semibold text-lg">{container.name}</span>
                    <div className={`flex items-center space-x-1 ${getStatusColor(container.status)}`}>
                      <span>{getStatusIcon(container.status)}</span>
                      <span className="font-medium">{container.status}</span>
                    </div>
                  </div>
                  
                  <div className="text-sm text-gray-600 mt-1">
                    <div>Pod: {container.kubernetesPodName}</div>
                    {isTeacher() && container.owner && (
                      <div>Owner: {container.owner.username}</div>
                    )}
                  </div>
                </div>

                <div className="flex gap-2">
                  {container.status === 'Stopped' && (
                    <Button 
                      onClick={() => handleStartContainer(container.id)}
                      className="bg-green-500 hover:bg-green-600"
                      size="sm"
                    >
                      Start
                    </Button>
                  )}
                  
                  {container.status === 'Running' && (
                    <Button 
                      onClick={() => handleStopContainer(container.id)}
                      className="bg-yellow-500 hover:bg-yellow-600"
                      size="sm"
                    >
                      Stop
                    </Button>
                  )}
                  
                  <Button 
                    onClick={() => handleDeleteContainer(container.id)}
                    className="bg-red-500 hover:bg-red-600"
                    size="sm"
                  >
                    Delete
                  </Button>
                </div>
              </div>
            ))}

            {containers.length === 0 && (
              <div className="text-center py-8 text-gray-500">
                {isTeacher() 
                  ? "No student containers found."
                  : "No containers found. Create your first container to get started."
                }
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Additional Info for Teachers */}
      {isTeacher() && (
        <Card>
          <CardTitle className="mb-4 p-4">Quick Actions</CardTitle>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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
                Kubernetes Management
              </Button>
              <Button 
                onClick={loadData}
                className="bg-gray-600 hover:bg-gray-700"
              >
                Refresh Data
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
