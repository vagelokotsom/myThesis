import React, { useState, useEffect, useCallback, useMemo } from "react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { toast } from "react-hot-toast";
import api from "../services/api";
import { useAuth } from "../contexts/AuthContext";

export default function StudentContainers() {
  const { user, isTeacher } = useAuth();
  const [pods, setPods] = useState([]);
  const [refreshCountdown, setRefreshCountdown] = useState(60);
  const [showHelpModal, setShowHelpModal] = useState(false);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [logs, setLogs] = useState({});
  const [showLogsFor, setShowLogsFor] = useState(null);
  const [sshConnections, setSshConnections] = useState([]);
  const [showSshModal, setShowSshModal] = useState(false);
  const [selectedPod, setSelectedPod] = useState(null);
  const [sshInfo, setSshInfo] = useState(null);

  const loadData = useCallback(async () => {
    const teacherView = isTeacher();

    try {
      setLoading(true);

      if (user?.token) {
        api.setToken(user.token);
      }

      const [podsResponse, templatesResponse] = await Promise.all([
        teacherView ? api.getAllContainers() : api.getMyContainers(),
        teacherView ? api.getImageTemplates() : api.getSshEnabledTemplates()
      ]);

      setTemplates(templatesResponse || []);

      let resolvedPods = podsResponse || [];

      // Ensure we sync statuses with Kubernetes before rendering
      if (resolvedPods.length > 0) {
        try {
          if (user?.token) {
            api.setToken(user.token);
          }

          if (teacherView) {
            await api.refreshAllContainerStatuses();
            if (user?.token) {
              api.setToken(user.token);
            }
            resolvedPods = await api.getAllContainers();
          } else {
            await Promise.all(
              resolvedPods.map((pod) =>
                api.refreshContainerStatus(pod.id).catch(() => null)
              )
            );
            if (user?.token) {
              api.setToken(user.token);
            }
            resolvedPods = await api.getMyContainers();
          }
        } catch (syncError) {
          console.warn('Failed to refresh container statuses from Kubernetes', syncError);
        }
      }

      setPods(resolvedPods);

      if (!teacherView) {
        try {
          const sshResponse = await api.getSshConnections();
          setSshConnections(sshResponse || []);
        } catch (error) {
          // Don't fail the whole load if SSH connections fail
        }
      }
    } catch (error) {
      toast.error("Failed to load container data");
    } finally {
      setRefreshCountdown(60);
      setLoading(false);
    }
  }, [user, isTeacher]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    const interval = setInterval(() => {
      setRefreshCountdown((prev) => {
        if (prev <= 1) {
          loadData();
          return 60;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [loadData]);

  const selectedTemplateDetails = useMemo(() => {
    if (!selectedTemplate) {
      return null;
    }
    return templates.find((template) => String(template.id) === String(selectedTemplate)) || null;
  }, [selectedTemplate, templates]);

  const handleCreatePod = async () => {
    if (!selectedTemplate) {
      toast.error("Please select a template");
      return;
    }

    try {
      await api.createContainerFromTemplate(Number(selectedTemplate));
      toast.success("Pod created successfully");
      setShowCreateModal(false);
      setSelectedTemplate("");
      loadData();
    } catch (error) {
      console.error("Failed to create container:", error);
      toast.error("Failed to create container");
    }
  };

  const handleStartPod = async (podId) => {
    try {
      await api.startContainer(podId);
      await api.refreshContainerStatus(podId).catch(() => null);
      toast.success("Pod started");
      loadData();
    } catch (error) {
      console.error("Failed to start container:", error);
      toast.error("Failed to start container");
    }
  };

  const handleStopPod = async (podId) => {
    try {
      await api.stopContainer(podId);
      await api.refreshContainerStatus(podId).catch(() => null);
      toast.success("Pod stopped");
      loadData();
    } catch (error) {
      console.error("Failed to stop container:", error);
      toast.error("Failed to stop container");
    }
  };

  const handleDeletePod = async (podId) => {
    if (!window.confirm("Are you sure you want to delete this pod? This action cannot be undone.")) {
      return;
    }

    try {
      await api.deleteContainer(podId);
      toast.success("Pod deleted");
      loadData();
    } catch (error) {
      console.error("Failed to delete container:", error);
      toast.error("Failed to delete container");
    }
  };

  const handleGetLogs = async (podId) => {
    try {
      const podLogs = await api.getContainerLogs(podId);
      setLogs(prev => ({
        ...prev,
        [podId]: podLogs
      }));
      setShowLogsFor(podId);
    } catch (error) {
      console.error("Failed to get logs:", error);
      toast.error("Failed to retrieve container logs");
    }
  };

  // Removed unused handleCreateSshConnection

  const handleRevokeSshConnection = async (connectionId) => {
    try {
      await api.revokeSshConnection(connectionId);
      toast.success("SSH connection revoked");
      setSshConnections(prev => prev.filter(conn => conn.id !== connectionId));
    } catch (error) {
      console.error("Failed to revoke SSH connection:", error);
      toast.error("Failed to revoke SSH connection");
    }
  };

  const handleShowSshInfo = async (pod) => {
    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }

  const info = await api.getContainerSshInfo(pod.id);
  setSelectedPod(pod);
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

  const getStatusColor = (status) => {
    const colors = {
      'Running': 'text-green-600',
      'Pending': 'text-yellow-600',
      'Stopped': 'text-red-600',
      'Creating': 'text-blue-600',
      'Error': 'text-red-800',
      'Unknown': 'text-gray-600'
    };
    return colors[status] || colors.Unknown;
  };

  const getStatusIcon = (status) => {
    const icons = {
      'Running': '🟢',
      'Pending': '🟡',
      'Stopped': '🔴',
      'Creating': '🔵',
      'Error': '❌',
      'Unknown': '❓'
    };
    return icons[status] || icons.Unknown;
  };

  if (loading) {
    return (
      <div className="p-6 flex justify-center">
        <div className="text-lg">Loading pods...</div>
      </div>
    );
  }

    return (
      <div className="p-6 space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold">My Pods</h1>
            <div className="text-xs text-gray-500 mt-1">Status auto-refresh in <span className="font-semibold">{refreshCountdown}s</span></div>
          </div>
          <div className="flex gap-2">
            {!isTeacher() && (
              <Button
                onClick={() => setShowCreateModal(true)}
                className="bg-blue-600 hover:bg-blue-700"
              >
                Create New Pod
              </Button>
            )}
            <Button
              variant="outline"
              onClick={() => setShowHelpModal(true)}
              className="border-gray-400 text-gray-700"
            >
              Help / Onboarding
            </Button>
          </div>
        </div>
      {/* Help/Onboarding Modal */}
      {showHelpModal && (
        <Card className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>Welcome to Your Container Workspace</CardTitle>
                <Button
                  size="sm"
                  onClick={() => setShowHelpModal(false)}
                  className="bg-gray-500 hover:bg-gray-600"
                >
                  Close
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-4 text-sm">
                <div>
                  <strong>What is a Container?</strong>
                  <p className="mt-1 text-gray-700">A container is your personal workspace in the cloud. You can run code, access files, and connect via SSH.</p>
                </div>
                <div>
                  <strong>How to Use:</strong>
                  <ul className="list-disc ml-5 mt-1 text-gray-700 space-y-1">
                    <li><strong>Create:</strong> Click <span className="font-semibold">Create New Container</span> and select a template.</li>
                    <li><strong>Start/Stop:</strong> Use the Start/Stop buttons to control your container.</li>
                    <li><strong>Delete:</strong> Remove containers you no longer need.</li>
                    <li><strong>Logs:</strong> View logs for troubleshooting or monitoring.</li>
                    <li><strong>SSH:</strong> Connect securely to your container using the SSH Info button.</li>
                  </ul>
                </div>
                <div>
                  <strong>SSH Access & Troubleshooting:</strong>
                  <ul className="list-disc ml-5 mt-1 text-gray-700 space-y-1">
                    <li>Use the provided SSH command and password to connect.</li>
                    <li>If direct connection fails (especially on macOS), use the port-forwarding instructions.</li>
                    <li>Keep the port-forward terminal open while using SSH.</li>
                    <li>If you see errors, check troubleshooting tips in the SSH Info modal.</li>
                  </ul>
                </div>
                <div>
                  <strong>Need Help?</strong>
                  <p className="mt-1 text-gray-700">Contact your instructor or system administrator for support, or check the help section in each modal.</p>
                </div>
              </div>
            </CardContent>
          </div>
        </Card>
      )}

      {/* Create Pod Modal */}
      {showCreateModal && (
        <Card>
          <CardHeader>
            <CardTitle>Create New Pod</CardTitle>
          </CardHeader>
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
                {/* Show template description and recommended use case */}
                {selectedTemplate && (
                  <div className="mt-3 p-3 bg-gray-50 border border-gray-200 rounded">
                    <div className="font-medium text-gray-800 mb-1">
                    {selectedTemplateDetails?.name}
                    </div>
                    <div className="text-sm text-gray-700 mb-1">
                      <strong>Description:</strong> {selectedTemplateDetails?.description || "No description provided."}
                    </div>
                    {/* Example: recommended use case, if available */}
                    {selectedTemplateDetails?.recommendedUse && (
                      <div className="text-xs text-blue-700">
                        <strong>Recommended use:</strong> {selectedTemplateDetails.recommendedUse}
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="flex space-x-2">
                <Button onClick={handleCreatePod} className="bg-green-600 hover:bg-green-700">
                  Create Pod
                </Button>
                <Button
                  onClick={() => setShowCreateModal(false)}
                  className="bg-gray-500 hover:bg-gray-600"
                >
                  Cancel
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Pods List */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {pods.map((pod) => (
          <Card key={pod.id}>
            <CardHeader>
              <div className="flex justify-between items-start">
                <div>
                  <CardTitle className="text-lg">{pod.name}</CardTitle>
                  {isTeacher() && pod.owner && (
                    <p className="text-sm text-gray-600">Owner: {pod.owner.username}</p>
                  )}
                </div>
                <div className={`flex items-center space-x-1 ${getStatusColor(pod.status)}`}>
                  <span>{getStatusIcon(pod.status)}</span>
                  <span className="font-medium">{pod.status}</span>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="text-sm">
                  <strong>Pod Name:</strong> {pod.kubernetesPodName}
                </div>
                {pod.resourceLimits && (
                  <div className="text-sm mt-1">
                    <strong>Resources:</strong>
                    <span className="ml-2">CPU: {pod.resourceLimits["cpu-limit"] || pod.resourceLimits["cpu-request"] || "N/A"}</span>
                    <span className="ml-2">Memory: {pod.resourceLimits["memory-limit"] || pod.resourceLimits["memory-request"] || "N/A"}</span>
                  </div>
                )}

                {/* Pod Actions */}
                <div className="flex flex-wrap gap-2">
                  {pod.status === 'Stopped' && (
                    <Button
                      size="sm"
                      onClick={() => handleStartPod(pod.id)}
                      className="bg-green-500 hover:bg-green-600"
                    >
                      Start
                    </Button>
                  )}

                  {pod.status === 'Running' && (
                    <Button
                      size="sm"
                      onClick={() => handleStopPod(pod.id)}
                      className="bg-yellow-500 hover:bg-yellow-600"
                    >
                      Stop
                    </Button>
                  )}

                  <Button
                    size="sm"
                    onClick={() => handleGetLogs(pod.id)}
                    className="bg-blue-500 hover:bg-blue-600"
                  >
                    View Logs
                  </Button>

                  {!isTeacher() && pod.status === 'Running' && (
                    <Button
                      size="sm"
                      onClick={() => handleShowSshInfo(pod)}
                      className="bg-green-500 hover:bg-green-600"
                    >
                      SSH Info
                    </Button>
                  )}

                  <Button
                    size="sm"
                    onClick={() => handleDeletePod(pod.id)}
                    className="bg-red-500 hover:bg-red-600"
                  >
                    Delete
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {pods.length === 0 && (
        <Card>
          <CardContent className="text-center py-8">
            <div className="text-gray-500">
              {isTeacher()
                ? "No student pods found."
                : "No pods found. Create your first pod to get started."
              }
            </div>
          </CardContent>
        </Card>
      )}

      {/* SSH Connections Section for Students */}
      {!isTeacher() && sshConnections.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Active SSH Connections</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {sshConnections.map((connection) => (
                <div key={connection.id} className="flex justify-between items-center border-b pb-2">
                  <div>
                    <div className="font-medium">{connection.podName}</div>
                    <div className="text-sm text-gray-600">
                      Host: {connection.sshHost}:{connection.sshPort}
                    </div>
                    <div className="text-sm text-gray-600">
                      Username: {connection.username}
                    </div>
                  </div>
                  <Button
                    size="sm"
                    onClick={() => handleRevokeSshConnection(connection.id)}
                    className="bg-red-500 hover:bg-red-600"
                  >
                    Revoke
                  </Button>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* SSH Quick Guide for Students */}
      {!isTeacher() && (
        <Card>
          <CardHeader>
            <CardTitle>SSH Quick Guide</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2 text-sm text-gray-700">
              <p>1) Create a pod from a template, then wait for status to become Running.</p>
              <p>2) Click <strong>SSH Info</strong> on a pod card to see the exact host and port.</p>
              <p>3) For direct access: <span className="font-mono">ssh -p &lt;port&gt; root@&lt;host&gt;</span>.</p>
              <p>4) If direct access fails on macOS, use the port-forward command shown in SSH Info.</p>
              <p className="text-gray-500">Tip: SSH details are per pod, so always use the SSH Info button for the correct values.</p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Logs Modal */}
      {showLogsFor && (
        <Card className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg w-full max-w-4xl max-h-[80vh] overflow-hidden">
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>Pod Logs</CardTitle>
                <Button
                  size="sm"
                  onClick={() => setShowLogsFor(null)}
                  className="bg-gray-500 hover:bg-gray-600"
                >
                  Close
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <pre className="bg-gray-900 text-green-400 p-4 rounded overflow-auto max-h-96 text-sm">
                {logs[showLogsFor] || "No logs available"}
              </pre>
            </CardContent>
          </div>
        </Card>
      )}

      {/* SSH Info Modal */}
      {showSshModal && selectedPod && sshInfo && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">SSH Connection Instructions</h2>

            <div className="space-y-4">
              <div className="bg-gray-50 p-4 rounded-md">
                <h3 className="font-medium mb-2">Pod: {selectedPod.name}</h3>
                <p className="text-sm text-gray-600 mb-2">Image: {sshInfo.dockerImage}</p>
                <p className="text-xs text-gray-500 mb-2">SSH is provided by the main container for SSH-ready images, or by a sidecar for standard images.</p>

                {sshInfo.ready ? (
                  <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div><strong>Host:</strong> {sshInfo.host}</div>
                      <div><strong>Port:</strong> {sshInfo.port}</div>
                      <div><strong>Username:</strong> {sshInfo.username}</div>
                    </div>

                    <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded">
                      <p className="text-sm font-medium text-blue-800 mb-2">
                        <strong>Method 1: Direct Connection</strong>
                        <span className="text-xs text-blue-600 ml-2">(Port {sshInfo.port} - assigned by Kubernetes)</span>
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
                      <p className="text-xs text-blue-700 mt-2">
                        Use your SSH key: ssh -i ~/.ssh/id_ed25519 -p {sshInfo.port} {sshInfo.username}@{sshInfo.host}
                      </p>
                      <p className="text-xs text-blue-700 mt-2">
                        ⚠️ May not work on macOS due to Docker/Minikube networking limitations
                      </p>
                    </div>

                    {sshInfo.portForwardCommand && (
                      <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded">
                        <p className="text-sm font-medium text-green-800 mb-3">
                          <strong>Method 2: Port Forward (Recommended)</strong>
                          <span className="text-xs text-green-600 ml-2">(Uses local port 8023 for convenience)</span>
                        </p>

                        {/* Port explanation */}
                        {sshInfo.portExplanation && (
                          <div className="mb-4 p-2 bg-gray-50 border border-gray-200 rounded">
                            <p className="text-xs font-medium text-gray-700 mb-2">🔍 Why Different Ports?</p>
                            <div className="space-y-1 text-xs text-gray-600">
                              <div>• <strong>Port {sshInfo.port}:</strong> {sshInfo.portExplanation.nodePort}</div>
                              <div>• <strong>Port 8023:</strong> {sshInfo.portExplanation.localPort}</div>
                              <div>• <strong>Reason:</strong> {sshInfo.portExplanation.why}</div>
                            </div>
                          </div>
                        )}

                        {/* Step-by-step instructions */}
                        <div className="space-y-3 mb-4">
                          <p className="text-xs font-medium text-green-700">📋 Step-by-Step Instructions:</p>
                          <div className="text-xs text-green-700"><strong>Step 1:</strong> Open a terminal/command prompt</div>
                          <div className="text-xs text-green-700"><strong>Step 2:</strong> Run the port-forward command below (keep this terminal open)</div>
                          <div className="text-xs text-green-700"><strong>Step 3:</strong> Open a new terminal window</div>
                          <div className="text-xs text-green-700"><strong>Step 4:</strong> Connect via SSH with your key (see command below)</div>
                        </div>

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
                                ssh -i ~/.ssh/id_ed25519 -p 8023 {sshInfo.username}@127.0.0.1
                              </code>
                              <Button
                                onClick={() => copyToClipboard(`ssh -i ~/.ssh/id_ed25519 -p 8023 ${sshInfo.username}@127.0.0.1`)}
                                className="px-2 py-1 text-xs bg-green-600 hover:bg-green-700 text-white"
                              >
                                Copy
                              </Button>
                            </div>
                          </div>
                        </div>

                        {/* Troubleshooting section */}
                        <div className="mt-4 p-2 bg-yellow-50 border border-yellow-200 rounded">
                          <p className="text-xs font-medium text-yellow-800 mb-2">🔧 Troubleshooting:</p>
                          <div className="space-y-1 text-xs text-yellow-700">
                            <div><strong>connection refused:</strong> Make sure the port-forward command is running in a separate terminal</div>
                            <div><strong>port in use:</strong> Try a different port (e.g., 8024:22) in both commands</div>
                            <div><strong>permission denied:</strong> Make sure you are using the correct SSH private key</div>
                            <div><strong>command not found:</strong> Make sure kubectl is installed and configured</div>
                          </div>
                        </div>
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
                  onClick={() => copyToClipboard(
                    `SSH Connection Instructions for Pod: ${selectedPod.name}\n\nStep-by-Step Instructions:\n1. Open a terminal/command prompt\n2. Run the port-forward command below (keep this terminal open)\n3. Open a new terminal window\n4. Run the SSH command below\n\n=== TERMINAL 1: Port Forwarding (Keep this running) ===\n${sshInfo.portForwardCommand}\n\n=== TERMINAL 2: SSH Connection ===\nssh -i ~/.ssh/id_ed25519 -p 8023 ${sshInfo.username}@127.0.0.1\n\n=== Troubleshooting ===\n- Make sure Terminal 1 is still running the port-forward command\n- If port 8023 is in use, try changing it to 8024:22 in both commands\n- Make sure kubectl is installed and configured`
                  )}
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
