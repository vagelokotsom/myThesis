import React, { useState, useEffect, useCallback } from "react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardTitle } from "../components/ui/card";
// import { Input } from "../components/ui/input"; // Remove if not used
import { toast } from "react-hot-toast";
import api from "../services/api";
import { useAuth } from "../contexts/AuthContext";

export default function PodManagement() {
  const { isTeacher, isAdmin, isStudent, user } = useAuth();
  // const [pods, setPods] = useState([]); // Remove if not used
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [containers, setContainers] = useState([]);
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [showCreateContainer, setShowCreateContainer] = useState(false);
  const [showSshModal, setShowSshModal] = useState(false);
  const [selectedPod, setSelectedPod] = useState(null);
  const [sshInfo, setSshInfo] = useState(null);
  const [sshPublicKey, setSshPublicKey] = useState("");
  const [sshPublicKeySet, setSshPublicKeySet] = useState(false);
  const [savingSshKey, setSavingSshKey] = useState(false);
  const [downloadingKubeconfig, setDownloadingKubeconfig] = useState(false);
  const [kubeconfigScript, setKubeconfigScript] = useState("");
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    if (user?.token) {
      api.setToken(user.token);
    }
  }, [user?.token]);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      // Ensure API has the latest token before making requests
      if (user?.token) {
        api.setToken(user.token);
      }
      
      if (isTeacher() || isAdmin()) {
        // Teachers/Admins see all containers and can manage them
        const [containersResponse, templatesResponse] = await Promise.all([
          api.getAllContainers(),
          api.getContainerTemplates()
        ]);
        setContainers(containersResponse || []);
        setTemplates(templatesResponse || []);
      } else {
        // Students see only their containers and SSH-enabled templates
        const [containersResponse, templatesResponse, profileResponse] = await Promise.all([
          api.getMyContainers(),
          api.getSshEnabledTemplates(),
          api.getCurrentUser()
        ]);
        setContainers(containersResponse || []);
        setTemplates(templatesResponse || []);
        setSshPublicKeySet(Boolean(profileResponse?.sshPublicKeySet));
      }
    } catch (error) {
      console.error("Failed to load data:", error);
      toast.error("Failed to load container data");
    } finally {
      setLoading(false);
    }
  }, [isTeacher, user?.token]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // ...existing code...

  const handleCreateContainer = async () => {
    if (!selectedTemplate) {
      toast.error("Please select a template");
      return;
    }

    try {
      if (user?.token) {
        api.setToken(user.token);
      }
      await api.createContainerFromTemplate(Number(selectedTemplate));
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
      if (user?.token) api.setToken(user.token);
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
      if (user?.token) api.setToken(user.token);
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
      if (user?.token) api.setToken(user.token);
      await api.deleteContainer(containerId);
      toast.success("Container deleted");
      loadData();
    } catch (error) {
      console.error("Failed to delete container:", error);
      toast.error("Failed to delete container");
    }
  };

  const handleSaveSshKey = async () => {
    if (!sshPublicKey.trim()) {
      toast.error("Please paste your SSH public key");
      return;
    }

    try {
      setSavingSshKey(true);
      if (user?.token) api.setToken(user.token);
      await api.updateSshPublicKey(sshPublicKey.trim());
      toast.success("SSH public key saved");
      setSshPublicKeySet(true);
      setSshPublicKey("");
    } catch (error) {
      console.error("Failed to save SSH key:", error);
      toast.error("Failed to save SSH public key");
    } finally {
      setSavingSshKey(false);
    }
  };

  const handleDownloadKubeconfig = async () => {
    try {
      setDownloadingKubeconfig(true);
      if (user?.token) api.setToken(user.token);
      const configText = await api.downloadKubeconfig();
      const blob = new Blob([configText], { type: "text/yaml" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `kubeconfig-${user?.username || "student"}.yaml`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      toast.success("Kubeconfig downloaded");
    } catch (error) {
      console.error("Failed to download kubeconfig:", error);
      toast.error("Failed to download kubeconfig");
    } finally {
      setDownloadingKubeconfig(false);
    }
  };

  const handleGenerateKubeconfigScript = async () => {
    try {
      if (user?.token) api.setToken(user.token);
      const configText = await api.getKubeconfigText();

      const getValue = (key) => {
        const match = configText.match(new RegExp(`^\\s*${key}:\\s*(.+)$`, "m"));
        return match ? match[1].trim() : "";
      };

      const server = getValue("server");
      const caData = getValue("certificate-authority-data");
      const token = getValue("token");
      const namespace = getValue("namespace");
      const clusterName = getValue("name");

      const kubeUser = user?.username || "student";
      const contextName = `${kubeUser}-${namespace}`;

      const script = [
        `kubectl config set-cluster ${clusterName} --server=${server} --certificate-authority=<(echo ${caData} | base64 -d) --embed-certs=true`,
        `kubectl config set-credentials ${kubeUser} --token=${token}`,
        `kubectl config set-context ${contextName} --cluster=${clusterName} --user=${kubeUser} --namespace=${namespace}`,
        `kubectl config use-context ${contextName}`
      ].join("\\n");

      setKubeconfigScript(script);
    } catch (error) {
      console.error("Failed to generate kubeconfig script:", error);
      toast.error("Failed to generate kubectl setup commands");
    }
  };

  const handleShowSshInfo = async (container) => {
    try {
      if (user?.token) api.setToken(user.token);
      const info = await api.getContainerSshInfo(container.id);
      setSelectedPod(container);
      setSshInfo(info);
      setShowSshModal(true);
    } catch (error) {
      console.error("Failed to get SSH info:", error);
      toast.error("Failed to retrieve SSH information");
    }
  };

  const copyToClipboard = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      toast.success("Copied to clipboard!");
    } catch (error) {
      console.error("Failed to copy:", error);
      toast.error("Failed to copy to clipboard");
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

  const filteredContainers = containers.filter((container) => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return true;
    const owner = container.owner?.username || "";
    const pod = container.kubernetesPodName || "";
    const name = container.name || "";
    const status = container.status || "";
    return (
      name.toLowerCase().includes(query) ||
      owner.toLowerCase().includes(query) ||
      pod.toLowerCase().includes(query) ||
      status.toLowerCase().includes(query)
    );
  });

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
          {isTeacher() || isAdmin() ? "All Pods" : "My Pods"}
        </h1>
        {isStudent() && (
          <Button 
            onClick={() => setShowCreateContainer(true)}
            className="bg-blue-600 hover:bg-blue-700"
          >
            Create New Container
          </Button>
        )}
      </div>

      {/* Create Pod Modal for Students */}
      {showCreateContainer && (
        <Card>
          <CardTitle className="mb-4 p-4">Create New Pod</CardTitle>
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
                  Create Pod
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

      {/* Pods List */}
      <Card>
        <CardTitle className="mb-4 p-4">
          {isTeacher() || isAdmin() ? "All Student Pods" : "My Pods"}
        </CardTitle>
        <CardContent>
          {(isTeacher() || isAdmin()) && (
            <div className="mb-4">
              <input
                type="text"
                placeholder="Search by pod, owner, or status..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full border rounded-md px-3 py-2 text-sm"
              />
            </div>
          )}
          <div className="space-y-4">
            {filteredContainers.map((container) => (
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
                    {(isTeacher() || isAdmin()) && container.owner && (
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

                  {isStudent() && container.status === 'Running' && (
                    <Button
                      onClick={() => handleShowSshInfo(container)}
                      className="bg-green-500 hover:bg-green-600"
                      size="sm"
                    >
                      SSH Info
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
                {isTeacher() || isAdmin()
                  ? "No student pods found."
                  : "No pods found. Create your first pod to get started."
                }
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Additional Info for Teachers */}
      {(isTeacher() || isAdmin()) && (
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

      {/* Student Access Setup */}
      {isStudent() && (
        <Card>
          <CardTitle className="mb-4 p-4">Student Access Setup</CardTitle>
          <CardContent>
            <div className="space-y-4 text-sm text-gray-700">
              <div>
                <p className="font-medium text-gray-800 mb-1">SSH Key (Recommended)</p>
                <p className="text-gray-600">
                  Generate a key on your laptop and paste the public key below. This enables secure, passwordless SSH.
                </p>
                <div className="mt-2 bg-gray-50 border border-gray-200 rounded p-2 text-xs font-mono">
                  ssh-keygen -t ed25519 -C "{user?.email || "you@example.com"}"
                  <br />
                  cat ~/.ssh/id_ed25519.pub
                </div>
                <textarea
                  value={sshPublicKey}
                  onChange={(e) => setSshPublicKey(e.target.value)}
                  rows={3}
                  placeholder="Paste your SSH public key here"
                  className="mt-2 w-full border rounded-md px-3 py-2 text-sm"
                />
                <div className="flex items-center gap-2 mt-2">
                  <Button
                    onClick={handleSaveSshKey}
                    className="bg-green-600 hover:bg-green-700"
                    disabled={savingSshKey}
                  >
                    {savingSshKey ? "Saving..." : "Save SSH Key"}
                  </Button>
                  {sshPublicKeySet && (
                    <span className="text-green-700 text-xs">SSH key saved</span>
                  )}
                </div>
                <p className="text-xs text-gray-500 mt-2">
                  Note: apply the key to new pods by creating a new container or restarting an existing one.
                </p>
              </div>

              <div className="pt-2 border-t border-gray-100">
                <p className="font-medium text-gray-800 mb-1">Kubectl Profile</p>
                <p className="text-gray-600">
                  Download your namespace-scoped kubeconfig to access only your own resources.
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  Note: Minikube uses a local API server URL. If kubectl fails, run
                  <span className="font-mono">
                    {" kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'"}
                  </span>
                  and update the kubeconfig server value.
                </p>
                <p className="text-xs text-gray-500">
                  Tokens expire after 24 hours. Download a fresh kubeconfig if it stops working.
                </p>
                <div className="mt-2 bg-gray-50 border border-gray-200 rounded p-2 text-xs font-mono">
                  export KUBECONFIG=~/Downloads/kubeconfig-{user?.username || "student"}.yaml
                  <br />
                  kubectl get pods
                </div>
                <Button
                  onClick={handleDownloadKubeconfig}
                  className="bg-blue-600 hover:bg-blue-700 mt-2"
                  disabled={downloadingKubeconfig}
                >
                  {downloadingKubeconfig ? "Preparing..." : "Download Kubeconfig"}
                </Button>
                <Button
                  onClick={handleGenerateKubeconfigScript}
                  className="bg-gray-700 hover:bg-gray-800 mt-2 ml-2"
                >
                  Generate kubectl Setup Commands
                </Button>
                {kubeconfigScript && (
                  <div className="mt-3 bg-gray-50 border border-gray-200 rounded p-2 text-xs font-mono whitespace-pre-wrap">
                    {kubeconfigScript}
                  </div>
                )}
              </div>
            </div>
          </CardContent>
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

                        <div className="space-y-3 mb-4">
                          <p className="text-xs font-medium text-green-700">📋 Step-by-Step Instructions:</p>
                          <div className="text-xs text-green-700"><strong>Step 1:</strong> Open a terminal/command prompt</div>
                          <div className="text-xs text-green-700"><strong>Step 2:</strong> Run the port-forward command below (keep this terminal open)</div>
                          <div className="text-xs text-green-700"><strong>Step 3:</strong> Open a new terminal window</div>
                          <div className="text-xs text-green-700"><strong>Step 4:</strong> Connect via SSH with your key (see command below)</div>
                        </div>

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

      {/* SSH Quick Guide for Students */}
      {isStudent() && (
        <Card>
          <CardTitle className="mb-4 p-4">SSH Quick Guide</CardTitle>
          <CardContent>
            <div className="space-y-2 text-sm text-gray-700">
              <p>1) Create a pod from a template and wait for status to become Running.</p>
              <p>2) Open the pod details and click <strong>SSH Info</strong> to see the host and port.</p>
              <p>3) Connect with <span className="font-mono">ssh -p &lt;port&gt; root@&lt;host&gt;</span>.</p>
              <p>4) If direct access fails on macOS, use the port-forward command shown in SSH Info.</p>
              <p className="text-gray-500">Tip: SSH details are per pod, so always use SSH Info for the correct values.</p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
