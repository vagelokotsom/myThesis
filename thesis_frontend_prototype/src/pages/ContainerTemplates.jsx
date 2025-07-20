import { useState, useEffect } from "react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { toast } from "react-hot-toast";
import api from "../services/api";

export default function ContainerTemplates() {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState(null);
  
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    dockerImage: "",
    category: "",
    cpuLimit: "",
    memoryLimit: "",
    cpuRequest: "",
    memoryRequest: "",
    sshEnabled: false,
    persistentStorage: false,
    storageSize: "",
    shared: true,
    environmentVars: "",
    command: "",
    args: ""
  });

  useEffect(() => {
    loadTemplates();
  }, []);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      const response = await api.getMyTemplates();
      setTemplates(response);
    } catch (error) {
      console.error("Failed to load templates:", error);
      toast.error("Failed to load container templates");
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const resetForm = () => {
    setFormData({
      name: "",
      description: "",
      dockerImage: "",
      category: "",
      cpuLimit: "",
      memoryLimit: "",
      cpuRequest: "",
      memoryRequest: "",
      sshEnabled: false,
      persistentStorage: false,
      storageSize: "",
      shared: true,
      environmentVars: "",
      command: "",
      args: ""
    });
    setEditingTemplate(null);
    setShowCreateForm(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.name || !formData.dockerImage) {
      toast.error("Name and Docker image are required");
      return;
    }

    try {
      // Convert environment vars from string to array
      const envVars = formData.environmentVars 
        ? formData.environmentVars.split('\n').filter(line => line.trim())
        : [];
      
      // Convert args from string to array  
      const argsArray = formData.args
        ? formData.args.split('\n').filter(line => line.trim())
        : [];

      const templateData = {
        ...formData,
        environmentVars: envVars,
        args: argsArray
      };

      if (editingTemplate) {
        await api.updateContainerTemplate(editingTemplate.id, templateData);
        toast.success("Template updated successfully");
      } else {
        await api.createContainerTemplate(templateData);
        toast.success("Template created successfully");
      }
      
      resetForm();
      loadTemplates();
    } catch (error) {
      console.error("Failed to save template:", error);
      toast.error("Failed to save template");
    }
  };

  const handleEdit = (template) => {
    setFormData({
      name: template.name || "",
      description: template.description || "",
      dockerImage: template.dockerImage || "",
      category: template.category || "",
      cpuLimit: template.cpuLimit || "",
      memoryLimit: template.memoryLimit || "",
      cpuRequest: template.cpuRequest || "",
      memoryRequest: template.memoryRequest || "",
      sshEnabled: template.sshEnabled || false,
      persistentStorage: template.persistentStorage || false,
      storageSize: template.storageSize || "",
      shared: template.shared !== false,
      environmentVars: Array.isArray(template.environmentVars) ? template.environmentVars.join('\n') : "",
      command: template.command || "",
      args: Array.isArray(template.args) ? template.args.join('\n') : ""
    });
    setEditingTemplate(template);
    setShowCreateForm(true);
  };

  const handleDelete = async (templateId) => {
    if (!window.confirm("Are you sure you want to delete this template?")) {
      return;
    }

    try {
      await api.deleteContainerTemplate(templateId);
      toast.success("Template deleted successfully");
      loadTemplates();
    } catch (error) {
      console.error("Failed to delete template:", error);
      toast.error("Failed to delete template");
    }
  };

  const getCategoryColor = (category) => {
    const colors = {
      'web': 'bg-blue-100 text-blue-800',
      'database': 'bg-green-100 text-green-800',
      'development': 'bg-purple-100 text-purple-800',
      'data-science': 'bg-orange-100 text-orange-800',
      'security': 'bg-red-100 text-red-800',
      'default': 'bg-gray-100 text-gray-800'
    };
    return colors[category] || colors.default;
  };

  if (loading) {
    return (
      <div className="p-6 flex justify-center">
        <div className="text-lg">Loading container templates...</div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Container Templates</h1>
        <Button 
          onClick={() => setShowCreateForm(true)}
          className="bg-blue-600 hover:bg-blue-700"
        >
          Create New Template
        </Button>
      </div>

      {showCreateForm && (
        <Card>
          <CardHeader>
            <CardTitle>
              {editingTemplate ? "Edit Template" : "Create New Template"}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Template Name *</label>
                  <Input
                    value={formData.name}
                    onChange={(e) => handleInputChange("name", e.target.value)}
                    placeholder="e.g., Ubuntu Development Environment"
                    required
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium mb-1">Docker Image *</label>
                  <Input
                    value={formData.dockerImage}
                    onChange={(e) => handleInputChange("dockerImage", e.target.value)}
                    placeholder="e.g., ubuntu:22.04"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Category</label>
                  <select
                    value={formData.category}
                    onChange={(e) => handleInputChange("category", e.target.value)}
                    className="w-full border rounded-md px-3 py-2"
                  >
                    <option value="">Select Category</option>
                    <option value="web">Web Development</option>
                    <option value="database">Database</option>
                    <option value="development">Development</option>
                    <option value="data-science">Data Science</option>
                    <option value="security">Security</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Command</label>
                  <Input
                    value={formData.command}
                    onChange={(e) => handleInputChange("command", e.target.value)}
                    placeholder="e.g., /bin/bash"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => handleInputChange("description", e.target.value)}
                  className="w-full border rounded-md px-3 py-2 h-20"
                  placeholder="Describe what this template provides..."
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">CPU Limit</label>
                  <Input
                    value={formData.cpuLimit}
                    onChange={(e) => handleInputChange("cpuLimit", e.target.value)}
                    placeholder="e.g., 500m"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium mb-1">Memory Limit</label>
                  <Input
                    value={formData.memoryLimit}
                    onChange={(e) => handleInputChange("memoryLimit", e.target.value)}
                    placeholder="e.g., 512Mi"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">CPU Request</label>
                  <Input
                    value={formData.cpuRequest}
                    onChange={(e) => handleInputChange("cpuRequest", e.target.value)}
                    placeholder="e.g., 100m"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium mb-1">Memory Request</label>
                  <Input
                    value={formData.memoryRequest}
                    onChange={(e) => handleInputChange("memoryRequest", e.target.value)}
                    placeholder="e.g., 256Mi"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="sshEnabled"
                    checked={formData.sshEnabled}
                    onChange={(e) => handleInputChange("sshEnabled", e.target.checked)}
                  />
                  <label htmlFor="sshEnabled" className="text-sm font-medium">SSH Enabled</label>
                </div>

                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="persistentStorage"
                    checked={formData.persistentStorage}
                    onChange={(e) => handleInputChange("persistentStorage", e.target.checked)}
                  />
                  <label htmlFor="persistentStorage" className="text-sm font-medium">Persistent Storage</label>
                </div>

                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="shared"
                    checked={formData.shared}
                    onChange={(e) => handleInputChange("shared", e.target.checked)}
                  />
                  <label htmlFor="shared" className="text-sm font-medium">Shared Template</label>
                </div>
              </div>

              {formData.persistentStorage && (
                <div>
                  <label className="block text-sm font-medium mb-1">Storage Size</label>
                  <Input
                    value={formData.storageSize}
                    onChange={(e) => handleInputChange("storageSize", e.target.value)}
                    placeholder="e.g., 1Gi"
                  />
                </div>
              )}

              <div>
                <label className="block text-sm font-medium mb-1">Environment Variables (one per line)</label>
                <textarea
                  value={formData.environmentVars}
                  onChange={(e) => handleInputChange("environmentVars", e.target.value)}
                  className="w-full border rounded-md px-3 py-2 h-20"
                  placeholder="JAVA_HOME=/usr/lib/jvm/java-11&#10;NODE_ENV=development"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Arguments (one per line)</label>
                <textarea
                  value={formData.args}
                  onChange={(e) => handleInputChange("args", e.target.value)}
                  className="w-full border rounded-md px-3 py-2 h-20"
                  placeholder="-c&#10;tail -f /dev/null"
                />
              </div>

              <div className="flex space-x-2">
                <Button type="submit" className="bg-green-600 hover:bg-green-700">
                  {editingTemplate ? "Update Template" : "Create Template"}
                </Button>
                <Button 
                  type="button" 
                  onClick={resetForm}
                  className="bg-gray-500 hover:bg-gray-600"
                >
                  Cancel
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {templates.map((template) => (
          <Card key={template.id} className="h-fit">
            <CardHeader>
              <div className="flex justify-between items-start">
                <CardTitle className="text-lg">{template.name}</CardTitle>
                <div className="flex space-x-1">
                  <Button
                    size="sm"
                    onClick={() => handleEdit(template)}
                    className="bg-blue-500 hover:bg-blue-600 text-xs px-2 py-1"
                  >
                    Edit
                  </Button>
                  <Button
                    size="sm"
                    onClick={() => handleDelete(template.id)}
                    className="bg-red-500 hover:bg-red-600 text-xs px-2 py-1"
                  >
                    Delete
                  </Button>
                </div>
              </div>
              {template.category && (
                <span className={`inline-block px-2 py-1 rounded-full text-xs ${getCategoryColor(template.category)}`}>
                  {template.category}
                </span>
              )}
            </CardHeader>
            <CardContent>
              <div className="space-y-2 text-sm">
                <div><strong>Image:</strong> {template.dockerImage}</div>
                {template.description && (
                  <div><strong>Description:</strong> {template.description}</div>
                )}
                
                <div className="grid grid-cols-2 gap-2 text-xs">
                  {template.cpuLimit && <div><strong>CPU Limit:</strong> {template.cpuLimit}</div>}
                  {template.memoryLimit && <div><strong>Memory Limit:</strong> {template.memoryLimit}</div>}
                  {template.cpuRequest && <div><strong>CPU Request:</strong> {template.cpuRequest}</div>}
                  {template.memoryRequest && <div><strong>Memory Request:</strong> {template.memoryRequest}</div>}
                </div>

                <div className="flex space-x-4 text-xs">
                  <span className={template.sshEnabled ? "text-green-600" : "text-gray-400"}>
                    🔑 SSH {template.sshEnabled ? "Enabled" : "Disabled"}
                  </span>
                  <span className={template.persistentStorage ? "text-blue-600" : "text-gray-400"}>
                    💾 Storage {template.persistentStorage ? "Persistent" : "Ephemeral"}
                  </span>
                  <span className={template.shared ? "text-purple-600" : "text-gray-400"}>
                    🌐 {template.shared ? "Shared" : "Private"}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {templates.length === 0 && (
        <Card>
          <CardContent className="text-center py-8">
            <div className="text-gray-500">
              No container templates found. Create your first template to get started.
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
