package IO;

import Core.ClassRoom;
import Core.Course;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

public class Exporter {

    public static void exportSchedule(
            Path filePath,
            String examName,
            HashMap<Course, Integer> schedule,   // Course -> slot
            HashMap<Integer, String[]> slotMap,  // slot -> [date, time]
            HashMap<Course, ClassRoom> roomMap   // Course -> ClassRoom
    ) {

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {

            // HEADER
            writer.write("ExamName,CourseID,ClassRoom,Date,StartTime,EndTime,Students");

            writer.newLine();

            for (var entry : schedule.entrySet()) {
                Course course = entry.getKey();
                int slot = entry.getValue();

                String[] dateTime = slotMap.get(slot);
                if (dateTime == null || dateTime.length < 3) continue;

                String date = dateTime[0];
                String startTime = dateTime[1];
                String endTime = dateTime[2];


                ClassRoom room = roomMap.get(course);
                if (room == null) continue;

                // Students
                Set<String> students = course.getEnrolledStudentIDs();
                String studentStr = String.join("|", students);

                writer.write(
                        examName + "," +
                                course.getID() + "," +
                                room.getName() + "," +
                                date + "," +
                                startTime + "," +
                                endTime + "," +
                                studentStr
                );


                writer.newLine();
            }

            System.out.println("CSV export completed: " + filePath);

        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }
}
