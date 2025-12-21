# SE302
# Scheduler Module
_________________________________________________
This module is the core of the Exam Planner. It uses a constraint-based probabilistic backtracking algorithm to generate conflict-free exam schedules.


## Features

* **Constraint Checking:**
    * **Student Conflicts:** Ensures no student is scheduled for two exams at the same time.
    * **Room Capacity:** Verifies that assigned classrooms have enough capacity for the enrolled students.
    * **Daily Load Limit:** Enforces a maximum of 2 exams per student per day.
* **Optimization:**
    * Prioritizes _hard_ courses (those with high conflict degrees) to minimize backtracking.
    * Automatically calculates the minimum required days based on course volume and student load.
    * Assigns the largest courses to the largest available rooms to maximize space efficiency.
* **Reliability:**
    * **Timeout Protection:** Includes a timeout mechanism to prevent freezing on impossible schedules(set to 10 seconds).
    * **Random Restart:** Uses randomized restarts based on a geometric distribution to escape local optima during the search process."

## Algorithm Overview

The scheduler uses a **Randomized Backtracking** approach with **Heuristic Sorting**:

1.  **Graph Generation:** A conflict graph is built where nodes are courses and edges represent shared students.
2.  **Sorting:** Courses are sorted by _degree of conflict_ (descending) and then by enrollment size. This ensures the most difficult-to-schedule courses are placed first.
3.  **Optimal Start Calculation:** The system calculates a theoretical minimum number of days required before starting, checking both total volume and per-student daily limits.
4.  **Solving:**
    * The solver attempts to assign a valid time slot to each course recursively.
    * If a valid slot is found that satisfies all constraints (Time, Room, Student Load), it proceeds to the next course.
    * If no slot is found, it backtracks.
5.  **Room Assignment:** Once a valid time schedule is generated, rooms are assigned using a **Best Fit** strategy, matching large classes to large rooms.
