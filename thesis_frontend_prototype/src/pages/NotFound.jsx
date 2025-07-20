// src/pages/NotFound.jsx
import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center h-screen">
      <h1 className="text-2xl font-bold">404 - Not Found</h1>
      <Link className="mt-4 text-blue-500" to="/">Return to Login</Link>
    </div>
  );
}
