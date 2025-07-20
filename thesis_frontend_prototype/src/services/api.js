const API_BASE_URL = 'http://localhost:8080/api';

class ApiService {
  constructor() {
    this.refreshToken();
  }

  refreshToken() {
    // Get token from user object in localStorage, matching AuthContext pattern
    const savedUser = localStorage.getItem('user');
    console.log('refreshToken: savedUser from localStorage:', savedUser);
    
    if (savedUser) {
      try {
        const parsedUser = JSON.parse(savedUser);
        console.log('refreshToken: parsedUser:', parsedUser);
        console.log('refreshToken: parsedUser.token:', parsedUser.token);
        this.token = parsedUser.token;
      } catch (error) {
        console.error('refreshToken: Error parsing savedUser:', error);
        this.token = null;
      }
    } else {
      console.log('refreshToken: No savedUser found in localStorage');
      this.token = null;
    }
    
    console.log('refreshToken: Final token value:', this.token);
  }

  setToken(token) {
    console.log('API.setToken called with:', token ? token.substring(0, 20) + '...' : 'null');
    this.token = token;
    // Update the token in the user object in localStorage
    const savedUser = localStorage.getItem('user');
    if (savedUser && token) {
      try {
        const userData = JSON.parse(savedUser);
        userData.token = token;
        localStorage.setItem('user', JSON.stringify(userData));
      } catch (error) {
        console.error('Error updating token in user data:', error);
      }
    }
    
    // Also maintain backwards compatibility with separate authToken storage
    if (token) {
      localStorage.setItem('authToken', token);
    } else {
      localStorage.removeItem('authToken');
      // Clear user data if token is being removed
      localStorage.removeItem('user');
    }
  }

  getAuthHeaders() {
    return {
      'Content-Type': 'application/json',
      ...(this.token && { 'Authorization': `Bearer ${this.token}` })
    };
  }

  async request(endpoint, options = {}) {
    // Only refresh token from localStorage if we don't already have one
    if (!this.token) {
      this.refreshToken();
    }
    
    console.log('=== API Request Debug ===');
    console.log('Endpoint:', endpoint);
    console.log('Current token:', this.token);
    console.log('Headers:', this.getAuthHeaders());
    
    const url = `${API_BASE_URL}${endpoint}`;
    
    const config = {
      headers: this.getAuthHeaders(),
      ...options
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        if (response.status === 401) {
          this.setToken(null);
          // Don't automatically redirect, let components handle the auth state change
          throw new Error('Authentication failed');
        }
        
        const errorText = await response.text();
        throw new Error(errorText || `HTTP error! status: ${response.status}`);
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }
      
      return await response.text();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  // Helper method to make authenticated requests with a specific token
  async requestWithToken(endpoint, token, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const headers = {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    };
    
    const config = {
      headers,
      ...options
    };

    console.log('=== API Request With Token Debug ===');
    console.log('Endpoint:', endpoint);
    console.log('Token:', token);
    console.log('Headers:', headers);

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        if (response.status === 401) {
          throw new Error('Authentication failed');
        }
        
        const errorText = await response.text();
        throw new Error(errorText || `HTTP error! status: ${response.status}`);
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }
      
      return await response.text();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  // Authentication endpoints
  async login(username, password) {
    const response = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    
    if (response.token) {
      this.setToken(response.token);
    }
    
    return response;
  }

  async logout() {
    this.setToken(null);
  }

  // Container Templates endpoints
  async getContainerTemplates() {
    return await this.request('/container-templates');
  }

  // Image Templates endpoints  
  async getImageTemplates() {
    return await this.request('/images');
  }

  async createContainerTemplate(template) {
    return await this.request('/container-templates', {
      method: 'POST',
      body: JSON.stringify(template)
    });
  }

  async updateContainerTemplate(id, template) {
    return await this.request(`/container-templates/${id}`, {
      method: 'PUT',
      body: JSON.stringify(template)
    });
  }

  async deleteContainerTemplate(id) {
    return await this.request(`/container-templates/${id}`, {
      method: 'DELETE'
    });
  }

  async getMyTemplates() {
    return await this.request('/container-templates/my-templates');
  }

  async getSshEnabledTemplates() {
    return await this.request('/container-templates/ssh-enabled');
  }

  async getTemplatesByCategory(category) {
    return await this.request(`/container-templates/category/${category}`);
  }

  async searchTemplates(name) {
    return await this.request(`/container-templates/search?name=${encodeURIComponent(name)}`);
  }

  async getExampleTemplates() {
    return await this.request('/container-templates/examples');
  }

  // Container Instance endpoints
  async getMyContainers() {
    return await this.request('/containers/my-containers');
  }

  async getAllContainers() {
    return await this.request('/containers');
  }

  async createContainer(templateId) {
    return await this.request('/containers', {
      method: 'POST',
      body: JSON.stringify({ templateId })
    });
  }

  async createContainerForStudent(imageId, studentId) {
    console.log('=== API createContainerForStudent Debug ===');
    console.log('imageId:', imageId, 'type:', typeof imageId);
    console.log('studentId:', studentId, 'type:', typeof studentId);
    
    const body = { imageId, studentId };
    console.log('Request body:', body);
    
    return await this.request('/containers/create-for-student', {
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  async startContainer(id) {
    return await this.request(`/containers/${id}/start`, {
      method: 'POST'
    });
  }

  async stopContainer(id) {
    return await this.request(`/containers/${id}/stop`, {
      method: 'POST'
    });
  }

  async deleteContainer(id) {
    return await this.request(`/containers/${id}`, {
      method: 'DELETE'
    });
  }

  async getContainerLogs(id) {
    return await this.request(`/containers/${id}/logs`);
  }

  async getContainerStats() {
    return await this.request('/containers/stats');
  }
  
  async getContainerSshInfo(id) {
    return await this.request(`/containers/${id}/ssh-info`);
  }

  // Container status refresh methods
  async refreshContainerStatus(id) {
    return await this.request(`/containers/${id}/refresh-status`, {
      method: 'POST'
    });
  }

  async refreshAllContainerStatuses() {
    return await this.request('/containers/refresh-all-statuses', {
      method: 'POST'
    });
  }

  // SSH Connection endpoints
  async createSshConnection(containerInstanceId) {
    return await this.request(`/ssh/connect/${containerInstanceId}`, {
      method: 'POST'
    });
  }

  async getSshConnections() {
    return await this.request('/ssh/connections');
  }

  async getSshConnection(id) {
    return await this.request(`/ssh/connections/${id}`);
  }

  async revokeSshConnection(id) {
    return await this.request(`/ssh/connections/${id}`, {
      method: 'DELETE'
    });
  }

  // Kubernetes Pod Management endpoints  
  async getAllPods(allNamespaces = false) {
    return await this.request(`/kubernetes/pods?allNamespaces=${allNamespaces}`);
  }

  async getPodsInNamespace(namespace) {
    return await this.request(`/kubernetes/namespaces/${namespace}/pods`);
  }

  async getPod(namespace, name) {
    return await this.request(`/kubernetes/namespaces/${namespace}/pods/${name}`);
  }

  async createPod(namespace, podData) {
    const params = new URLSearchParams(podData);
    return await this.request(`/kubernetes/namespaces/${namespace}/pods?${params}`, {
      method: 'POST'
    });
  }

  async deletePod(namespace, name) {
    return await this.request(`/kubernetes/namespaces/${namespace}/pods/${name}`, {
      method: 'DELETE'
    });
  }

  async updatePodResources(namespace, name, resources) {
    return await this.request(`/kubernetes/namespaces/${namespace}/pods/${name}/resources`, {
      method: 'PUT',
      body: JSON.stringify(resources)
    });
  }

  // Kubernetes Deployment Management endpoints
  async getAllDeployments(allNamespaces = false) {
    return await this.request(`/kubernetes/deployments?allNamespaces=${allNamespaces}`);
  }

  async getDeploymentsInNamespace(namespace) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments`);
  }

  async getDeployment(namespace, name) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments/${name}`);
  }

  async createDeployment(namespace, deploymentData) {
    const params = new URLSearchParams(deploymentData);
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments?${params}`, {
      method: 'POST'
    });
  }

  async deleteDeployment(namespace, name) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments/${name}`, {
      method: 'DELETE'
    });
  }

  async scaleDeployment(namespace, name, replicas) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments/${name}/scale?replicas=${replicas}`, {
      method: 'PATCH'
    });
  }

  async updateDeploymentImage(namespace, name, image) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments/${name}/image?image=${encodeURIComponent(image)}`, {
      method: 'PATCH'
    });
  }

  // Kubernetes Namespace Management endpoints
  async getAllNamespaces() {
    return await this.request('/kubernetes/namespaces');
  }

  async getNamespace(name) {
    return await this.request(`/kubernetes/namespaces/${name}`);
  }

  async createNamespace(name, labels = {}) {
    const params = new URLSearchParams({ name, ...labels });
    return await this.request(`/kubernetes/namespaces?${params}`, {
      method: 'POST'
    });
  }

  async deleteNamespace(name) {
    return await this.request(`/kubernetes/namespaces/${name}`, {
      method: 'DELETE'
    });
  }

  // Enhanced Container Instance Management
  async createContainerFromTemplate(templateId, studentId = null) {
    const body = { templateId };
    if (studentId) body.studentId = studentId;
    
    return await this.request('/containers/create-for-student', {
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  async getContainerStatus(id) {
    return await this.request(`/containers/${id}/status`);
  }

  async restartContainer(id) {
    return await this.request(`/containers/${id}/restart`, {
      method: 'POST'
    });
  }

  // User Management (for teachers managing students)
  async getAllStudents() {
    return await this.request('/users/students');
  }

  async getAllUsers() {
    return await this.request('/users');
  }

  async getUserContainers(userId) {
    return await this.request(`/users/${userId}/containers`);
  }

  // Generic HTTP methods for convenience
  async get(endpoint) {
    return await this.request(endpoint, { method: 'GET' });
  }

  async post(endpoint, data) {
    return await this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  async put(endpoint, data) {
    return await this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  async delete(endpoint) {
    return await this.request(endpoint, { method: 'DELETE' });
  }

  // Generic HTTP methods with token parameter
  async getWithToken(endpoint, token) {
    return await this.requestWithToken(endpoint, token, { method: 'GET' });
  }

  async postWithToken(endpoint, data, token) {
    return await this.requestWithToken(endpoint, token, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }
}

export default new ApiService();
