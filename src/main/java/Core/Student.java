package Core;

public record Student(int ID, String name) {

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

