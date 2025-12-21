package IO;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.StringJoiner;

public class DataGenerator {

        //                      TESTING
        private static final int NUM_CLASSROOMS = 156;
        private static final int NUM_COURSES = 1178;
        private static final int NUM_STUDENTS = 8000;

        // File Names
        private static final String FILE_CLASSROOMS = "sampleData_AllClassroomsAndTheirCapacities.csv";
        private static final String FILE_COURSES = "sampleData_AllCoursesWithTime.csv";
        private static final String FILE_STUDENTS = "sampleData_AllStudents.csv";
        private static final String FILE_ATTENDANCE = "sampleData_AllAttendanceLists.csv";

        private static final Random random = new Random();

        public static void main(String[] args) {
                System.out.println("Generating larger dataset with specific structure...");

                generateClassrooms();
                generateCourses();
                generateStudents();
                generateAttendance();

                System.out.println("Generation Complete. Files Created:");
                System.out.println(" - " + FILE_CLASSROOMS);
                System.out.println(" - " + FILE_COURSES);
                System.out.println(" - " + FILE_STUDENTS);
                System.out.println(" - " + FILE_ATTENDANCE);
        }

        private static void generateClassrooms() {
                try (FileWriter writer = new FileWriter(FILE_CLASSROOMS)) {
                        writer.write("ALL OF THE CLASSROOMS; AND THEIR CAPACITIES IN THE SYSTEM\n");

                        for (int i = 1; i <= NUM_CLASSROOMS; i++) {
                                String roomId = "Classroom_" + String.format("%02d", i);
                                int capacity = 40 + random.nextInt(81); // Capacity 40 to 120
                                writer.write(roomId + ";" + capacity + "\n");
                        }
                } catch (IOException e) {
                        ErrorHandler.getInstance().logError("Sınıfları oluştururken hata.");
                }
        }

        private static void generateCourses() {
                try (FileWriter writer = new FileWriter(FILE_COURSES)) {
                        writer.write("ALL OF THE COURSES IN THE SYSTEM AND THEIR EXAM TIME\n");

                        int[] durations = {60, 70, 75, 80, 85, 90, 100, 110, 120};

                        for (int i = 1; i <= NUM_COURSES; i++) {
                                String courseId = "CourseCode_" + String.format("%02d", i);
                                int duration = durations[random.nextInt(durations.length)];
                                writer.write(courseId + "," + duration + "\n");
                        }
                } catch (IOException e) {
                        ErrorHandler.getInstance().logError("Dersleri oluştururken hata.");
                }
        }

        private static void generateStudents() {
                try (FileWriter writer = new FileWriter(FILE_STUDENTS)) {
                        writer.write("ALL OF THE STUDENTS IN THE SYSTEM\n");

                        for (int i = 1; i <= NUM_STUDENTS; i++) {
                                String stdId = "Std_ID_" + String.format("%03d", i);
                                writer.write(stdId + "\n");
                        }
                } catch (IOException e) {
                        ErrorHandler.getInstance().logError("Öğrencileri oluştururken hata");
                }
        }

        private static void generateAttendance() {
                // Structure:
                // CourseCode_01
                // ['Std_ID_xxx', 'Std_ID_yyy']
                try (FileWriter writer = new FileWriter(FILE_ATTENDANCE)) {



                        for (int i = 1; i <= NUM_COURSES; i++) {
                                String courseId = "CourseCode_" + String.format("%02d", i);

                                writer.write(courseId + "\n");

                                int classSize = 20 + random.nextInt(31);
                                HashSet<Integer> enrolledStudents = new HashSet<>();
                                while (enrolledStudents.size() < classSize) {
                                        enrolledStudents.add(1 + random.nextInt(NUM_STUDENTS));
                                }


                                StringJoiner joiner = new StringJoiner(", ", "[", "]");
                                for (int stdIdx : enrolledStudents) {
                                        String stdId = "'Std_ID_" + String.format("%03d", stdIdx) + "'";
                                        joiner.add(stdId);
                                }


                                writer.write(joiner.toString() + "\n");
                        }
                } catch (IOException e) {
                        ErrorHandler.getInstance().logError("Katılımı oluştururken hata.");
                        System.err.println(e.getMessage());
                }
        }
}