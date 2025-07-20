// src/components/ui/input.jsx
export function Input({ type = "text", className = "", ...props }) {
    return (
      <input
        type={type}
        className={`border rounded-md px-3 py-2 w-full focus:outline-none focus:ring-2 focus:ring-blue-400 ${className}`}
        {...props}
      />
    );
  }
  