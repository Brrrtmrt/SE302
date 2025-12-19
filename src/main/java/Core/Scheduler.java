package Core;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import Helpers.TimeSlot;
import IO.Importer;


public class Scheduler {

        Scheduler() {
        }

        //      Most of these is useless

        //course names Ex:CE 323,EEE 242...
        private ArrayList<Course> courses;

        private ArrayList<Course> attendance_list;

        //      Time Slots
        private ArrayList<LocalTime> slotIds;

        //      Integer representation of slots
        private ArrayList<Integer> slots;

        //course -> slot schedule[CE 323]=1 means exam will be on Monday 8.30AM.type is int for now may change.
        private HashMap<Course, Integer> schedule;

        //      Graph
        private HashMap<Course, ArrayList<Course>> mp;
        private int total_rooms;

        private int[] exams_per_slot;

        private ArrayList<ClassRoom> classrooms;

        private ArrayList<TimeSlot> active_timeslots;

        public void init(Path classroom, Path cour_path, Path attendance_path) {
                this.courses = Importer.importCourses(cour_path);
                this.attendance_list = Importer.importAttandenceLists(attendance_path);
                this.slotIds = TimeSlot.set_time_slots();
                //      slots are init on generate_schedule
                //      schedule is created on generate_schedule
                this.mp = Graph.createGraph(attendance_list);

                this.classrooms = Importer.importClassRooms(classroom);
                this.total_rooms = this.classrooms.size();
        }

        public void generate_schedule(boolean skip_weekend) {
                this.courses.sort((c1, c2) -> Integer.compare(c2.getEnrolledStudentIDs().size(), c1.getEnrolledStudentIDs().size()));


                int maxRoomCapacity = 0;
                if (this.classrooms != null && !this.classrooms.isEmpty()) {
                        maxRoomCapacity = this.classrooms.stream()
                                .mapToInt(ClassRoom::getCapacity)
                                .max()
                                .orElse(0);
                } else {
                        System.err.println("Error: No classrooms initialized. Cannot schedule.");
                        return;
                }


                for (Course c : this.courses) {
                        int enrolled = c.getEnrolledStudentIDs().size();
                        if (enrolled > maxRoomCapacity) {
                                System.err.println("CRITICAL ERROR: Course " + c.getID() +
                                        " has " + enrolled + " students, but the largest room only holds " + maxRoomCapacity + ".");
                                System.err.println("Stopping to prevent infinite loop.");
                                return;
                        }
                }
                int days = 1;
                boolean solved = false;
                this.schedule = new HashMap<>();


                while (!solved) {

                        System.out.println("Trying with " + days + " day(s)");

                        ArrayList<TimeSlot> curr = TimeSlot.slot_generator(days, LocalDate.now(), slotIds, skip_weekend);

                        //      cp
                        this.active_timeslots = curr;


                        //      Holds real DateTime
                        this.slots = new ArrayList<>();
                        for (TimeSlot ts : curr) {
                                this.slots.add(ts.getID());
                        }
                        this.exams_per_slot = new int[slots.size()];

                        if (solver(0)) {
                                solved = true;
                                System.out.println("Schedule possible in " + days + " day(s).");

                                for (var entry : schedule.entrySet()) {
                                        int slot_id = entry.getValue();
                                        TimeSlot rt = curr.get(slot_id);
                                        //      day will not change, no exam on (e.g.23.00) can change later
                                        LocalTime start = rt.getTime();
                                        LocalTime end = start.plusMinutes(100);
                                        System.out.println(entry.getKey().getID() + " -> " + rt + " " + "ends at: " + end);
                                }
                                return;
                        } else {
                                schedule.clear();
                                days++;
                        }
                }


        }

        boolean solver(int courseIndex) {
                //      Base case: EOL
                if (courseIndex == courses.size()) {
                        return true;
                }

                Course curr = courses.get(courseIndex);

                //      Try every possible slot i because maybe it may throw ArrayIndexOutOfBoundsException if ID happens to be is something large      (somehow)
                for (int i = 0; i < slots.size(); i++) {

                        int slot = slots.get(i);

                        if (!isGraphSafe(curr, slot)) {
                                continue;
                        }
                        if (exams_per_slot[i] >= total_rooms) {
                                continue;
                        }

                        //      Check constraints (capacity, std per day)

                        //      Commented out because infinite loop(not implemented yet)->Tolga

                        if (!checkRoomCapacity(curr, slot)) continue;
                        if (!checkMaxStudentsPerDay(curr, slot)) continue;

                        //      Assign the slot
                        schedule.put(curr, slot);
                        exams_per_slot[i]++;

                        //      Recurse
                        if (solver(courseIndex + 1)) {
                                return true;
                        }

                        //      Backtrack (Remove assignment to try the next slot)
                        schedule.remove(curr);
                        exams_per_slot[i]--;
                }


                return false; //        No slot possible

        }

        private boolean isGraphSafe(Course current, int proposedSlot) {
                ArrayList<Course> neighbors = mp.get(current);

                //      If no conflicts exist in the graph, it's safe
                if (neighbors == null || neighbors.isEmpty()) return true;

                //      Now check for time too
                TimeSlot propTS = active_timeslots.get(proposedSlot);

                for (Course neighbor : neighbors) {
                        //      Check scheduled neighbors.
                        if (schedule.containsKey(neighbor)) {
                                int neighborSlot = schedule.get(neighbor);
                                TimeSlot neighborTS = active_timeslots.get(neighborSlot);

                                //      Constraint 1: No exact overlap
                                if (proposedSlot == neighborSlot) return false;

                                //      If they are not on same day then it is valid
                                if (propTS.getDate().equals(neighborTS.getDate())) {
                                        if (Math.abs(proposedSlot - neighborSlot) < 2) {
                                                return false;
                                        }
                                }
                        }
                }
                return true;
        }

        boolean checkRoomCapacity(Course course, int slot) {

                ArrayList<Course> coursesInSlot = new ArrayList<>();
                coursesInSlot.add(course);

                for (var entry : schedule.entrySet()) {
                        if (entry.getValue() == slot) {
                                coursesInSlot.add(entry.getKey());
                        }
                }

                if (coursesInSlot.size() > this.classrooms.size()) {
                        return false;
                }

                //      Sort everything Descending (Biggest course -> Biggest room)
                //      Sort courses by # of students
                coursesInSlot.sort((c1, c2) -> Integer.compare(c2.getEnrolledStudentIDs().size(), c1.getEnrolledStudentIDs().size()));

                //      Sort classrooms by capacity
                ArrayList<ClassRoom> roomsDesc = new ArrayList<>(this.classrooms);
                roomsDesc.sort((r1, r2) -> Integer.compare(r2.getCapacity(), r1.getCapacity()));

                //      Verify that every course fits in its assigned room
                //      (Biggest course goes to biggest room, 2nd biggest to 2nd biggest, etc.)
                for (int i = 0; i < coursesInSlot.size(); i++) {
                        int students = coursesInSlot.get(i).getEnrolledStudentIDs().size();
                        int capacity = roomsDesc.get(i).getCapacity();

                        if (students > capacity) {
                                return false; // Fits in time, but not in space
                        }
                }

                return true;
        }

        boolean checkMaxStudentsPerDay(Course course, int slot) {
                // get the date of the slot
                LocalDate targetDate = active_timeslots.get(slot).getDate();

                // get the list of students in the current course
                HashSet<String> studentsInCurrentCourse = course.getEnrolledStudentIDs();

                // check each student's daily load
                for (String studentID : studentsInCurrentCourse) {
                        int examsToday = 0;

                        // look at all courses already scheduled
                        for (var entry : schedule.entrySet()) {
                                Course scheduledCourse = entry.getKey();
                                int scheduledSlotIdx = entry.getValue();
                                LocalDate scheduledDate = active_timeslots.get(scheduledSlotIdx).getDate();

                                // if the scheduled course is on the same day
                                if (scheduledDate.equals(targetDate)) {
                                        // check if this student is in the scheduled course
                                        if (scheduledCourse.getEnrolledStudentIDs().contains(studentID)) {
                                                examsToday++;
                                        }
                                }
                        }

                        // if this student already has 2 exams on this date, we cannot add another
                        if (examsToday >= 2) {
                                return false;
                        }
                }

                return true; // all students have 0 or 1 exams so far today
        }

        public static void main(String[] args) {

                Scheduler scheduler = new Scheduler();
                //      These should work without absolute paths FIX IT.
                Path croom = Path.of("docs\\sampleData_AllClassroomsAndTheirCapacities.csv");
                Path cour = Path.of("docs\\sampleData_AllCourses.csv");
                Path att = Path.of("docs\\sampleData_AllAttendanceLists.csv");
                scheduler.init(croom, cour, att);
                scheduler.generate_schedule(false);

                /*      This is tested, output may look abnormal
                        it is valid (not checking the constraints
                        bounded by subprograms)
                *
                *
                * */

        }
}
