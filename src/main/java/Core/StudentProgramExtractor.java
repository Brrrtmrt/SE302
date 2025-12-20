package Core;

import Helpers.TimeSlot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentProgramExtractor {

    private final HashMap<Course, Integer> schedule;
    private final ArrayList<TimeSlot> timeSlots;

    /**
     * Constructor requires the generated schedule and the list of active time slots.
     * These usually come from the Scheduler instance after generate_schedule() is called.
     *
     * @param schedule  Map of Course -> Slot Index
     * @param timeSlots List of TimeSlot objects corresponding to the indices
     */
    public StudentProgramExtractor(HashMap<Course, Integer> schedule, ArrayList<TimeSlot> timeSlots) {
        this.schedule = schedule;
        this.timeSlots = timeSlots;
    }

    /**
     * Extracts the exams that a specific student has and their times.
     *
     * @param studentID The ID of the student (e.g., "Std_ID_001")
     * @return A list of strings describing the exams.
     */
    public List<String> getExamsForStudent(String studentID) {
        List<String> studentExams = new ArrayList<>();

        if (schedule == null || timeSlots == null) {
            return studentExams;
        }

        // Iterate through the schedule to find courses this student is enrolled in
        for (Map.Entry<Course, Integer> entry : schedule.entrySet()) {
            Course course = entry.getKey();
            
            // Check if the student is in this course
            if (course.getEnrolledStudentIDs().contains(studentID)) {
                int slotIndex = entry.getValue();
                
                // Retrieve the time details using the slot index
                if (slotIndex >= 0 && slotIndex < timeSlots.size()) {
                    TimeSlot slot = timeSlots.get(slotIndex);
                    
                    // Format: CourseCode | Date | Time
                    String examInfo = String.format("%s | %s | %s | %d mins", 
                                        course.getID(), 
                                        slot.getDate(), 
                                        slot.getTime(),
                                        course.getDuration());
                    
                    studentExams.add(examInfo);
                }
            }
        }
        return studentExams;
    }
}
