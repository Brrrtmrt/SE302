package Helpers;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;


public class TimeSlot {
        private int m_id;            // Integer used on solver 0,1,2,.....n
        private LocalDate m_date;    //    2026-01-01
        private LocalTime m_time;    //    09:00

        private static int step_size_t;

        public TimeSlot(int id, LocalDate date, LocalTime time) {
                this.m_id = id;
                this.m_date = date;
                this.m_time = time;
        }

        public static int getStep_size_t() {
                return step_size_t;
        }

        public static void setStep_size_t(int step_size) {
                if (step_size < 0) {
                        System.err.println("Error: Step size must be a positive integer.");
                        TimeSlot.step_size_t = 55;
                        return;
                }
                TimeSlot.step_size_t = step_size;
        }

        @Override
        public String toString() {
                return m_date + " " + m_time;
        }

        public LocalDate getDate() {
                return m_date;
        }

        public LocalTime getTime() {
                return m_time;
        }

        public int getID() {
                return m_id;
        }

        public static ArrayList<LocalTime> set_time_slots() {
                if (step_size_t <= 0) {
                        throw new IllegalArgumentException("step_size_t must be > 0");
                }

                ArrayList<LocalTime> times = new ArrayList<>();

                LocalTime start = LocalTime.of(8, 30);
                LocalTime end = LocalTime.of(19, 00);

                while (!start.isAfter(end)) {
                        times.add(start);
                        start = start.plusMinutes(step_size_t); // Step size (Resolution)
                }
                return times;
        }

        public static ArrayList<TimeSlot> slot_generator(int num_days, LocalDate start_date, ArrayList<LocalTime> time_slots, boolean skip_weekend) {
                ArrayList<TimeSlot> all_slots = new ArrayList<>();
                int slotID = 0;
                int days_added = 0;
                int cal_day_offset = 0;


                while (days_added < num_days) {
                        LocalDate curr_date = start_date.plusDays(cal_day_offset);
                        cal_day_offset++;


                        if (skip_weekend && curr_date.getDayOfWeek().getValue() >= 6) {
                                continue;
                        }

                        for (LocalTime time : time_slots) {
                                all_slots.add(new TimeSlot(slotID, curr_date, time));
                                slotID++;
                        }

                        days_added++;
                }
                return all_slots;
        }
}
