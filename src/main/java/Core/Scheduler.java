package Core;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import Helpers.TimeSlot;
import IO.Importer;

import static Tests.Debug.*;


public class Scheduler {

        private final Random random = new Random();
        private int THROW_THRESHOLD;
        private final int MAX_LEVEL_CAP = 20;

        // FAIL FAST CONFIGURATION
        // If 5 random shuffles fail, assume the day count is impossible.
        private final int MAX_RETRIES_PER_DAY = 5;

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


        private int calculateOptimalStartDay(boolean skip_weekend) {
                ArrayList<TimeSlot> oneDaySlots = TimeSlot.slot_generator(1, LocalDate.now(), slotIds, skip_weekend);
                int slotsPerDay = Math.max(1, oneDaySlots.size());

                long totalCourseSlotsNeeded = 0;
                for (Course c : this.courses) {
                        // How many slots does this specific course need?
                        int slotsForCourse = (int) Math.ceil((double) c.getDuration() / TimeSlot.getStep_size_t());
                        totalCourseSlotsNeeded += slotsForCourse;
                }

                long totalCapacityPerDay = (long) this.total_rooms * slotsPerDay;

                int minDaysByVolume = (int) Math.ceil((double) totalCourseSlotsNeeded / totalCapacityPerDay);


                HashMap<String, Integer> studentLoadMap = new HashMap<>();
                int maxExamsForSingleStudent = 0;

                for (Course c : this.courses) {
                        for (String studentID : c.getEnrolledStudentIDs()) {
                                int count = studentLoadMap.getOrDefault(studentID, 0) + 1;
                                studentLoadMap.put(studentID, count);
                                if (count > maxExamsForSingleStudent) {
                                        maxExamsForSingleStudent = count;
                                }
                        }
                }


                int dailyStudentLimit = 2;
                int minDaysByStudent = (int) Math.ceil((double) maxExamsForSingleStudent / dailyStudentLimit);


                int theoreticalMin = Math.max(minDaysByVolume, minDaysByStudent);


                int buffer = (int) Math.ceil(theoreticalMin * 0.7);

                int optimalStart = theoreticalMin + buffer;

                if (DEBUG) {
                        System.out.println("--- OPTIMAL START CALCULATION ---");
                        System.out.println("Slots Per Day: " + slotsPerDay);
                        System.out.println("Total Rooms: " + total_rooms);
                        System.out.println("Bound 1 (Volume): " + minDaysByVolume + " days");
                        System.out.println("Bound 2 (Student Load): " + minDaysByStudent + " days (Max exams: " + maxExamsForSingleStudent + ")");
                        System.out.println("Theoretical Min: " + theoreticalMin);
                        System.out.println("Calculated Start (w/ buffer): " + optimalStart);
                        System.out.println("---------------------------------");
                }

                return Math.max(1, optimalStart);
        }

        private int randomLevel() {
                int lvl = 0;
                while (random.nextBoolean() && lvl < MAX_LEVEL_CAP) {
                        lvl++;
                }
                return lvl;
        }

        public void init(Path classroom, Path cour_path, Path attendance_path, int stepsize) {
                this.classrooms = Importer.importClassRooms(classroom);
                this.courses = Importer.importCourses(cour_path);
                ArrayList<Course> attendance_list = Importer.importAttandenceLists(attendance_path);

                HashMap<String, Course> courseMap = new HashMap<>();
                for (Course c : this.courses) {
                        courseMap.put(c.getID(), c);
                }

                for (Course attCourse : attendance_list) {
                        Course mainCourse = courseMap.get(attCourse.getID());
                        if (mainCourse != null) {
                                for (String studentID : attCourse.getEnrolledStudentIDs()) {
                                        mainCourse.addEnrolledStudentID(studentID);
                                }
                        }
                }

                this.total_rooms = this.classrooms.size();
                TimeSlot.setStep_size_t(stepsize);
                this.slotIds = TimeSlot.set_time_slots();
                this.mp = Graph.createGraph(this.courses);
        }

        public void generate_schedule(int initialDays, LocalDate startDate, boolean skip_weekend) {

                // 1. Sort Courses (Hardest first)
                this.courses.sort((c1, c2) -> {
                        int deg1 = mp.getOrDefault(c1, new ArrayList<>()).size();
                        int deg2 = mp.getOrDefault(c2, new ArrayList<>()).size();
                        if (deg1 != deg2) return Integer.compare(deg2, deg1);
                        return Integer.compare(c2.getEnrolledStudentIDs().size(), c1.getEnrolledStudentIDs().size());
                });


                int n = Math.max(1, this.courses.size());
                int log2Courses = (int) (Math.log(n) / Math.log(2));
                int safetyBuffer = (n > 200) ? 8 : 4;
                this.THROW_THRESHOLD = Math.min(log2Courses + safetyBuffer, MAX_LEVEL_CAP - 1);


                int calculatedStart = calculateOptimalStartDay(skip_weekend);


                int days = Math.max(initialDays, calculatedStart);


                boolean solved = false;
                this.schedule = new HashMap<>();
                int currentDayRetries = 0;
                while (!solved) {


                        if (DEBUG && currentDayRetries == 0) {
                                System.out.println("Checking " + days + " days...");
                        }

                        ArrayList<TimeSlot> curr = TimeSlot.slot_generator(days, startDate, slotIds, skip_weekend);
                        this.active_timeslots = curr;
                        this.slots = new ArrayList<>();
                        for (TimeSlot ts : curr) this.slots.add(ts.getID());

                        // Shuffle slots for randomness
                        Collections.shuffle(this.slots, this.random);

                        // 1. Math Check (Skip impossibly small days instantly)
                        int totalSystemCapacity = this.slots.size() * this.total_rooms;
                        if (this.courses.size() > totalSystemCapacity) {
                                days++;
                                currentDayRetries = 0;
                                continue;
                        }

                        try {
                                if (solver(0)) {
                                        solved = true;
                                        assignRooms();
                                        if (DEBUG) {
                                                System.out.println("SUCCESS: Schedule generated in " + days + " day(s).");
                                                printSchedule(curr);
                                        }
                                        return;
                                } else {
                                        // Natural Failure (Exhausted options)
                                        schedule.clear();
                                        days++;
                                        currentDayRetries = 0;
                                }
                        } catch (RuntimeException e) {
                                if (e.getMessage().equals("RANDOM_RESTART")) {
                                        schedule.clear();
                                        currentDayRetries++;

                                        // Fail Fast Check
                                        if (currentDayRetries >= MAX_RETRIES_PER_DAY) {
                                                if (DEBUG)
                                                        System.out.println(">> Abandoning " + days + " days (Max Retries). Moving to next.");
                                                days++;
                                                currentDayRetries = 0;
                                        }
                                } else {
                                        throw e;
                                }
                        }
                }
        }

        boolean solver(int courseIndex) {

                if (randomLevel() >= THROW_THRESHOLD) {
                        throw new RuntimeException("RANDOM_RESTART");
                }

                if (courseIndex == courses.size()) return true;

                Course curr = courses.get(courseIndex);

                for (int i = 0; i < slots.size(); i++) {
                        int slot = slots.get(i);

                        if (!checkRoomCapacity(curr, slot)) continue;
                        if (!checkStudentConflicts(curr, slot)) continue;
                        if (!checkMaxStudentsPerDay(curr, slot)) continue;

                        schedule.put(curr, slot);

                        if (solver(courseIndex + 1)) return true;

                        schedule.remove(curr);
                }
                return false;
        }

        private boolean isTimeOverlap(TimeSlot slotA, int durationA, TimeSlot slotB, int durationB) {
                if (!slotA.getDate().equals(slotB.getDate())) return false;
                LocalTime startA = slotA.getTime();
                LocalTime endA = startA.plusMinutes(durationA);

                LocalTime startB = slotB.getTime();
                LocalTime endB = startB.plusMinutes(durationB);

                return startA.isBefore(endB) && startB.isBefore(endA);
        }


        private void printSchedule(ArrayList<TimeSlot> curr) {
                for (var entry : schedule.entrySet()) {
                        Course c = entry.getKey();
                        int slot_id = entry.getValue();
                        TimeSlot rt = curr.get(slot_id);
                        ClassRoom room = roomAssignments.get(c);
                        LocalTime start = rt.getTime();
                        LocalTime end = start.plusMinutes(c.getDuration());
                        System.out.println(String.format("%-10s -> %s %s - %s [Room: %s]",
                                c.getID(), rt.getDate(), start, end,
                                (room != null ? room.getName() : "N/A")));
                }
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
                                if (i < roomsDesc.size()) {
                                        this.roomAssignments.put(coursesInThisSlot.get(i), roomsDesc.get(i));
                                }
                        }
                }
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
                                if (isTimeOverlap(propStart, propDur, neighborStart, neighborDur)) return false;
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
                        if (coursesInSlot.get(i).getEnrolledStudentIDs().size() > roomsDesc.get(i).getCapacity())
                                return false;
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
                                        if (scheduledCourse.getEnrolledStudentIDs().contains(studentID)) examsToday++;
                                }
                        }
                        if (examsToday >= 2) return false;
                }
                return true;
        }

        public static void main(String[] args) {
                Scheduler scheduler = new Scheduler();
                Path croom = Path.of("docs\\sampleData_AllClassroomsAndTheirCapacities.csv");
                Path cour = Path.of("docs\\sampleData_AllCoursesWithTime.csv");
                Path att = Path.of("docs\\sampleData_AllAttendanceLists.csv");
                scheduler.init(croom, cour, att, 55);
                scheduler.generate_schedule(5, LocalDate.of(2025, 12, 15), false);
                StudentProgramExtractor extractor = new StudentProgramExtractor(
                scheduler.getSchedule(), 
                scheduler.getActiveTimeSlots()
                );

                // 2. Get exams for a specific student
                List<String> myExams = extractor.getExamsForStudent("Std_ID_001");
                System.out.println("Exams for Std_ID_001: " + myExams);

              
        }
}