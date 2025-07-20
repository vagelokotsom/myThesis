import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswords {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String teacherPassword = "teacher123";
        String studentPassword = "student123";
        
        String teacherHash = encoder.encode(teacherPassword);
        String studentHash = encoder.encode(studentPassword);
        
        System.out.println("Teacher password hash: " + teacherHash);
        System.out.println("Student password hash: " + studentHash);
        
        // Verify the hashes work
        System.out.println("Teacher hash matches: " + encoder.matches(teacherPassword, teacherHash));
        System.out.println("Student hash matches: " + encoder.matches(studentPassword, studentHash));
    }
}
