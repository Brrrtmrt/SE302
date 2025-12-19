package Core;

import java.util.HashSet;
import java.util.Objects;

public class Course {
        private final String ID;
        private final int duration ;

        private HashSet<String> enrolledStudentIDs;


        public Course(String ID, int duration) {
                this.ID = ID;
                this.duration = duration;
                this.enrolledStudentIDs = new HashSet<>();
        }

        public void addEnrolledStudentID(String studentID) {
                this.enrolledStudentIDs.add(studentID);
        }

        public String getID() {
                return ID;
        }
        public int getDuration() {
                return duration;
        }
        public final HashSet<String> getEnrolledStudentIDs() {
                return this.enrolledStudentIDs;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Course course = (Course) o;
                return Objects.equals(ID, course.ID) && Objects.equals(enrolledStudentIDs, course.enrolledStudentIDs);
        }

        @Override
        public int hashCode() {
                return Objects.hash(ID, enrolledStudentIDs);
        }

        @Override
        public String toString() {
                return ID;
        }
}
