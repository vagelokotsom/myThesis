import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { toast } from "react-hot-toast";

export default function Navbar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout, isTeacher, isStudent } = useAuth();

  const handleLogout = () => {
    logout();
    toast.success("Logged out successfully");
    navigate("/");
  };

  const getRoleDisplay = () => {
    if (isTeacher()) return "Teacher";
    if (isStudent()) return "Student";
    return "User";
  };

  return (
    <nav className="bg-gray-800 text-white py-4 px-6">
      <ul className="flex items-center">
        <li className="mr-6">
          <Link
            to="/dashboard"
            className={`hover:text-gray-300 ${location.pathname === "/dashboard" ? "font-bold underline" : ""
              }`}
          >
            Dashboard
          </Link>
        </li>
        
        {/* Teacher-specific navigation */}
        {isTeacher() && (
          <>
            <li className="mr-6">
              <Link
                to="/courses"
                className={`hover:text-gray-300 ${location.pathname === "/courses" ? "font-bold underline" : ""
                  }`}
              >
                Courses
              </Link>
            </li>
            
            <li className="mr-6">
              <Link
                to="/templates"
                className={`hover:text-gray-300 ${location.pathname === "/templates" ? "font-bold underline" : ""
                  }`}
              >
                Container Templates
              </Link>
            </li>

            <li className="mr-6">
              <Link
                to="/student-containers"
                className={`hover:text-gray-300 ${location.pathname === "/student-containers" ? "font-bold underline" : ""
                  }`}
              >
                Student Containers
              </Link>
            </li>
            
            <li className="mr-6">
              <Link
                to="/kubernetes"
                className={`hover:text-gray-300 ${location.pathname === "/kubernetes" ? "font-bold underline" : ""
                  }`}
              >
                Kubernetes
              </Link>
            </li>
          </>
        )}

        {/* Container management for both teachers and students */}
        <li className="mr-6">
          <Link
            to="/pods"
            className={`hover:text-gray-300 ${location.pathname === "/pods" ? "font-bold underline" : ""
              }`}
          >
            {isTeacher() ? "Pod Management" : "My Containers"}
          </Link>
        </li>

        {/* Student-specific navigation */}
        {isStudent() && (
          <li className="mr-6">
            <Link
              to="/containers"
              className={`hover:text-gray-300 ${location.pathname === "/containers" ? "font-bold underline" : ""
                }`}
            >
              Container Dashboard
            </Link>
          </li>
        )}

        {/* User info and logout aligned to top-right corner */}
        <li className="ml-auto flex items-center space-x-4">
          <span className="text-sm">
            {user?.username} ({getRoleDisplay()})
          </span>
          <button
            onClick={handleLogout}
            className="bg-red-500 hover:bg-red-600 px-3 py-1 rounded-md transition-colors"
          >
            Logout
          </button>
        </li>
      </ul>
    </nav>
  );
}
