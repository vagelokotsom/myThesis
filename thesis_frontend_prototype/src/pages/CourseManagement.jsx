import { useState } from "react";
import { Card, CardContent, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Button } from "../components/ui/button";

export default function CourseManagement() {
  const [courses, setCourses] = useState([
    { id: 1, name: "Kubernetes 101" },
    { id: 2, name: "Distributed Systems" },
  ]);
  const [newCourse, setNewCourse] = useState("");

  const addCourse = () => {
    if (!newCourse.trim()) return;
    setCourses([
      ...courses,
      { id: Date.now(), name: newCourse.trim() }
    ]);
    setNewCourse("");
  };

  const deleteCourse = (id) => {
    setCourses(courses.filter(c => c.id !== id));
  };

  return (
    <div className="p-6 space-y-4">
      <Card>
        <CardTitle>Add Course</CardTitle>
        <CardContent className="flex gap-2">
          <Input
            placeholder="Course name"
            value={newCourse}
            onChange={(e) => setNewCourse(e.target.value)}
          />
          <Button onClick={addCourse}>Add</Button>
        </CardContent>
      </Card>

      <Card>
        <CardTitle>Your Courses</CardTitle>
        <CardContent>
          {courses.map(course => (
            <div key={course.id} className="flex justify-between py-2 border-b">
              <span>{course.name}</span>
              <Button className="bg-red-500" onClick={() => deleteCourse(course.id)}>Delete</Button>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
