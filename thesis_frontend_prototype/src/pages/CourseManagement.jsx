import React, { useState, useEffect } from "react";
import api from "../services/api";
import { Card, CardContent, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Button } from "../components/ui/button";
import { useAuth } from "../contexts/AuthContext";

export default function CourseManagement() {
  const { user, isAdmin } = useAuth();
  const [courses, setCourses] = useState([]);
  const [newCourse, setNewCourse] = useState({ name: "", description: "" });
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState(null);
  const [selectedUserId, setSelectedUserId] = useState("");
  const [selectedTeacherId, setSelectedTeacherId] = useState("");
  const [courseSearch, setCourseSearch] = useState("");
  const [coursePage, setCoursePage] = useState(1);
  const [userSearch, setUserSearch] = useState("");
  const coursePageSize = 5;
  const teacherOptions = users.filter(u => u.role === "ROLE_TEACHER");
  const handleAssignTeacher = async (courseId, teacherId) => {
    if (!teacherId) return;
    setLoading(true);
    try {
      await api.post(`/courses/${courseId}/assign-teacher`, { teacherId });
      loadCourses();
      setSelectedTeacherId("");
    } catch (error) {
      console.error("Failed to assign teacher:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCourses();
    loadUsers();
  }, [user]);

  const loadCourses = async () => {
    setLoading(true);
    try {
      const data = await api.get("/courses");
      setCourses(data);
    } catch (error) {
      console.error("Failed to load courses:", error);
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    setLoading(true);
    try {
      if (user?.token) {
        api.setToken(user.token);
      }
      const data = isAdmin()
        ? await api.get("/superadmin/users")
        : await api.get("/users/students");
      setUsers(data);
    } catch (error) {
      console.error("Failed to load users:", error);
    } finally {
      setLoading(false);
    }
  };
  const handleEnrollUser = async (courseId, userId) => {
    if (!userId) return;
    setLoading(true);
    try {
      await api.post(`/courses/${courseId}/enroll`, { userId });
      loadCourses();
      setSelectedUserId("");
    } catch (error) {
      console.error("Failed to enroll user:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateCourse = async () => {
    if (!newCourse.name) return;
    setLoading(true);
    try {
      await api.post("/courses", newCourse);
      setNewCourse({ name: "", description: "" });
      loadCourses();
    } catch (error) {
      console.error("Failed to create course:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteCourse = async (id) => {
    setLoading(true);
    try {
      await api.delete(`/courses/${id}`);
      loadCourses();
    } catch (error) {
      console.error("Failed to delete course:", error);
    } finally {
      setLoading(false);
    }
  };

  const filteredCourses = courses.filter(course => {
    const query = courseSearch.trim().toLowerCase();
    if (!query) return true;
    return (
      course.name?.toLowerCase().includes(query) ||
      course.description?.toLowerCase().includes(query)
    );
  });

  const totalCoursePages = Math.max(1, Math.ceil(filteredCourses.length / coursePageSize));
  const currentCoursePage = Math.min(coursePage, totalCoursePages);
  const pagedCourses = filteredCourses.slice(
    (currentCoursePage - 1) * coursePageSize,
    currentCoursePage * coursePageSize
  );

  const filteredUsers = users.filter(u => {
    const query = userSearch.trim().toLowerCase();
    if (!query) return true;
    return (
      u.username?.toLowerCase().includes(query) ||
      u.email?.toLowerCase().includes(query)
    );
  });

  return (
    <div className="p-6 space-y-6">
      <Card>
        <CardTitle>Add Course</CardTitle>
        <CardContent className="flex gap-2">
          <Input
            placeholder="Course name"
            value={newCourse.name}
            onChange={e => setNewCourse({ ...newCourse, name: e.target.value })}
          />
          <Input
            placeholder="Description"
            value={newCourse.description}
            onChange={e => setNewCourse({ ...newCourse, description: e.target.value })}
          />
          <Button onClick={handleCreateCourse} disabled={loading}>Add</Button>
        </CardContent>
      </Card>

      <Card>
        <CardTitle>All Courses</CardTitle>
        <CardContent>
          <div className="mb-3 flex items-center gap-2">
            <Input
              placeholder="Search courses..."
              value={courseSearch}
              onChange={e => {
                setCourseSearch(e.target.value);
                setCoursePage(1);
              }}
            />
            <Input
              placeholder="Search users..."
              value={userSearch}
              onChange={e => setUserSearch(e.target.value)}
            />
          </div>
          {pagedCourses.map(course => (
            <div key={course.id} className="border-b py-4">
              <div className="flex justify-between items-center">
                <span>
                  <span className="font-semibold">{course.name}</span>
                  <span className="text-xs text-gray-500 ml-2">{course.description}</span>
                </span>
                {isAdmin() && (
                  <Button className="bg-red-500" onClick={() => handleDeleteCourse(course.id)} disabled={loading}>Delete</Button>
                )}
              </div>
              <div className="mt-2 flex gap-4">
                {/* Teacher assignment UI */}
                {isAdmin() && (
                  <div className="flex gap-2 items-center">
                    <select
                      value={selectedCourseId === course.id ? selectedTeacherId : ""}
                      onChange={e => {
                        setSelectedCourseId(course.id);
                        setSelectedTeacherId(e.target.value);
                      }}
                      className="border rounded px-2 py-1"
                    >
                      <option value="">Assign teacher</option>
                      {teacherOptions.map(teacher => (
                        <option key={teacher.id} value={teacher.id}>{teacher.username}</option>
                      ))}
                    </select>
                    <Button
                      onClick={() => handleAssignTeacher(course.id, selectedTeacherId)}
                      disabled={loading || !selectedTeacherId}
                    >Assign</Button>
                    {course.teacher ? (
                      <span className="ml-2 text-sm text-green-700">Assigned: {course.teacher.username}</span>
                    ) : (
                      <span className="ml-2 text-sm text-gray-500">No teacher assigned</span>
                    )}
                  </div>
                )}
                {/* Enrollment UI */}
                <div className="flex gap-2 items-center">
                  <select
                    value={selectedCourseId === course.id ? selectedUserId : ""}
                    onChange={e => {
                      setSelectedCourseId(course.id);
                      setSelectedUserId(e.target.value);
                    }}
                    className="border rounded px-2 py-1"
                  >
                    <option value="">Select user to enroll</option>
                    {filteredUsers
                      .filter(user => {
                        const enrolledIds = course.enrollments ? course.enrollments.map(e => e.student.id) : [];
                        return !enrolledIds.includes(user.id);
                      })
                      .map(user => (
                        <option key={user.id} value={user.id}>
                          {user.username} ({user.email})
                        </option>
                      ))}
                  </select>
                  <Button
                    onClick={() => handleEnrollUser(course.id, selectedUserId)}
                    disabled={loading || !selectedUserId}
                  >Enroll</Button>
                </div>
              </div>
              <div className="mt-2">
                <span className="font-semibold text-sm">Enrolled Users:</span>
                <ul className="ml-4 list-disc">
                  {course.enrollments && course.enrollments.length > 0 ? (
                    course.enrollments.map(e => (
                      <li key={e.student.id}>{e.student.username} ({e.student.email})</li>
                    ))
                  ) : (
                    <li className="text-gray-400">No users enrolled</li>
                  )}
                </ul>
              </div>
            </div>
          ))}
          <div className="mt-4 flex items-center justify-between">
            <span className="text-sm text-gray-500">
              Page {currentCoursePage} of {totalCoursePages}
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={() => setCoursePage(Math.max(1, currentCoursePage - 1))}
                disabled={currentCoursePage === 1}
              >
                Prev
              </Button>
              <Button
                variant="outline"
                onClick={() => setCoursePage(Math.min(totalCoursePages, currentCoursePage + 1))}
                disabled={currentCoursePage === totalCoursePages}
              >
                Next
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
