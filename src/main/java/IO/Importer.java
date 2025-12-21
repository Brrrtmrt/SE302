package IO;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import Core.ClassRoom;
import Core.Course;
import Core.Student;


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
                                        classRooms.add(classRoom);
                                }
                        }
                } catch (Exception e) {
                        ErrorHandler.getInstance().logError("Sınıflar dosyasını içe aktarırken okuma hatası.");
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
                                        students.add(student);
                                }
                        }
                } catch (IOException e) {
                        ErrorHandler.getInstance().logError("Öğrenciler dosyasını içe aktarırken okuma hatası.");
                }
                return students;
        }

        public static ArrayList<Course> importCourses(Path filePath) {
                String separator = new Importer().detectSeparator(filePath);
                if (separator.equals("+")) {
                        ArrayList<Course> courses = new ArrayList<>();
                        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                                reader.readLine(); // Skip header line
                                String line;
                                while ((line = reader.readLine()) != null) {
                                        line = line.trim();
                                        if (!line.isEmpty()) {


                                                Course course = new Course(line, 90);
                                                courses.add(course);

                                        }
                                }

                        } catch (IOException e) {
                                ErrorHandler.getInstance().logError("Dersler dosyasını içe aktarırken okuma hatası.");
                        }
                        return courses;

                }//if

                else {
                        ArrayList<Course> courses = new ArrayList<>();
                        if (separator.equals(",")) {

                                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                                        reader.readLine(); // Skip header line
                                        String line;
                                        while ((line = reader.readLine()) != null) {
                                                String[] parts = line.split(separator);
                                                if (parts.length >= 1) {
                                                        String id = parts[0].trim();
                                                        int duration = parts[1].trim().isEmpty() ? 90 : Integer.parseInt(parts[1].trim());
                                                        Course course = new Course(id, duration);
                                                        courses.add(course);
                                                }

                                        }


                                } catch (IOException e) {
                                        ErrorHandler.getInstance().logError("Dersler dosyasını içe aktarırken okuma hatası.");
                                }


                        }
                        return courses;
                }
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
                                                continue;
                                        }

                                        // Clean the string
                                        String cleanLine = line.replace("[", "").replace("]", "").replace("'", "");

                                        Course course = new Course(currentCourseID, 90);

                                        // Only split if there is content (handles "[]" empty lists)
                                        if (!cleanLine.isBlank()) {
                                                String[] studentIDs = cleanLine.split(separator);
                                                for (String id : studentIDs) {
                                                        course.addEnrolledStudentID(id.trim());
                                                }
                                        }
                                        courses.add(course);
                                } else {
                                        // --- Process Course ID ---
                                        // If it's not a list, it must be the ID for the next batch
                                        currentCourseID = line;
                                }

                        }

                } catch (IOException e) {
                        ErrorHandler.getInstance().logError("Katılım dosyasını içe aktarırken okuma hatası.");
                }
                return courses;
        }


        private String detectSeparator(Path filePath) {
                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                        String line;
                        reader.readLine(); // Skip header line
                        while ((line = reader.readLine()) != null) {
                                if (line.trim().isEmpty()) continue;
                                if (line.contains(",")) return ",";
                                if (line.contains(";")) return ";";
                                else {
                                        return "+";
                                }
                        }
                } catch (IOException e) {
                        ErrorHandler.getInstance().logError("Dosyayı içe aktarırken ayırıcı belirlenemedi.");
                }
                return ","; // Default fallback
        }
}