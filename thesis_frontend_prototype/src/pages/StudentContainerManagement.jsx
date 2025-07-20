import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { toast } from 'react-hot-toast';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';

export default function StudentContainerManagement() {
  const { user, isTeacher } = useAuth();
  const [students, setStudents] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [containers, setContainers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showSshModal, setShowSshModal] = useState(false);
  const [selectedContainer, setSelectedContainer] = useState(null);
  const [sshInfo, setSshInfo] = useState(null);
  const [selectedStudent, setSelectedStudent] = useState('');
  const [selectedTemplate, setSelectedTemplate] = useState('');
  const [creating, setCreating] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    if (!isTeacher()) {
      toast.error('Access denied. Teachers only.');
      return;
    }
    
    // Ensure we have the user and token before loading data
    if (user && user.token) {
      loadData();
    }
  }, [user]); // Add user as dependency

  // Auto-refresh containers every 30 seconds
  useEffect(() => {
    if (!isTeacher() || !user?.token) return;
    
    const interval = setInterval(() => {
      refreshContainerStatuses();
    }, 30000); // 30 seconds

    return () => clearInterval(interval);
  }, [user, isTeacher]);

  const loadData = async () => {
    try {
      setLoading(true);
      console.log('Loading data for StudentContainerManagement...');
      
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      const [studentsData, templatesData, containersData] = await Promise.all([
        api.getAllStudents(),
        api.getImageTemplates(), // Use ImageTemplates instead of ContainerTemplates
        api.getAllContainers()
      ]);
      
      console.log('Raw students response:', studentsData);
      console.log('Raw templates response:', templatesData);
      console.log('Raw containers response:', containersData);
      
      setStudents(studentsData || []);
      setTemplates(templatesData || []);
      setContainers(containersData || []);
      
      console.log('Final state - students:', studentsData?.length || 0);
      console.log('Final state - templates:', templatesData?.length || 0);
      console.log('Final state - containers:', containersData?.length || 0);
    } catch (error) {
      console.error('Failed to load data:', error);
      toast.error('Failed to load data: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const refreshContainerStatuses = async () => {
    try {
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      // First, call the backend to refresh statuses from Kubernetes
      try {
        await api.refreshAllContainerStatuses();
        console.log('Called refresh endpoint to update statuses from Kubernetes');
      } catch (refreshError) {
        console.warn('Failed to call refresh endpoint:', refreshError);
        // Continue anyway to get current data
      }
      
      // Then fetch the updated data
      const containersData = await api.getAllContainers();
      setContainers(containersData || []);
      console.log('Container statuses refreshed:', containersData?.length || 0);
    } catch (error) {
      console.error('Failed to refresh container statuses:', error);
      // Don't show toast for auto-refresh errors to avoid spam
    }
  };

  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      await loadData();
      toast.success('Data refreshed successfully!');
    } catch (error) {
      toast.error('Failed to refresh data');
    } finally {
      setRefreshing(false);
    }
  };

  const handleCreateContainer = async () => {
    if (!selectedStudent || !selectedTemplate) {
      toast.error('Please select both a student and a template');
      return;
    }

    console.log('=== Create Container Debug ===');
    console.log('selectedStudent (raw):', selectedStudent);
    console.log('selectedTemplate (raw):', selectedTemplate);
    console.log('selectedStudent (parsed):', parseInt(selectedStudent));
    console.log('selectedTemplate (parsed):', parseInt(selectedTemplate));

    try {
      setCreating(true);
      
      // Ensure API has the current token
      if (user && user.token) {
        console.log('Setting API token for container creation:', user.token.substring(0, 20) + '...');
        api.setToken(user.token);
      } else {
        console.log('No user or token available for container creation:', { user: !!user, token: !!user?.token });
      }
      
      const imageId = parseInt(selectedTemplate);
      const studentId = parseInt(selectedStudent);
      
      console.log('Calling API with imageId:', imageId, 'studentId:', studentId);
      
      await api.createContainerForStudent(imageId, studentId);
      toast.success('Container created successfully!');
      setShowCreateModal(false);
      setSelectedStudent('');
      setSelectedTemplate('');
      await loadData(); // Reload containers
    } catch (error) {
      console.error('Failed to create container:', error);
      toast.error('Failed to create container: ' + error.message);
    } finally {
      setCreating(false);
    }
  };

  const getStudentContainers = (studentId) => {
    return containers.filter(container => container.owner?.id === studentId);
  };

  const getStudentName = (studentId) => {
    const student = students.find(s => s.id === studentId);
    return student ? student.username : 'Unknown';
  };

  const getTemplateName = (templateId) => {
    const template = templates.find(t => t.id === templateId);
    return template ? template.name : 'Unknown';
  };

  const handleShowSshInfo = async (container) => {
    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      const info = await api.getContainerSshInfo(container.id);
      setSelectedContainer(container);
      setSshInfo(info);
      setShowSshModal(true);
    } catch (error) {
      console.error('Failed to get SSH info:', error);
      toast.error('Failed to get SSH information: ' + error.message);
    }
  };

  const copyToClipboard = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      toast.success('Copied to clipboard!');
    } catch (error) {
      console.error('Failed to copy:', error);
      toast.error('Failed to copy to clipboard');
    }
  };

  if (!isTeacher()) {
    return (
      <div className="p-6">
        <Card>
          <CardContent className="p-6">
            <h2 className="text-xl font-bold text-red-600">Access Denied</h2>
            <p>This page is only accessible to teachers.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="p-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              <span className="ml-2">Loading...</span>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold">Student Container Management</h1>
        <div className="flex gap-3">
          <Button 
            onClick={handleRefresh}
            disabled={refreshing}
            variant="outline"
            className="border-gray-300 text-gray-700 hover:bg-gray-50"
          >
            {refreshing ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-600 mr-2"></div>
                Refreshing...
              </>
            ) : (
              'Refresh'
            )}
          </Button>
          <Button 
            onClick={() => setShowCreateModal(true)}
            className="bg-blue-600 hover:bg-blue-700 text-white"
          >
            Create Container for Student
          </Button>
        </div>
      </div>

      {/* Create Container Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">Create Container for Student</h2>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Select Student</label>
                <select
                  value={selectedStudent}
                  onChange={(e) => setSelectedStudent(e.target.value)}
                  className="w-full p-2 border rounded-md"
                >
                  <option value="">Choose a student...</option>
                  {students.map(student => (
                    <option key={student.id} value={student.id}>
                      {student.username} ({student.email})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Select Template</label>
                <select
                  value={selectedTemplate}
                  onChange={(e) => setSelectedTemplate(e.target.value)}
                  className="w-full p-2 border rounded-md"
                >
                  <option value="">Choose a template...</option>
                  {templates.map(template => (
                    <option key={template.id} value={template.id}>
                      {template.name} - {template.dockerImage}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex gap-2 mt-6">
              <Button
                onClick={handleCreateContainer}
                disabled={creating}
                className="bg-blue-600 hover:bg-blue-700 text-white flex-1"
              >
                {creating ? 'Creating...' : 'Create Container'}
              </Button>
              <Button
                onClick={() => setShowCreateModal(false)}
                variant="outline"
                className="flex-1"
              >
                Cancel
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Students and their containers */}
      <div className="grid gap-6">
        {students.length === 0 ? (
          <Card>
            <CardContent className="p-8 text-center">
              <div className="text-gray-400 mb-4">
                <svg className="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">No Students Found</h3>
              <p className="text-gray-500">
                No students are registered in the system yet. Students need to be registered before you can create containers for them.
              </p>
            </CardContent>
          </Card>
        ) : (
          students.map(student => {
          const studentContainers = getStudentContainers(student.id);
          
          return (
            <Card key={student.id}>
              <CardHeader>
                <CardTitle className="flex justify-between items-center">
                  <span>{student.username} ({student.email})</span>
                  <span className="text-sm text-gray-500">
                    {studentContainers.length} container(s)
                  </span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                {studentContainers.length === 0 ? (
                  <p className="text-gray-500 italic">No containers assigned</p>
                ) : (
                  <div className="space-y-2">
                    {studentContainers.map(container => (
                      <div 
                        key={container.id}
                        className="flex justify-between items-center p-3 bg-gray-50 rounded-md"
                      >
                        <div>
                          <div className="font-medium">{container.name}</div>
                          <div className="text-sm text-gray-600">
                            Template: {getTemplateName(container.imageTemplate?.id)}
                          </div>
                          <div className="text-sm text-gray-600 flex items-center">
                            Status: 
                            <span className={`ml-1 px-2 py-1 rounded-full text-xs font-medium ${
                              container.status === 'Running' ? 'bg-green-100 text-green-800' : 
                              container.status === 'Stopped' ? 'bg-red-100 text-red-800' : 
                              container.status === 'Creating' ? 'bg-yellow-100 text-yellow-800' :
                              'bg-gray-100 text-gray-800'
                            }`}>
                              {container.status}
                            </span>
                          </div>
                          <div className="text-sm text-gray-600">
                            Pod: {container.kubernetesPodName || 'Not assigned'}
                          </div>
                          {container.createdAt && (
                            <div className="text-sm text-gray-500">
                              Created: {new Date(container.createdAt).toLocaleDateString()}
                            </div>
                          )}
                        </div>
                        <div className="flex gap-2">
                          {container.status === 'Running' ? (
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleShowSshInfo(container)}
                              className="bg-green-50 border-green-200 text-green-700 hover:bg-green-100"
                            >
                              SSH Info
                            </Button>
                          ) : (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled
                              className="opacity-50"
                            >
                              {container.status}
                            </Button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          );
        }))
        }
      </div>

      {/* Quick Stats */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Statistics</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">{students.length}</div>
              <div className="text-sm text-gray-600">Total Students</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">
                {containers.filter(c => c.status === 'Running').length}
              </div>
              <div className="text-sm text-gray-600">Running Containers</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-gray-600">{containers.length}</div>
              <div className="text-sm text-gray-600">Total Containers</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-600">{templates.length}</div>
              <div className="text-sm text-gray-600">Available Templates</div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* SSH Info Modal */}
      {showSshModal && selectedContainer && sshInfo && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">SSH Connection Instructions</h2>
            
            <div className="space-y-4">
              <div className="bg-gray-50 p-4 rounded-md">
                <h3 className="font-medium mb-2">Container: {selectedContainer.name}</h3>
                <p className="text-sm text-gray-600 mb-2">Image: {sshInfo.dockerImage}</p>
                
                {sshInfo.ready ? (
                  <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div><strong>Host:</strong> {sshInfo.host}</div>
                      <div><strong>Port:</strong> {sshInfo.port}</div>
                      <div><strong>Username:</strong> {sshInfo.username}</div>
                      <div><strong>Password:</strong> {sshInfo.password}</div>
                    </div>
                    
                    <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded">
                      <p className="text-sm font-medium text-blue-800 mb-2">
                        <strong>Method 1: Direct Connection (may not work on macOS)</strong>
                      </p>
                      <div className="flex items-center gap-2">
                        <code className="text-xs bg-blue-100 p-2 rounded flex-1">
                          ssh -p {sshInfo.port} {sshInfo.username}@{sshInfo.host}
                        </code>
                        <Button
                          onClick={() => copyToClipboard(`ssh -p ${sshInfo.port} ${sshInfo.username}@${sshInfo.host}`)}
                          className="px-2 py-1 text-xs bg-blue-600 hover:bg-blue-700 text-white"
                        >
                          Copy
                        </Button>
                      </div>
                    </div>
                    
                    {sshInfo.portForwardCommand && (
                      <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded">
                        <p className="text-sm font-medium text-green-800 mb-3">
                          <strong>Method 2: Port Forward (Recommended for macOS/Minikube)</strong>
                        </p>
                        
                        {/* Step-by-step instructions */}
                        {sshInfo.stepByStepInstructions && (
                          <div className="space-y-3 mb-4">
                            <p className="text-xs font-medium text-green-700">📋 Step-by-Step Instructions:</p>
                            {Object.entries(sshInfo.stepByStepInstructions).map(([step, instruction]) => (
                              <div key={step} className="text-xs text-green-700">
                                <strong>{step.replace('step', 'Step ')}:</strong> {instruction}
                              </div>
                            ))}
                          </div>
                        )}
                        
                        {/* Command boxes */}
                        <div className="space-y-2">
                          <div>
                            <p className="text-xs text-green-700 mb-1">🔌 Port Forward Command (Run in Terminal 1):</p>
                            <div className="flex items-center gap-2">
                              <code className="text-xs bg-green-100 p-2 rounded flex-1">
                                {sshInfo.portForwardCommand}
                              </code>
                              <Button
                                onClick={() => copyToClipboard(sshInfo.portForwardCommand)}
                                className="px-2 py-1 text-xs bg-green-600 hover:bg-green-700 text-white"
                              >
                                Copy
                              </Button>
                            </div>
                          </div>
                          <div>
                            <p className="text-xs text-green-700 mb-1">🔐 SSH Command (Run in Terminal 2):</p>
                            <div className="flex items-center gap-2">
                              <code className="text-xs bg-green-100 p-2 rounded flex-1">
                                {sshInfo.portForwardSsh}
                              </code>
                              <Button
                                onClick={() => copyToClipboard(sshInfo.portForwardSsh)}
                                className="px-2 py-1 text-xs bg-green-600 hover:bg-green-700 text-white"
                              >
                                Copy
                              </Button>
                            </div>
                          </div>
                          <div>
                            <p className="text-xs text-green-700 mb-1">🔑 Password:</p>
                            <div className="flex items-center gap-2">
                              <code className="text-xs bg-green-100 p-2 rounded flex-1">
                                {sshInfo.password}
                              </code>
                              <Button
                                onClick={() => copyToClipboard(sshInfo.password)}
                                className="px-2 py-1 text-xs bg-green-600 hover:bg-green-700 text-white"
                              >
                                Copy
                              </Button>
                            </div>
                          </div>
                        </div>
                        
                        {/* Troubleshooting section */}
                        {sshInfo.troubleshooting && (
                          <div className="mt-4 p-2 bg-yellow-50 border border-yellow-200 rounded">
                            <p className="text-xs font-medium text-yellow-800 mb-2">🔧 Troubleshooting:</p>
                            <div className="space-y-1">
                              {Object.entries(sshInfo.troubleshooting).map(([issue, solution]) => (
                                <div key={issue} className="text-xs text-yellow-700">
                                  <strong>{issue.replace(/([A-Z])/g, ' $1').toLowerCase()}:</strong> {solution}
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                    
                    <div className="mt-2 p-2 bg-yellow-50 border border-yellow-200 rounded">
                      <p className="text-xs text-yellow-800">
                        <strong>Note:</strong> {sshInfo.alternativeNote || sshInfo.note}
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="p-3 bg-orange-50 border border-orange-200 rounded">
                    <p className="text-sm text-orange-800">
                      {sshInfo.message}
                    </p>
                  </div>
                )}
              </div>
            </div>

            <div className="flex gap-2 mt-6">
              {sshInfo?.ready && sshInfo.portForwardCommand && (
                <Button
                  onClick={() => copyToClipboard(`# SSH Connection Instructions for Container: ${selectedContainer.name}

# Step-by-Step Instructions:
# 1. Open a terminal/command prompt
# 2. Run the port-forward command below (keep this terminal open)
# 3. Open a new terminal window
# 4. Run the SSH command below
# 5. Enter the password when prompted

# === TERMINAL 1: Port Forwarding (Keep this running) ===
${sshInfo.portForwardCommand}

# === TERMINAL 2: SSH Connection ===
${sshInfo.portForwardSsh}

# === Password ===
# When prompted, enter: ${sshInfo.password}

# === Troubleshooting ===
# - Make sure Terminal 1 is still running the port-forward command
# - If port 8023 is in use, try changing it to 8024:22 in both commands
# - Make sure kubectl is installed and configured`)}
                  className="bg-green-600 hover:bg-green-700 text-white"
                >
                  Copy All Commands
                </Button>
              )}
              <Button
                onClick={() => setShowSshModal(false)}
                className="bg-gray-600 hover:bg-gray-700 text-white flex-1"
              >
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
