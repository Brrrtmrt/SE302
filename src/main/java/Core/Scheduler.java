package Core;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import Helpers.TimeSlot;
import IO.Importer;

import static Tests.Debug.*;


public class Scheduler {


        private long startTime;
        private final long TIMEOUT_MS = 3000;


        public Scheduler() {
        }

        private ArrayList<Course> courses;
        private ArrayList<LocalTime> slotIds;
        private ArrayList<Integer> slots;
        private HashMap<Course, Integer> schedule;
        private HashMap<Course, ArrayList<Course>> mp;
        private int total_rooms;
        private ArrayList<ClassRoom> classrooms;
        private ArrayList<TimeSlot> active_timeslots;
        private HashMap<Course, ClassRoom> roomAssignments;

        public void init(Path classroom, Path cour_path, Path attendance_path, int stepsize) {
                this.classrooms = Importer.importClassRooms(classroom);
                this.courses = Importer.importCourses(cour_path);
                ArrayList<Course> attendance_list = Importer.importAttandenceLists(attendance_path);
                this.total_rooms = this.classrooms.size();
                TimeSlot.setStep_size_t(stepsize);
                this.slotIds = TimeSlot.set_time_slots();
                this.mp = Graph.createGraph(attendance_list);
        }

        public void loadData(ArrayList<Course> courses, ArrayList<ClassRoom> classrooms, int stepsize) {
                this.courses = courses;
                this.classrooms = classrooms;
                this.total_rooms = classrooms.size();
                TimeSlot.setStep_size_t(stepsize);
                this.slotIds = TimeSlot.set_time_slots();
                this.mp = Graph.createGraph(courses);
        }

        public HashMap<Course, Integer> getSchedule() {
                return this.schedule;
        }

        public ArrayList<TimeSlot> getActiveTimeSlots() {
                return this.active_timeslots;
        }

        public HashMap<Course, ClassRoom> getRoomAssignments() {
                return this.roomAssignments;
        }

        private void assignRooms() {
                this.roomAssignments = new HashMap<>();
                HashMap<Integer, ArrayList<Course>> coursesBySlot = new HashMap<>();

                for (var entry : schedule.entrySet()) {
                        int slotId = entry.getValue();
                        coursesBySlot.computeIfAbsent(slotId, k -> new ArrayList<>()).add(entry.getKey());
                }

                ArrayList<ClassRoom> roomsDesc = new ArrayList<>(this.classrooms);
                roomsDesc.sort((r1, r2) -> Integer.compare(r2.getCapacity(), r1.getCapacity()));

                for (int slotId : coursesBySlot.keySet()) {
                        ArrayList<Course> coursesInThisSlot = coursesBySlot.get(slotId);
                        coursesInThisSlot.sort((c1, c2) -> Integer.compare(c2.getEnrolledStudentIDs().size(), c1.getEnrolledStudentIDs().size()));

                        for (int i = 0; i < coursesInThisSlot.size(); i++) {
                                Course c = coursesInThisSlot.get(i);
                                ClassRoom assignedRoom = roomsDesc.get(i);
                                this.roomAssignments.put(c, assignedRoom);
                        }
                }
        }

      // Change the signature to accept initialDays and startDate
public void generate_schedule(int initialDays, LocalDate startDate, boolean skip_weekend) {

    // 1. Sorting logic (Kept same)
    this.courses.sort((c1, c2) -> {
        int deg1 = mp.getOrDefault(c1, new ArrayList<>()).size();
        int deg2 = mp.getOrDefault(c2, new ArrayList<>()).size();
        if (deg1 != deg2) return Integer.compare(deg2, deg1);
        return Integer.compare(c2.getEnrolledStudentIDs().size(), c1.getEnrolledStudentIDs().size());
    });

    // 2. Max Room Capacity Check (Kept same)
    int maxRoomCapacity = this.classrooms.stream().mapToInt(ClassRoom::getCapacity).max().orElse(0);
    for (Course c : this.courses) {
        if (c.getEnrolledStudentIDs().size() > maxRoomCapacity) {
            System.err.println("CRITICAL: Course " + c.getID() + " is too big.");
            return;
        }
    }

    // 3. Set start days from user input instead of calculating logic
    int days = initialDays; 

    boolean solved = false;
    this.schedule = new HashMap<>();

    while (!solved) {

        if (DEBUG) {
            System.out.println("Trying with " + days + " day(s)");
        }

        // 4. Use the passed 'startDate' instead of LocalDate.now()
        ArrayList<TimeSlot> curr = TimeSlot.slot_generator(days, startDate, slotIds, skip_weekend);
        
        this.active_timeslots = curr;
        this.slots = new ArrayList<>();
        for (TimeSlot ts : curr) {
            this.slots.add(ts.getID());
        }

        // Capacity check (logic kept to skip impossible day counts quickly)
        int totalSystemCapacity = this.slots.size() * this.total_rooms;
        if (this.courses.size() > totalSystemCapacity) {
            if (DEBUG) System.out.println("Skipping " + days + " days (Not enough slots)");
            days++;
            continue;
        }

        this.startTime = System.currentTimeMillis();

        try {
            if (solver(0)) {
                solved = true;
                assignRooms();
                if (DEBUG) {
                    System.out.println("Schedule possible in " + days + " day(s).");
                    
                    // 5. Updated Print Logic to include End Time
                    for (var entry : schedule.entrySet()) {
                        Course c = entry.getKey();
                        int slot_id = entry.getValue();
                        TimeSlot rt = curr.get(slot_id);
                        ClassRoom room = roomAssignments.get(c);
                        
                        // Calculate End Time
                        LocalTime start = rt.getTime();
                        LocalTime end = start.plusMinutes(c.getDuration());

                        System.out.println(String.format("%-10s -> %s %s - %s [Room: %s]", 
                            c.getID(), 
                            rt.getDate(), 
                            start, 
                            end, 
                            (room != null ? room.getName() : "N/A")
                        ));
                    }
                }
                return;
            } else {
                schedule.clear();
                days++; // Retry with +1 day
            }
        } catch (RuntimeException e) {
            if (e.getMessage().equals("TIMEOUT")) {
                if (DEBUG)
                    System.out.println(">> Timeout reached (3s). Abandoning " + days + " days. Trying next...");
                schedule.clear();
                days++; // Retry with +1 day on timeout
            } else {
                throw e;
            }
        }
    }
}

        boolean solver(int courseIndex) {

                if (System.currentTimeMillis() - this.startTime > TIMEOUT_MS) {
                        throw new RuntimeException("TIMEOUT");
                }


                if (courseIndex == courses.size()) {
                        return true;
                }

                Course curr = courses.get(courseIndex);

                for (int i = 0; i < slots.size(); i++) {
                        int slot = slots.get(i);

                        if (!checkRoomCapacity(curr, slot)) continue;
                        if (!checkStudentConflicts(curr, slot)) continue;
                        if (!checkMaxStudentsPerDay(curr, slot)) continue;

                        schedule.put(curr, slot);

                        if (solver(courseIndex + 1)) {
                                return true;
                        }

                        schedule.remove(curr);
                }
                return false;
        }


        @Deprecated
        private boolean isGraphSafe(Course current, int proposedSlot) {
                return true;
        }

        private boolean checkStudentConflicts(Course current, int proposedSlotId) {
                ArrayList<Course> neighbors = mp.get(current);
                if (neighbors == null || neighbors.isEmpty()) return true;

                TimeSlot propStart = active_timeslots.get(proposedSlotId);
                int propDur = current.getDuration();

                for (Course neighbor : neighbors) {
                        if (schedule.containsKey(neighbor)) {
                                int neighborSlotId = schedule.get(neighbor);
                                TimeSlot neighborStart = active_timeslots.get(neighborSlotId);
                                int neighborDur = neighbor.getDuration();
                                if (isTimeOverlap(propStart, propDur, neighborStart, neighborDur)) {
                                        return false;
                                }
                        }
                }
                return true;
        }

        boolean checkRoomCapacity(Course course, int slot) {
                ArrayList<Course> coursesInSlot = new ArrayList<>();
                coursesInSlot.add(course);
                TimeSlot proposedTS = active_timeslots.get(slot);
                int proposedDur = course.getDuration();

                for (var entry : schedule.entrySet()) {
                        Course scheduledC = entry.getKey();
                        int scheduledSlot = entry.getValue();
                        TimeSlot scheduledTS = active_timeslots.get(scheduledSlot);
                        int scheduledDur = scheduledC.getDuration();

                        if (isTimeOverlap(proposedTS, proposedDur, scheduledTS, scheduledDur)) {
                                coursesInSlot.add(scheduledC);
                        }
                }

                if (coursesInSlot.size() > this.classrooms.size()) return false;

                coursesInSlot.sort((c1, c2) -> Integer.compare(c2.getEnrolledStudentIDs().size(), c1.getEnrolledStudentIDs().size()));
                ArrayList<ClassRoom> roomsDesc = new ArrayList<>(this.classrooms);
                roomsDesc.sort((r1, r2) -> Integer.compare(r2.getCapacity(), r1.getCapacity()));

                for (int i = 0; i < coursesInSlot.size(); i++) {
                        int students = coursesInSlot.get(i).getEnrolledStudentIDs().size();
                        int capacity = roomsDesc.get(i).getCapacity();
                        if (students > capacity) return false;
                }
                return true;
        }

        boolean checkMaxStudentsPerDay(Course course, int slot) {
                LocalDate targetDate = active_timeslots.get(slot).getDate();
                HashSet<String> studentsInCurrentCourse = course.getEnrolledStudentIDs();

                for (String studentID : studentsInCurrentCourse) {
                        int examsToday = 0;
                        for (var entry : schedule.entrySet()) {
                                Course scheduledCourse = entry.getKey();
                                int scheduledSlotIdx = entry.getValue();
                                LocalDate scheduledDate = active_timeslots.get(scheduledSlotIdx).getDate();

                                if (scheduledDate.equals(targetDate)) {
                                        if (scheduledCourse.getEnrolledStudentIDs().contains(studentID)) {
                                                examsToday++;
                                        }
                                }
                        }
                        if (examsToday >= 2) return false;
                }
                return true;
        }

        private boolean isTimeOverlap(TimeSlot slotA, int durationA, TimeSlot slotB, int durationB) {
                if (!slotA.getDate().equals(slotB.getDate())) return false;
                LocalTime startA = slotA.getTime();
                LocalTime endA = startA.plusMinutes(durationA).plusMinutes(TimeSlot.getStep_size_t());
                LocalTime startB = slotB.getTime();
                LocalTime endB = startB.plusMinutes(durationB).plusMinutes(TimeSlot.getStep_size_t());
                return startA.isBefore(endB) && startB.isBefore(endA);
        }

     public static void main(String[] args) {
    Scheduler scheduler = new Scheduler();

    Path croom = Path.of("docs//sampleData_AllClassroomsAndTheirCapacities.csv");
    Path cour = Path.of("docs//sampleData_AllCoursesWithTime.csv");
    Path att = Path.of("docs//sampleData_AllAttendanceLists.csv");

    scheduler.init(croom, cour, att, 55);

    // --- UPDATED INPUTS ---
    int startDayCount = 5;
    // Set specific start date: Year, Month, Day
    LocalDate startDate = LocalDate.of(2025, 12, 15); 
    boolean skipWeekends = true;

    scheduler.generate_schedule(startDayCount, startDate, skipWeekends);
}
}