// import React from 'react';
// import axios from 'axios'; // Import Axios library

// const MyContainersPage = () => {
//   const handleCreateContainer = () => {
//     // Make a POST request to the backend API to create a new container
//     axios.post('http://localhost:8180/api/containers/nginx')
//       .then(response => {
//         console.log('Response:', response.data); // Log the response from the server
//         // Handle any further logic based on the response if needed
//       })
//       .catch(error => {
//         console.error('Error creating container:', error); // Log any errors
//         // Handle error scenarios if needed
//       });
//   };

//   return (
//     <div>
//       <h1>My Containers</h1>
//       <button onClick={handleCreateContainer}>Create Container</button>
//     </div>
//   );
// }

// export default MyContainersPage;

import React, { useState } from 'react';
import axios from 'axios';

const MyContainersPage = () => {
  const [yamlData, setYamlData] = useState(''); // State to store YAML data
  const [selectedFile, setSelectedFile] = useState(null); // State to store selected file

  // Function to handle input text change
  const handleInputChange = (event) => {
    setYamlData(event.target.value);
  };

  // Function to handle file selection
  const handleFileSelect = (event) => {
    setSelectedFile(event.target.files[0]);
  };

  // Function to handle form submission
  const handleSubmit = () => {
    // Make a POST request to the backend API based on the selected option
    if (yamlData.trim() !== '') {
      // If YAML data is provided directly
      axios.post('http://localhost:8180/api/containers/createFromYamlString', { yamlData })
        .then(response => {
          console.log('Response:', response.data);
          // Handle success
        })
        .catch(error => {
          console.error('Error creating container:', error);
          // Handle error
        });
    } else if (selectedFile !== null) {
      // If a file is uploaded
      const formData = new FormData();
      formData.append('file', selectedFile);

      axios.post('http://localhost:8180/api/containers/upload', formData)
        .then(response => {
          console.log('Response:', response.data);
          // Handle success
        })
        .catch(error => {
          console.error('Error creating container:', error);
          // Handle error
        });
    } else {
      // Neither YAML data nor file is provided
      console.error('Please provide YAML data or upload a file.');
    }
  };

  return (
    <div>
      <h1>My Containers</h1>
      <textarea
        placeholder="Enter YAML data"
        value={yamlData}
        onChange={handleInputChange}
        rows={10}
        cols={50}
      />
      <input
        type="file"
        accept=".yaml, .yml"
        onChange={handleFileSelect}
      />
      <button onClick={handleSubmit}>Create Container</button>
    </div>
  );
};

export default MyContainersPage;

