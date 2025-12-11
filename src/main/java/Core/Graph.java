package Core;

import java.util.*;


class Graph {

        /*
         * *************************************************************************************************************************
         * Changelog:                                                                                                              *
         *      11.12.2025      Although specified on our design document using Set for O(1) lookups upon conducting a research,   *
         *      I realised using ArrayList would be better.                                                                        *
         *      Rationale:      1)   Spatial Locality   2)  Overhead of Set duplicate check is actually higher than iterating List.*
         * End Changelog                                                                                                           *
         * *************************************************************************************************************************
         *
         * */


        /**
         * Rationale for adjacency list as design choice
         * Let K be number of conflicts on graph and Let N be amount of nodes in graph, graph will most likely be sparse
         * so no need to check non-conflicting courses. (K << N)
         * <p>
         * <p>
         * <p>
         * Generates an adjacency list
         * (HashMap< Course, ArrayList < Course>>) where nodes are
         * courses and edges represent conflict (e.g. Cannot be
         * scheduled at the same time)
         *
         * @param courses Course List
         * @return Adjacency list representation of data.
         */
        public static HashMap<Course, ArrayList<Course>> createGraph(ArrayList<Course> courses) {

                if (courses == null) {
                        return null;
                }

                HashMap<Course, ArrayList<Course>> adjList = new HashMap<>();

                for (Course c : courses) {
                        adjList.put(c, new ArrayList<>());
                }

                for (int i = 0; i < courses.size(); i++) {
                        for (int j = i + 1; j < courses.size(); j++) {

                                Course c1 = courses.get(i);
                                Course c2 = courses.get(j);

                                //      This replaces hasCommonElements
                                if (!Collections.disjoint(c1.getEnrolledStudentIDs(), c2.getEnrolledStudentIDs())) {
                                        adjList.get(c1).add(c2);
                                        adjList.get(c2).add(c1);
                                }
                        }
                }

                return adjList;
        }

        public static void main(String[] args) {
                //      Test
                Course c1 = new Course("SE 323");
                c1.addEnrolledStudentID("Mert Saadet");
                c1.addEnrolledStudentID("Mert Utma");

                Course c2 = new Course("CE 323");
                c2.addEnrolledStudentID("Mert Saadet"); //      Bu bir conflict, iki node birbirine bağlanmalı
                c2.addEnrolledStudentID("Efe Mehmed");

                Course c3 = new Course("GBE 251");
                c3.addEnrolledStudentID("NA"); //       Conflict yok bağlantı olmamalı.

                ArrayList<Course> allCourses = new ArrayList<>(List.of(c1, c2, c3));
                HashMap<Course, ArrayList<Course>> graph = createGraph(allCourses);

                //      Çizgeyi bastır
                for (var x : graph.entrySet()) {
                        System.out.println(x.getKey() + " conflicts with: " + x.getValue());
                }
        }


}
