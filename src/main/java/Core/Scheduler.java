package Core;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import Helpers.TimeSlot;
import IO.Importer;

import static Tests.Debug.*;

/**
 * Scheduler is responsible for generating an exam schedule given rooms, courses,
 * and attendance lists. It performs constraint checks (student conflicts,
 * room capacities, maximum exams per day) and uses a randomized backtracking
 * solver with occasional randomized restarts.
 *
 * <p>The primary entry points for external use are:
 * <ul>
 *     <li>`loadData` — supply preloaded lists of courses and classrooms</li>
 *     <li>`generate_schedule` — attempt to build a valid schedule starting at a date</li>
 * </ul>
 *
 * <p>Internal algorithm details:
 * <ul>
 *     <li>Courses are sorted by degree (conflicts) and enrollment size to schedule hardest first.</li>
 *     <li>A randomized solver assigns courses to shuffled time slots with backtracking.</li>
 *     <li>If the solver becomes "stuck", a randomized restart is thrown and retried up to a per-day limit.</li>
 * </ul>
 */
public class Scheduler {

        private static final Random random = new Random();

        /**
         * Threshold for random level at which the solver will trigger a random restart.
         * Calculated at runtime and bounded by {@link #MAX_LEVEL_CAP}.
         */
        private int THROW_THRESHOLD;
        /**
         * Upper bound for random level generator to avoid infinite loops.
         */

        private final int MAX_LEVEL_CAP = 20;

        /**
         * Maximum number of randomized restart retries per tested number of days
         * before the algorithm increases the days and continues.
         */
        private final int MAX_RETRIES_PER_DAY = 5;

        public Scheduler() {
        }

        /**
         * List of courses to schedule.
         */
        private ArrayList<Course> courses;

        /**
         * List of permitted start times identifiers produced by TimeSlot.
         */
        private ArrayList<LocalTime> slotIds;

        /**
         * Flattened list of time slot indices currently considered (shuffled).
         */
        private ArrayList<Integer> slots;

        /**
         * Resulting mapping from Course to assigned slot index.
         */
        private HashMap<Course, Integer> schedule;

        /**
         * Conflict graph representation mapping a Course to neighboring Courses
         * that share students and therefore cannot overlap.
         */
        private HashMap<Course, ArrayList<Course>> mp;

        /**
         * Total number of available rooms.
         */
        private int total_rooms;

        /**
         * List of classrooms available for assignment.
         */
        private ArrayList<ClassRoom> classrooms;

        /**
         * Active timeslots generated for the current scheduling attempt.
         */
        private ArrayList<TimeSlot> active_timeslots;

        /**
         * Final mapping of courses to assigned classrooms (after successful scheduling).
         */
        private HashMap<Course, ClassRoom> roomAssignments;


        /**
         * Calculates the optimal number of start days required for scheduling.
         *
         * @param skip_weekend Whether weekends should be skipped in the schedule.
         * @return The optimal number of start days.
         */
        private int calculateOptimalStartDay(boolean skip_weekend) {
                ArrayList<TimeSlot> oneDaySlots = TimeSlot.slot_generator(1, LocalDate.now(), slotIds, skip_weekend);
                int slotsPerDay = Math.max(1, oneDaySlots.size());

                long totalCourseSlotsNeeded = 0;
                for (Course c : this.courses) {
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

        /**
         * Generates a random level for the solver algorithm.
         *
         * @return random level in range [0, {@link #MAX_LEVEL_CAP}]
         */
        private int randomLevel() {
                int lvl = 0;
                while (random.nextBoolean() && lvl < MAX_LEVEL_CAP) {
                        lvl++;
                }
                return lvl;
        }

        /**
         * Initializes the scheduler with classroom, course, and attendance data.
         * Actual method for this is on GUI part.
         *
         * @param classroom       Path to the file containing classroom data.
         * @param cour_path       Path to the file containing course data.
         * @param attendance_path Path to the file containing attendance data.
         * @param stepsize        The step size for time slots.
         */
        private void init(Path classroom, Path cour_path, Path attendance_path, int stepsize) {
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

        /**
         * Generates a schedule for the courses, ensuring all constraints are met.
         *
         * @param initialDays  The initial number of days to consider for scheduling.(If too low, it will automatically set to valid number of days)
         * @param startDate    The start date for the schedule.
         * @param skip_weekend Whether weekends should be skipped in the schedule.
         */
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

                        if (days > initialDays + 30) {
                                throw new SchedulingException("Schedule unfeasible", "The algorithm could not find a valid solution within a reasonable time.\n" +
                                        "This usually happens due to:\n" +
                                        "1. Too many conflicts for the available rooms.\n" +
                                        "2. Exams duration exceeding day limits.");
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

        /**
         * Recursively attempts to assign slots to courses using backtracking.
         * <p>
         * Test slots on each call.
         * If randomLevel() exceeds THROW_THRESHOLD,to restart randomly
         * RuntimeException("RANDOM_RESTART") is thrown.
         *
         * @param courseIndex index of the course to assign next
         * @return true if remaining courses were successfully scheduled, false otherwise
         */
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

        /**
         * Determines whether two time intervals overlap on the same date.
         *
         * @param slotA     first TimeSlot
         * @param durationA duration of the first course in minutes
         * @param slotB     second TimeSlot
         * @param durationB duration of the second course in minutes
         * @return true if the two intervals overlap
         */
        private boolean isTimeOverlap(TimeSlot slotA, int durationA, TimeSlot slotB, int durationB) {
                if (!slotA.getDate().equals(slotB.getDate())) return false;
                LocalTime startA = slotA.getTime();
                LocalTime endA = startA.plusMinutes(durationA);

                LocalTime startB = slotB.getTime();
                LocalTime endB = startB.plusMinutes(durationB);

                return startA.isBefore(endB) && startB.isBefore(endA);
        }

        /**
         * Print formatted schedule to stdout for debugging.
         *
         * @param curr list of active TimeSlot instances matching slot indices used in {@code schedule}
         */
        private void printSchedule(ArrayList<TimeSlot> curr) {
                for (var entry : schedule.entrySet()) {
                        Course c = entry.getKey();
                        int slot_id = entry.getValue();
                        TimeSlot rt = curr.get(slot_id);
                        ClassRoom room = roomAssignments.get(c);
                        LocalTime start = rt.getTime();
                        LocalTime end = start.plusMinutes(c.getDuration());
                        System.out.printf("%-10s -> %s %s - %s [Room: %s]%n",
                                c.getID(), rt.getDate(), start, end,
                                (room != null ? room.getName() : "N/A"));
                }
        }

        /**
         * Load data directly from collections instead of files.
         * For GUI code.
         *
         * @param courses    list of courses to schedule
         * @param classrooms list of available classrooms
         * @param stepsize   time step size in minutes
         */
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

        /**
         * Assign classrooms to scheduled courses for each slot, matching largest courses
         * to the largest rooms to maximize feasibility.
         */
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

        /**
         * Check if placing {@code current} into {@code slot} violates student-overlap constraints
         * using the course conflict graph {@link #mp}.
         *
         * @param current        course to test
         * @param proposedSlotId slot index to test
         * @return true if no conflicts detected
         */
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

        /**
         * Check whether adding {@code course} into {@code slot} will exceed available room
         * capacity either by count or by room size.
         *
         * @param course course to test
         * @param slot   slot index to test
         * @return true if capacity constraints are satisfied
         */
        private boolean checkRoomCapacity(Course course, int slot) {
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

        /**
         * Ensure that no student in {@code course} will have more than 2 exams on the
         * target date if this course is placed in {@code slot}.
         *
         * @param course course to test
         * @param slot   slot index to test
         * @return true if the per-day student exam limit is not exceeded
         */
        private boolean checkMaxStudentsPerDay(Course course, int slot) {
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
}