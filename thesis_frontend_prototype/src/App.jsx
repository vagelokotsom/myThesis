import { Toaster } from "react-hot-toast";
import { BrowserRouter, Routes, Route, Outlet } from "react-router-dom";

import LoginPage from "./pages/LoginPage";
import Dashboard from "./pages/Dashboard";
import PodManagement from "./pages/PodManagement";
import CourseManagement from "./pages/CourseManagement";
import ContainerTemplates from "./pages/ContainerTemplates";
import StudentContainers from "./pages/StudentContainers";
import StudentContainerManagement from "./pages/StudentContainerManagement";
import KubernetesManagement from "./pages/KubernetesManagement";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";
import { AuthProvider } from "./contexts/AuthContext";

function Layout() {
  return (
    <ProtectedRoute>
      <Navbar />
      <div className="container mx-auto mt-4">
        <Outlet />
      </div>
    </ProtectedRoute>
  );
}



function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<LoginPage />} />

          {/* Protected routes with Navbar */}
          <Route element={<Layout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/pods" element={<PodManagement />} />
            <Route path="/containers" element={<StudentContainers />} />
            <Route path="/courses" element={
              <ProtectedRoute requireRole="ROLE_TEACHER">
                <CourseManagement />
              </ProtectedRoute>
            } />
            <Route path="/templates" element={
              <ProtectedRoute requireRole="ROLE_TEACHER">
                <ContainerTemplates />
              </ProtectedRoute>
            } />
            <Route path="/student-containers" element={
              <ProtectedRoute requireRole="ROLE_TEACHER">
                <StudentContainerManagement />
              </ProtectedRoute>
            } />
            <Route path="/kubernetes" element={
              <ProtectedRoute requireRole="ROLE_TEACHER">
                <KubernetesManagement />
              </ProtectedRoute>
            } />
          </Route>
        </Routes>
      </BrowserRouter>
      <Toaster position="top-right" />
    </AuthProvider>
  );
}

export default App;
