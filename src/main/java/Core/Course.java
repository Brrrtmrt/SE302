package Core;

import java.util.HashSet;
import java.util.Objects;

class Course {
        private final String ID;

        private HashSet<String> enrolledStudentIDs;

        public Course(String ID) {
                this.ID = ID;
                this.enrolledStudentIDs = new HashSet<>();
        }

        public void addEnrolledStudentID(String studentID) {
                this.enrolledStudentIDs.add(studentID);
        }

        public String getID() {
                return ID;
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
