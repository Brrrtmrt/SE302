package Core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


import static Core.Graph.*;

public class Scheduler {

        //course names Ex:CE 323,EEE 242...
        private ArrayList<Course> courses;

        //1,2,.... Ex:1=Monday 8.30AM ,2=Monday 10.20AM ...
        private ArrayList<Integer> slots;

        //course -> slot schedule[CE 323]=1 means exam will be on Monday 8.30AM.type is int for now may change.
        private HashMap<Course, Integer> schedule;
        private HashMap<Course, ArrayList<Course>> mp = createGraph(courses);

        boolean solver(int courseIndex) {
                //      Base case: EOL
                if (courseIndex == courses.size()) {
                        return true;
                }

                Course curr = courses.get(courseIndex);

                //      Try every possible slot
                for (var slot : slots) {

                        if (!isGraphSafe(curr, slot)) {
                                continue;
                        }

                        //      Check constraints (capacity, std per day)
                        if (!checkRoomCapacity(curr, slot)) continue;
                        if (!checkMaxStudentsPerDay(curr, slot)) continue;

                        //      Assign the slot
                        schedule.put(curr, slot);

                        //      Recurse
                        if (solver(courseIndex + 1)) {
                                return true;
                        }

                        //      Backtrack (Remove assignment to try the next slot)
                        schedule.remove(curr);
                }

                return false; //        No slot possible
        }

        private boolean isGraphSafe(Course current, int proposedSlot) {
                ArrayList<Course> neighbors = mp.get(current);

                //      If no conflicts exist in the graph, it's safe
                if (neighbors == null || neighbors.isEmpty()) return true;

                for (Course neighbor : neighbors) {
                        //      Check scheduled neighbors.
                        if (schedule.containsKey(neighbor)) {
                                int neighborSlot = schedule.get(neighbor);

                                //      Constraint 1: No exact overlap
                                if (proposedSlot == neighborSlot) return false;

                                //      Constraint 2: Gap must be >= 2
                                if (Math.abs(proposedSlot - neighborSlot) < 2) return false;
                        }
                }
                return true;
        }

        HashSet<String> getStudents(String std) {
                return null;
        }

        boolean hasCommonElements(final HashSet<String> set1, final HashSet<String> set2) {
                return false;
        }

        boolean checkRoomCapacity(Course course, int slot) {
                return false;
        }

        boolean checkMaxStudentsPerDay(Course course, int slot) {
                return false;
        }

}
