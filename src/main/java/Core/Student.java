package Core;


public record Student(String ID) {
       
        @Override
        public boolean equals(Object obj) {
                if (obj instanceof Student s) {
                        return this.ID == s.ID;
                }
                return false;
        }

        
    @Override
        public int hashCode() {
                return ID.hashCode();
        }
}

