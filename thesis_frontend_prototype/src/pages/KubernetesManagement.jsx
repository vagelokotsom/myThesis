import { useState, useEffect } from "react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { toast } from "react-hot-toast";
import { useAuth } from "../contexts/AuthContext";
import api from "../services/api";

export default function KubernetesManagement() {
  const { user, isTeacher } = useAuth();
  const [activeTab, setActiveTab] = useState('pods');
  const [pods, setPods] = useState([]);
  const [deployments, setDeployments] = useState([]);
  const [namespaces, setNamespaces] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedNamespace, setSelectedNamespace] = useState('default');
  
  // Create forms
  const [showCreatePod, setShowCreatePod] = useState(false);
  const [showCreateDeployment, setShowCreateDeployment] = useState(false);
  const [showCreateNamespace, setShowCreateNamespace] = useState(false);
  
  const [podForm, setPodForm] = useState({
    name: '',
    image: '',
    labels: '',
    cpuLimit: '',
    memoryLimit: '',
    cpuRequest: '',
    memoryRequest: ''
  });

  const [deploymentForm, setDeploymentForm] = useState({
    name: '',
    image: '',
    replicas: 1,
    labels: '',
    cpuLimit: '',
    memoryLimit: '',
    cpuRequest: '',
    memoryRequest: ''
  });

  const [namespaceForm, setNamespaceForm] = useState({
    name: '',
    labels: ''
  });

  useEffect(() => {
    if (!isTeacher()) {
      toast.error('Access denied. Teachers only.');
      return;
    }
    
    // Add a small delay to ensure user context is fully loaded
    const loadDataWithDelay = async () => {
      if (user && user.token) {
        await loadData();
      } else {
        console.log('Waiting for user authentication...');
      }
    };
    
    loadDataWithDelay();
  }, [activeTab, selectedNamespace, user?.token]); // Watch user.token specifically

  const loadData = async () => {
    setLoading(true);
    try {
      console.log('Loading Kubernetes data...');
      console.log('User:', user);
      console.log('User token:', user?.token ? user.token.substring(0, 20) + '...' : 'null');
      
      // Ensure API has the current token
      if (user && user.token) {
        console.log('Setting API token for Kubernetes management');
        api.setToken(user.token);
      } else {
        console.log('No user or token available for Kubernetes data loading');
        setLoading(false);
        return;
      }
      
      const namespacesData = await api.getAllNamespaces();
      setNamespaces(namespacesData || []);

      if (activeTab === 'pods') {
        const podsData = selectedNamespace === 'all' 
          ? await api.getAllPods(true)
          : await api.getPodsInNamespace(selectedNamespace);
        setPods(podsData || []);
      } else if (activeTab === 'deployments') {
        const deploymentsData = selectedNamespace === 'all'
          ? await api.getAllDeployments(true)
          : await api.getDeploymentsInNamespace(selectedNamespace);
        setDeployments(deploymentsData || []);
      }
    } catch (error) {
      console.error("Failed to load data:", error);
      toast.error("Failed to load Kubernetes resources");
    } finally {
      setLoading(false);
    }
  };

  const handleCreatePod = async (e) => {
    e.preventDefault();
    if (!podForm.name || !podForm.image) {
      toast.error("Name and image are required");
      return;
    }

    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      const labels = parseLabels(podForm.labels);
      const resources = {
        ...(podForm.cpuLimit && { 'cpu-limit': podForm.cpuLimit }),
        ...(podForm.memoryLimit && { 'memory-limit': podForm.memoryLimit }),
        ...(podForm.cpuRequest && { 'cpu-request': podForm.cpuRequest }),
        ...(podForm.memoryRequest && { 'memory-request': podForm.memoryRequest })
      };

      await api.createPod(selectedNamespace, {
        name: podForm.name,
        image: podForm.image,
        ...labels,
        ...resources
      });
      
      toast.success("Pod created successfully");
      setShowCreatePod(false);
      setPodForm({
        name: '', image: '', labels: '', cpuLimit: '', memoryLimit: '', cpuRequest: '', memoryRequest: ''
      });
      loadData();
    } catch (error) {
      console.error("Failed to create pod:", error);
      toast.error("Failed to create pod");
    }
  };

  const handleCreateDeployment = async (e) => {
    e.preventDefault();
    if (!deploymentForm.name || !deploymentForm.image) {
      toast.error("Name and image are required");
      return;
    }

    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      const labels = parseLabels(deploymentForm.labels);
      const resources = {
        ...(deploymentForm.cpuLimit && { 'cpu-limit': deploymentForm.cpuLimit }),
        ...(deploymentForm.memoryLimit && { 'memory-limit': deploymentForm.memoryLimit }),
        ...(deploymentForm.cpuRequest && { 'cpu-request': deploymentForm.cpuRequest }),
        ...(deploymentForm.memoryRequest && { 'memory-request': deploymentForm.memoryRequest })
      };

      await api.createDeployment(selectedNamespace, {
        name: deploymentForm.name,
        image: deploymentForm.image,
        replicas: deploymentForm.replicas,
        ...labels,
        ...resources
      });
      
      toast.success("Deployment created successfully");
      setShowCreateDeployment(false);
      setDeploymentForm({
        name: '', image: '', replicas: 1, labels: '', cpuLimit: '', memoryLimit: '', cpuRequest: '', memoryRequest: ''
      });
      loadData();
    } catch (error) {
      console.error("Failed to create deployment:", error);
      toast.error("Failed to create deployment");
    }
  };

  const handleCreateNamespace = async (e) => {
    e.preventDefault();
    if (!namespaceForm.name) {
      toast.error("Namespace name is required");
      return;
    }

    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      const labels = parseLabels(namespaceForm.labels);
      await api.createNamespace(namespaceForm.name, labels);
      
      toast.success("Namespace created successfully");
      setShowCreateNamespace(false);
      setNamespaceForm({ name: '', labels: '' });
      loadData();
    } catch (error) {
      console.error("Failed to create namespace:", error);
      toast.error("Failed to create namespace");
    }
  };

  const parseLabels = (labelsString) => {
    if (!labelsString.trim()) return {};
    
    const labels = {};
    labelsString.split(',').forEach(pair => {
      const [key, value] = pair.split('=').map(s => s.trim());
      if (key && value) {
        labels[key] = value;
      }
    });
    return labels;
  };

  const handleDeletePod = async (namespace, name) => {
    if (!window.confirm(`Are you sure you want to delete pod ${name}?`)) return;
    
    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      await api.deletePod(namespace, name);
      toast.success("Pod deleted successfully");
      loadData();
    } catch (error) {
      console.error("Failed to delete pod:", error);
      toast.error("Failed to delete pod");
    }
  };

  const handleDeleteDeployment = async (namespace, name) => {
    if (!window.confirm(`Are you sure you want to delete deployment ${name}?`)) return;
    
    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      await api.deleteDeployment(namespace, name);
      toast.success("Deployment deleted successfully");
      loadData();
    } catch (error) {
      console.error("Failed to delete deployment:", error);
      toast.error("Failed to delete deployment");
    }
  };

  const handleDeleteNamespace = async (name) => {
    if (!window.confirm(`Are you sure you want to delete namespace ${name}? This will delete all resources in the namespace.`)) return;
    
    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      await api.deleteNamespace(name);
      toast.success("Namespace deleted successfully");
      if (selectedNamespace === name) {
        setSelectedNamespace('default');
      }
      loadData();
    } catch (error) {
      console.error("Failed to delete namespace:", error);
      toast.error("Failed to delete namespace");
    }
  };

  const handleScaleDeployment = async (namespace, name, replicas) => {
    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      await api.scaleDeployment(namespace, name, replicas);
      toast.success(`Deployment scaled to ${replicas} replicas`);
      loadData();
    } catch (error) {
      console.error("Failed to scale deployment:", error);
      toast.error("Failed to scale deployment");
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      'Running': 'text-green-600',
      'Pending': 'text-yellow-600',
      'Failed': 'text-red-600',
      'Succeeded': 'text-blue-600',
      'Unknown': 'text-gray-600',
      'Active': 'text-green-600'
    };
    return colors[status] || colors.Unknown;
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

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Kubernetes Management</h1>
        <div className="flex space-x-2">
          <select
            value={selectedNamespace}
            onChange={(e) => setSelectedNamespace(e.target.value)}
            className="border rounded-md px-3 py-2"
          >
            <option value="all">All Namespaces</option>
            {namespaces.map((ns) => (
              <option key={ns.name} value={ns.name}>
                {ns.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex space-x-1 bg-gray-100 p-1 rounded-lg w-fit">
        {['pods', 'deployments', 'namespaces'].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 rounded-md capitalize transition-colors ${
              activeTab === tab
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Create Buttons */}
      <div className="flex space-x-2">
        {activeTab === 'pods' && (
          <Button
            onClick={() => setShowCreatePod(true)}
            className="bg-blue-600 hover:bg-blue-700"
          >
            Create Pod
          </Button>
        )}
        {activeTab === 'deployments' && (
          <Button
            onClick={() => setShowCreateDeployment(true)}
            className="bg-green-600 hover:bg-green-700"
          >
            Create Deployment
          </Button>
        )}
        {activeTab === 'namespaces' && (
          <Button
            onClick={() => setShowCreateNamespace(true)}
            className="bg-purple-600 hover:bg-purple-700"
          >
            Create Namespace
          </Button>
        )}
      </div>

      {/* Create Pod Form */}
      {showCreatePod && (
        <Card>
          <CardHeader>
            <CardTitle>Create New Pod</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreatePod} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  placeholder="Pod name"
                  value={podForm.name}
                  onChange={(e) => setPodForm({...podForm, name: e.target.value})}
                  required
                />
                <Input
                  placeholder="Container image"
                  value={podForm.image}
                  onChange={(e) => setPodForm({...podForm, image: e.target.value})}
                  required
                />
                <Input
                  placeholder="Labels (key=value,key2=value2)"
                  value={podForm.labels}
                  onChange={(e) => setPodForm({...podForm, labels: e.target.value})}
                />
                <Input
                  placeholder="CPU Limit (e.g., 500m)"
                  value={podForm.cpuLimit}
                  onChange={(e) => setPodForm({...podForm, cpuLimit: e.target.value})}
                />
                <Input
                  placeholder="Memory Limit (e.g., 512Mi)"
                  value={podForm.memoryLimit}
                  onChange={(e) => setPodForm({...podForm, memoryLimit: e.target.value})}
                />
                <Input
                  placeholder="CPU Request (e.g., 100m)"
                  value={podForm.cpuRequest}
                  onChange={(e) => setPodForm({...podForm, cpuRequest: e.target.value})}
                />
                <Input
                  placeholder="Memory Request (e.g., 256Mi)"
                  value={podForm.memoryRequest}
                  onChange={(e) => setPodForm({...podForm, memoryRequest: e.target.value})}
                />
              </div>
              <div className="flex space-x-2">
                <Button type="submit">Create Pod</Button>
                <Button type="button" onClick={() => setShowCreatePod(false)} className="bg-gray-500">Cancel</Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Create Deployment Form */}
      {showCreateDeployment && (
        <Card>
          <CardHeader>
            <CardTitle>Create New Deployment</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreateDeployment} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  placeholder="Deployment name"
                  value={deploymentForm.name}
                  onChange={(e) => setDeploymentForm({...deploymentForm, name: e.target.value})}
                  required
                />
                <Input
                  placeholder="Container image"
                  value={deploymentForm.image}
                  onChange={(e) => setDeploymentForm({...deploymentForm, image: e.target.value})}
                  required
                />
                <Input
                  type="number"
                  placeholder="Replicas"
                  value={deploymentForm.replicas}
                  onChange={(e) => setDeploymentForm({...deploymentForm, replicas: parseInt(e.target.value)})}
                  min="1"
                />
                <Input
                  placeholder="Labels (key=value,key2=value2)"
                  value={deploymentForm.labels}
                  onChange={(e) => setDeploymentForm({...deploymentForm, labels: e.target.value})}
                />
                <Input
                  placeholder="CPU Limit (e.g., 500m)"
                  value={deploymentForm.cpuLimit}
                  onChange={(e) => setDeploymentForm({...deploymentForm, cpuLimit: e.target.value})}
                />
                <Input
                  placeholder="Memory Limit (e.g., 512Mi)"
                  value={deploymentForm.memoryLimit}
                  onChange={(e) => setDeploymentForm({...deploymentForm, memoryLimit: e.target.value})}
                />
                <Input
                  placeholder="CPU Request (e.g., 100m)"
                  value={deploymentForm.cpuRequest}
                  onChange={(e) => setDeploymentForm({...deploymentForm, cpuRequest: e.target.value})}
                />
                <Input
                  placeholder="Memory Request (e.g., 256Mi)"
                  value={deploymentForm.memoryRequest}
                  onChange={(e) => setDeploymentForm({...deploymentForm, memoryRequest: e.target.value})}
                />
              </div>
              <div className="flex space-x-2">
                <Button type="submit">Create Deployment</Button>
                <Button type="button" onClick={() => setShowCreateDeployment(false)} className="bg-gray-500">Cancel</Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Create Namespace Form */}
      {showCreateNamespace && (
        <Card>
          <CardHeader>
            <CardTitle>Create New Namespace</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreateNamespace} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  placeholder="Namespace name"
                  value={namespaceForm.name}
                  onChange={(e) => setNamespaceForm({...namespaceForm, name: e.target.value})}
                  required
                />
                <Input
                  placeholder="Labels (key=value,key2=value2)"
                  value={namespaceForm.labels}
                  onChange={(e) => setNamespaceForm({...namespaceForm, labels: e.target.value})}
                />
              </div>
              <div className="flex space-x-2">
                <Button type="submit">Create Namespace</Button>
                <Button type="button" onClick={() => setShowCreateNamespace(false)} className="bg-gray-500">Cancel</Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {loading && (
        <div className="text-center py-8">
          <div className="text-lg">Loading {activeTab}...</div>
        </div>
      )}

      {/* Pods Tab */}
      {activeTab === 'pods' && !loading && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {pods.map((pod) => (
            <Card key={`${pod.namespace}-${pod.name}`}>
              <CardHeader>
                <div className="flex justify-between items-start">
                  <div>
                    <CardTitle className="text-lg">{pod.name}</CardTitle>
                    <p className="text-sm text-gray-600">Namespace: {pod.namespace}</p>
                  </div>
                  <div className={`flex items-center space-x-1 ${getStatusColor(pod.status)}`}>
                    <span className="font-medium">{pod.status}</span>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2 text-sm">
                  {pod.ip && <div><strong>IP:</strong> {pod.ip}</div>}
                  {pod.resources && Object.keys(pod.resources).length > 0 && (
                    <div>
                      <strong>Resources:</strong>
                      <div className="ml-4 grid grid-cols-2 gap-1">
                        {Object.entries(pod.resources).map(([key, value]) => (
                          <div key={key}>{key}: {value}</div>
                        ))}
                      </div>
                    </div>
                  )}
                  {pod.labels && Object.keys(pod.labels).length > 0 && (
                    <div>
                      <strong>Labels:</strong>
                      <div className="ml-4">
                        {Object.entries(pod.labels).map(([key, value]) => (
                          <span key={key} className="inline-block bg-gray-100 rounded px-2 py-1 text-xs mr-1 mt-1">
                            {key}: {value}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
                <div className="mt-4">
                  <Button
                    size="sm"
                    onClick={() => handleDeletePod(pod.namespace, pod.name)}
                    className="bg-red-500 hover:bg-red-600"
                  >
                    Delete
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Deployments Tab */}
      {activeTab === 'deployments' && !loading && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {deployments.map((deployment) => (
            <Card key={`${deployment.namespace}-${deployment.name}`}>
              <CardHeader>
                <div className="flex justify-between items-start">
                  <div>
                    <CardTitle className="text-lg">{deployment.name}</CardTitle>
                    <p className="text-sm text-gray-600">Namespace: {deployment.namespace}</p>
                  </div>
                  <div className="text-right">
                    <div className="text-sm">
                      {deployment.availableReplicas || 0}/{deployment.replicas || 0} replicas
                    </div>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2 text-sm">
                  <div><strong>Image:</strong> {deployment.image}</div>
                  {deployment.resources && Object.keys(deployment.resources).length > 0 && (
                    <div>
                      <strong>Resources:</strong>
                      <div className="ml-4 grid grid-cols-2 gap-1">
                        {Object.entries(deployment.resources).map(([key, value]) => (
                          <div key={key}>{key}: {value}</div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
                <div className="mt-4 flex space-x-2">
                  <Button
                    size="sm"
                    onClick={() => {
                      const newReplicas = prompt("Enter new replica count:", deployment.replicas);
                      if (newReplicas && !isNaN(newReplicas)) {
                        handleScaleDeployment(deployment.namespace, deployment.name, parseInt(newReplicas));
                      }
                    }}
                    className="bg-blue-500 hover:bg-blue-600"
                  >
                    Scale
                  </Button>
                  <Button
                    size="sm"
                    onClick={() => handleDeleteDeployment(deployment.namespace, deployment.name)}
                    className="bg-red-500 hover:bg-red-600"
                  >
                    Delete
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Namespaces Tab */}
      {activeTab === 'namespaces' && !loading && (
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">
          {namespaces.map((namespace) => (
            <Card key={namespace.name}>
              <CardHeader>
                <div className="flex justify-between items-start">
                  <CardTitle className="text-lg">{namespace.name}</CardTitle>
                  <div className={`${getStatusColor(namespace.status || 'Active')}`}>
                    {namespace.status || 'Active'}
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                {namespace.labels && Object.keys(namespace.labels).length > 0 && (
                  <div className="space-y-2">
                    <strong>Labels:</strong>
                    <div>
                      {Object.entries(namespace.labels).map(([key, value]) => (
                        <span key={key} className="inline-block bg-gray-100 rounded px-2 py-1 text-xs mr-1 mt-1">
                          {key}: {value}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
                {namespace.name !== 'default' && namespace.name !== 'kube-system' && (
                  <div className="mt-4">
                    <Button
                      size="sm"
                      onClick={() => handleDeleteNamespace(namespace.name)}
                      className="bg-red-500 hover:bg-red-600"
                    >
                      Delete
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Empty States */}
      {!loading && activeTab === 'pods' && pods.length === 0 && (
        <Card>
          <CardContent className="text-center py-8">
            <div className="text-gray-500">No pods found in the selected namespace.</div>
          </CardContent>
        </Card>
      )}

      {!loading && activeTab === 'deployments' && deployments.length === 0 && (
        <Card>
          <CardContent className="text-center py-8">
            <div className="text-gray-500">No deployments found in the selected namespace.</div>
          </CardContent>
        </Card>
      )}

      {!loading && activeTab === 'namespaces' && namespaces.length === 0 && (
        <Card>
          <CardContent className="text-center py-8">
            <div className="text-gray-500">No namespaces found.</div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
