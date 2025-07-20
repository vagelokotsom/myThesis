// src/components/ui/button.jsx
export function Button({ children, className = "", ...props }) {
    return (
      <button
        className={`bg-blue-500 text-white rounded-md px-4 py-2 hover:bg-blue-600 transition ${className}`}
        {...props}
      >
        {children}
      </button>
    );
  }
  