package IO;

import java.nio.file.Path;
import java.util.HashMap;

import Core.ClassRoom;
import Core.Course;

public class TestMain {

    public static void main(String[] args) {

        // --- Courses ---
        Course ce323 = new Course("CE 323",90);
        ce323.addEnrolledStudentID("S1");
        ce323.addEnrolledStudentID("S2");
        ce323.addEnrolledStudentID("S3");

        Course se323 = new Course("SE 323",80);
        se323.addEnrolledStudentID("S2");
        se323.addEnrolledStudentID("S4");

        // --- Schedule (Course -> Slot) ---
        HashMap<Course, Integer> schedule = new HashMap<>();
        schedule.put(ce323, 1);
        schedule.put(se323, 2);

        // --- Slot -> Date / Time ---
        HashMap<Integer, String[]> slotMap = new HashMap<>();
        slotMap.put(1, new String[]{"2025-01-15", "08:30", "10:00"});
        slotMap.put(2, new String[]{"2025-01-15", "10:20", "11:50"});

        // --- ClassRooms ---
        ClassRoom a101 = new ClassRoom("A101", 40);
        ClassRoom b202 = new ClassRoom("B202", 30);

        HashMap<Course, ClassRoom> roomMap = new HashMap<>();
        roomMap.put(ce323, a101);
        roomMap.put(se323, b202);

        // --- EXPORT ---
        Exporter.exportSchedule(
                Path.of("exam_schedule_test.csv"),
                "Final Exams",
                schedule,
                slotMap,
                roomMap
        );

        System.out.println("Test finished.");
    }
}
