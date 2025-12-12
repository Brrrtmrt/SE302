package Core;

public class Student {
        private int ID;
        private String name;

        public Student(int ID, String name) {
                this.ID = ID;
                this.name = name;
        }

        public int getID() {
                return ID;
        }

        public void setID(int ID) {
                this.ID = ID;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
                if (obj instanceof Student s) {
                        return this.ID == s.ID;
                }
                return false;
        }

        @Override
        public int hashCode() {
                return Integer.hashCode(ID);
        }
}

