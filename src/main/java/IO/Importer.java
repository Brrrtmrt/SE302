package IO;

import Core.ClassRoom;
import Core.Course;
import Core.Student;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;


public class Importer {
        public static ArrayList<ClassRoom> importClassRooms(Path filePath) {
                ArrayList<ClassRoom> classRooms = new ArrayList<>();
                String separator = new Importer().detectSeparator(filePath);

                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                        reader.readLine(); // Skip header line
                        String line;
                        while ((line = reader.readLine()) != null) {
                                String[] parts = line.split(separator);
                                if (parts.length >= 2) {
                                        String name = parts[0].trim();
                                        int capacity = Integer.parseInt(parts[1].trim());
                                        ClassRoom classRoom = new ClassRoom(name, capacity);
                                        System.out.println("Imported ClassRoom: " + classRoom.getName() + " with capacity " + classRoom.getCapacity());
                                }
                        }
                } catch (IOException e) {
                        System.out.println("Error reading ClassRooms file: " + e.getMessage());
                }
                return classRooms;
        }


        public static ArrayList<Student> importStudents(Path filePath) {
                ArrayList<Student> students = new ArrayList<>();
                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                        reader.readLine(); // Skip header line
                        String line;
                        while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (!line.isEmpty()) {


                                        Student student = new Student(line);
                                        System.out.println("Imported Student with ID: " + student.ID());
                                        students.add(student);
                                }
                        }
                } catch (IOException e) {
                        System.out.println("Error reading Students file: " + e.getMessage());

                }
                return students;
        }

        public static ArrayList<Course> importCourses(Path filePath) {
                ArrayList<Course> courses = new ArrayList<>();
                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                        reader.readLine(); // Skip header line
                        String line;
                        while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (!line.isEmpty()) {


                                        Course course = new Course(line);
                                        System.out.println("Imported Course with ID: " + course.getID());
                                        courses.add(course);

                                }
                        }

                } catch (IOException e) {
                        System.out.println("Error reading Students file: " + e.getMessage());

                }
                return courses;
        }

        public static ArrayList<Course> importAttandenceLists(Path filePath) {
                ArrayList<Course> courses = new ArrayList<>();
                String separator = new Importer().detectSeparator(filePath);

                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                        // reader.readLine(); // Skip header line no header present

                        String line;
                        String currentCourseID = null; // Holds the ID until the list is found

                        while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (line.isEmpty()) continue; // CRITICAL FIX: Skip blank lines to prevent desync

                                // DECISION LOGIC:
                                // If line has brackets '[' or the separator, it's a Student List.
                                // Otherwise, it is a Course ID.
                                boolean isStudentList = line.contains("[") || line.contains(separator);

                                if (isStudentList) {
                                        // --- Process Student List ---
                                        if (currentCourseID == null) {
                                                System.out.println("Skipping orphan student list (no course ID found): " + line);
                                                continue;
                                        }

                                        // Clean the string
                                        String cleanLine = line.replace("[", "").replace("]", "").replace("'", "");

                                        Course course = new Course(currentCourseID);

                                        // Only split if there is content (handles "[]" empty lists)
                                        if (!cleanLine.isBlank()) {
                                                String[] studentIDs = cleanLine.split(separator);
                                                for (String id : studentIDs) {
                                                        course.addEnrolledStudentID(id.trim());
                                                }
                                        }

                                        System.out.println("Imported Attendance List for: " + course.getID() + " with Students: " + course.getEnrolledStudentIDs());
                                        courses.add(course);
                                } else {
                                        // --- Process Course ID ---
                                        // If it's not a list, it must be the ID for the next batch
                                        currentCourseID = line;
                                }

                        }

                } catch (IOException e) {
                        System.out.println("Error reading Attendance file: " + e.getMessage());

                }
                return courses;
        }


        private String detectSeparator(Path filePath) {
                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                                if (line.trim().isEmpty()) continue;
                                if (line.contains(",")) return ",";
                                if (line.contains(";")) return ";";
                        }
                } catch (IOException e) {
                        System.err.println("Error detecting separator: " + e.getMessage());
                }
                return ","; // Default fallback
        }

        public static void main(String[] args) {
                
                ArrayList<Course> courses2 = importCourses(Path.of("docs\\sampleData_AllCourses.csv"));
                for (Course course : courses2) {
                        System.out.println("Course ID: " + course.getID());
                }
                ArrayList<Student> students2 = importStudents(Path.of("docs\\sampleData_AllStudents.csv"));
                for (Student student : students2) {
                        System.out.println("Student ID: " + student.ID());
                }
                ArrayList<ClassRoom> classRooms2 = importClassRooms(Path.of("docs\\sampleData_AllClassroomsAndTheirCapacities.csv"));
                for (ClassRoom classRoom : classRooms2) {
                        System.out.println("ClassRoom Name: " + classRoom.getName() + ", Capacity: " + classRoom.getCapacity());
                }
                ArrayList<Course> courses3 = importAttandenceLists(Path.of("docs\\sampleData_AllAttendanceLists.csv"));
                for (Course course : courses3) {
                        System.out.println("Course ID: " + course.getID() + ", Enrolled Students: " + course.getEnrolledStudentIDs());
                }
        }


}