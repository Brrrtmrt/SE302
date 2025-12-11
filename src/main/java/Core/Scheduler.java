package Core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


import static Core.Graph.*;

public class Scheduler {

        //course names Ex:CE 323,EEE 242...
        private ArrayList<String> course;

        //1,2,.... Ex:1=Monday 8.30AM ,2=Monday 10.20AM ...
        private ArrayList<Integer> slots;

        //course -> slot schedule[CE 323]=1 means exam will be on Monday 8.30AM.type is int for now may change.
        private HashMap<String, Integer> schedule;
        // private HashMap<String, HashSet<String>> mp = createGraph(ArrayList < Course > c);

        boolean solver(int courseIndex) {
                return false;
        }

        HashSet<String> getStudents(String std) {
                return null;
        }

        boolean hasCommonElements(final HashSet<String> set1, final HashSet<String> set2) {
                return false;
        }

        boolean checkRoomCapacity(String course, int slot) {
                return false;
        }

        boolean checkMaxStudentsPerDay(String course, int slot) {
                return false;
        }

}
