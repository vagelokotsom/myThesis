import { useEffect, useState } from "react";
import api from "../services/api";
import { Button } from "../components/ui/button";
import { Card, CardHeader, CardTitle, CardContent } from "../components/ui/card";

export default function SuperAdminDashboard() {
    const [users, setUsers] = useState([]);
    const [newUser, setNewUser] = useState({ username: "", email: "", password: "", role: "ROLE_STUDENT" });
    const [loading, setLoading] = useState(false);
    const [userSearch, setUserSearch] = useState("");
    const [userPage, setUserPage] = useState(1);
    const userPageSize = 8;
    // Course management state
    const [courses, setCourses] = useState([]);
    const [newCourse, setNewCourse] = useState({ name: "", description: "" });
    const [selectedCourseId, setSelectedCourseId] = useState(null);
    const [selectedUserId, setSelectedUserId] = useState("");
    const [selectedTeacherId, setSelectedTeacherId] = useState("");
    const [enrollSearch, setEnrollSearch] = useState("");
    const [enrollResults, setEnrollResults] = useState([]);
    const [enrollPage, setEnrollPage] = useState(0);
    const [enrollHasMore, setEnrollHasMore] = useState(false);
    const [enrollLoading, setEnrollLoading] = useState(false);
    const [activeEnrollCourseId, setActiveEnrollCourseId] = useState(null);
    const [courseSearch, setCourseSearch] = useState("");
    const [coursePage, setCoursePage] = useState(1);
    const coursePageSize = 5;

    useEffect(() => {
        loadUsers();
        loadCourses();
    }, []);

    const loadUsers = async () => {
        setLoading(true);
        try {
            const data = await api.get("/superadmin/users");
            setUsers(data);
        } catch (error) {
            console.error("Failed to load users:", error);
        } finally {
            setLoading(false);
        }
    };

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

    const teacherOptions = users.filter(u => u.role === "ROLE_TEACHER");
    const studentOptions = users.filter(u => u.role === "ROLE_STUDENT");

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

    const loadEnrollStudents = async ({ courseId, query, page, reset }) => {
        try {
            setEnrollLoading(true);
            const result = await api.searchStudents(query || "", page, 8);
            const items = result?.items || [];
            setEnrollResults(prev => reset ? items : [...prev, ...items]);
            setEnrollHasMore(Boolean(result?.hasMore));
            setEnrollPage(page);
            setActiveEnrollCourseId(courseId);
        } catch (error) {
            console.error("Failed to search students:", error);
        } finally {
            setEnrollLoading(false);
        }
    };

    const handleCreateUser = async () => {
        if (!newUser.username || !newUser.email || !newUser.password) return;
        setLoading(true);
        try {
            await api.post("/superadmin/users", newUser);
            setNewUser({ username: "", email: "", password: "", role: "ROLE_STUDENT" });
            loadUsers();
        } catch (error) {
            console.error("Failed to create user:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteUser = async (id) => {
        setLoading(true);
        try {
            await api.delete(`/superadmin/users/${id}`);
            loadUsers();
        } catch (error) {
            console.error("Failed to delete user:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleRoleChange = async (id, role) => {
        setLoading(true);
        try {
            await api.put(`/superadmin/users/${id}/role?role=${role}`);
            loadUsers();
        } catch (error) {
            console.error("Failed to update user role:", error);
        } finally {
            setLoading(false);
        }
    };

    const filteredUsers = users.filter(u => {
        const query = userSearch.trim().toLowerCase();
        if (!query) return true;
        return (
            u.username?.toLowerCase().includes(query) ||
            u.email?.toLowerCase().includes(query) ||
            u.role?.toLowerCase().includes(query)
        );
    });

    const totalUserPages = Math.max(1, Math.ceil(filteredUsers.length / userPageSize));
    const currentUserPage = Math.min(userPage, totalUserPages);
    const pagedUsers = filteredUsers.slice(
        (currentUserPage - 1) * userPageSize,
        currentUserPage * userPageSize
    );

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

    return (
        <div className="p-6 space-y-6">
            <Card>
                <CardHeader>
                    <CardTitle>Super Admin: User Management</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="mb-3 flex items-center gap-2">
                        <input
                            type="text"
                            placeholder="Search users..."
                            value={userSearch}
                            onChange={e => {
                                setUserSearch(e.target.value);
                                setUserPage(1);
                            }}
                            className="border rounded px-2 py-1 flex-1"
                        />
                    </div>
                    <div className="mb-4">
                        <h2 className="font-semibold mb-2">Create New User</h2>
                        <div className="flex gap-2 mb-2">
                            <input
                                type="text"
                                placeholder="Username"
                                value={newUser.username}
                                onChange={e => setNewUser({ ...newUser, username: e.target.value })}
                                className="border rounded px-2 py-1"
                            />
                            <input
                                type="email"
                                placeholder="Email"
                                value={newUser.email}
                                onChange={e => setNewUser({ ...newUser, email: e.target.value })}
                                className="border rounded px-2 py-1"
                            />
                            <input
                                type="password"
                                placeholder="Password"
                                value={newUser.password}
                                onChange={e => setNewUser({ ...newUser, password: e.target.value })}
                                className="border rounded px-2 py-1"
                            />
                            <select
                                value={newUser.role}
                                onChange={e => setNewUser({ ...newUser, role: e.target.value })}
                                className="border rounded px-2 py-1"
                            >
                                <option value="ROLE_STUDENT">Student</option>
                                <option value="ROLE_TEACHER">Teacher</option>
                                <option value="ROLE_ADMIN">Admin</option>
                            </select>
                            <Button onClick={handleCreateUser} disabled={loading}>Create</Button>
                        </div>
                    </div>
                    <h2 className="font-semibold mb-2">All Users</h2>
                    <table className="w-full border">
                        <thead>
                            <tr className="bg-gray-100">
                                <th className="p-2">Username</th>
                                <th className="p-2">Email</th>
                                <th className="p-2">Role</th>
                                <th className="p-2">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {pagedUsers.map(user => (
                                <tr key={user.id} className="border-t">
                                    <td className="p-2">{user.username}</td>
                                    <td className="p-2">{user.email}</td>
                                    <td className="p-2">
                                        <select
                                            value={user.role}
                                            onChange={e => handleRoleChange(user.id, e.target.value)}
                                            className="border rounded px-2 py-1"
                                            disabled={loading}
                                        >
                                            <option value="ROLE_STUDENT">Student</option>
                                            <option value="ROLE_TEACHER">Teacher</option>
                                            <option value="ROLE_ADMIN">Admin</option>
                                        </select>
                                    </td>
                                    <td className="p-2">
                                        <Button onClick={() => handleDeleteUser(user.id)} disabled={loading} className="bg-red-500 hover:bg-red-600 text-white">Delete</Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <div className="mt-4 flex items-center justify-between">
                        <span className="text-sm text-gray-500">
                            Page {currentUserPage} of {totalUserPages}
                        </span>
                        <div className="flex gap-2">
                            <Button
                                variant="outline"
                                onClick={() => setUserPage(Math.max(1, currentUserPage - 1))}
                                disabled={currentUserPage === 1}
                            >
                                Prev
                            </Button>
                            <Button
                                variant="outline"
                                onClick={() => setUserPage(Math.min(totalUserPages, currentUserPage + 1))}
                                disabled={currentUserPage === totalUserPages}
                            >
                                Next
                            </Button>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Course Management Section */}
            <Card className="mt-8">
                <CardHeader>
                    <CardTitle>Course Management</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="mb-3 flex items-center gap-2">
                        <input
                            type="text"
                            placeholder="Search courses..."
                            value={courseSearch}
                            onChange={e => {
                                setCourseSearch(e.target.value);
                                setCoursePage(1);
                            }}
                            className="border rounded px-2 py-1 flex-1"
                        />
                    </div>
                    <div className="mb-4 flex gap-2">
                        <input
                            type="text"
                            placeholder="Course name"
                            value={newCourse.name}
                            onChange={e => setNewCourse({ ...newCourse, name: e.target.value })}
                            className="border rounded px-2 py-1"
                        />
                        <input
                            type="text"
                            placeholder="Description"
                            value={newCourse.description}
                            onChange={e => setNewCourse({ ...newCourse, description: e.target.value })}
                            className="border rounded px-2 py-1"
                        />
                        <Button onClick={handleCreateCourse} disabled={loading}>Add</Button>
                    </div>
                    <h2 className="font-semibold mb-2">All Courses</h2>
                    {pagedCourses.map(course => (
                        <div key={course.id} className="border-b py-4">
                            <div className="flex justify-between items-center">
                                <span>
                                    <span className="font-semibold">{course.name}</span>
                                    <span className="text-xs text-gray-500 ml-2">{course.description}</span>
                                </span>
                                <Button className="bg-red-500" onClick={() => handleDeleteCourse(course.id)} disabled={loading}>Delete</Button>
                            </div>
                            <div className="mt-3 grid gap-4 md:grid-cols-2">
                                <div className="rounded border p-3">
                                    <div className="text-xs uppercase text-gray-500 mb-2">Course Teacher</div>
                                    <div className="text-sm mb-2">
                                        {course.teacher ? (
                                            <span className="text-green-700 font-medium">Assigned: {course.teacher.username}</span>
                                        ) : (
                                            <span className="text-gray-500">No teacher assigned</span>
                                        )}
                                    </div>
                                    <div className="flex gap-2 items-center">
                                        <select
                                            value={selectedCourseId === course.id ? selectedTeacherId : ""}
                                            onChange={e => {
                                                setSelectedCourseId(course.id);
                                                setSelectedTeacherId(e.target.value);
                                            }}
                                            className="border rounded px-2 py-1 flex-1"
                                        >
                                            <option value="">Select teacher</option>
                                            {teacherOptions.map(teacher => (
                                                <option key={teacher.id} value={teacher.id}>{teacher.username}</option>
                                            ))}
                                        </select>
                                        <Button
                                            onClick={() => handleAssignTeacher(course.id, selectedTeacherId)}
                                            disabled={loading || !selectedTeacherId}
                                        >Assign</Button>
                                    </div>
                                </div>

                                <div className="rounded border p-3">
                                    <div className="text-xs uppercase text-gray-500 mb-2">Enroll Students</div>
                                    <div className="text-sm mb-2 text-gray-600">
                                        Enrolled: {course.enrollments ? course.enrollments.length : 0}
                                    </div>
                                    <input
                                        type="text"
                                        placeholder="Search students..."
                                        value={enrollSearch}
                                        onFocus={() => {
                                            setActiveEnrollCourseId(course.id);
                                            if (enrollResults.length === 0) {
                                                loadEnrollStudents({ courseId: course.id, query: enrollSearch, page: 0, reset: true });
                                            }
                                        }}
                                        onChange={e => {
                                            const nextQuery = e.target.value;
                                            setEnrollSearch(nextQuery);
                                            setActiveEnrollCourseId(course.id);
                                            loadEnrollStudents({ courseId: course.id, query: nextQuery, page: 0, reset: true });
                                        }}
                                        className="border rounded px-2 py-1 mb-2 w-full"
                                    />
                                    <div className="flex gap-2 items-center">
                                        <select
                                            value={selectedCourseId === course.id ? selectedUserId : ""}
                                            onChange={e => {
                                                setSelectedCourseId(course.id);
                                                setSelectedUserId(e.target.value);
                                            }}
                                            className="border rounded px-2 py-1 flex-1"
                                        >
                                            <option value="">Select student</option>
                                            {(activeEnrollCourseId === course.id ? enrollResults : studentOptions)
                                                .filter(user => {
                                                    const enrolledIds = course.enrollments ? course.enrollments.map(e => e.student.id) : [];
                                                    if (enrolledIds.includes(user.id)) return false;
                                                    if (activeEnrollCourseId === course.id) return true;
                                                    const query = enrollSearch.trim().toLowerCase();
                                                    if (!query) return true;
                                                    return (
                                                        user.username?.toLowerCase().includes(query) ||
                                                        user.email?.toLowerCase().includes(query)
                                                    );
                                                })
                                                .map(user => (
                                                    <option key={user.id} value={user.id}>{user.username} ({user.email})</option>
                                                ))}
                                        </select>
                                        <Button
                                            onClick={() => handleEnrollUser(course.id, selectedUserId)}
                                            disabled={loading || !selectedUserId}
                                        >Enroll</Button>
                                    </div>
                                    {activeEnrollCourseId === course.id && (
                                        <div className="mt-2 flex items-center justify-between text-xs text-gray-500">
                                            <span>{enrollLoading ? "Loading..." : enrollResults.length ? `${enrollResults.length} result(s)` : "No results yet"}</span>
                                            {enrollHasMore && (
                                                <Button
                                                    variant="outline"
                                                    size="sm"
                                                    onClick={() => loadEnrollStudents({ courseId: course.id, query: enrollSearch, page: enrollPage + 1, reset: false })}
                                                    disabled={enrollLoading}
                                                >
                                                    Load more
                                                </Button>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                            <div className="mt-2">
                                <span className="font-semibold text-sm">Enrolled Students:</span>
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
