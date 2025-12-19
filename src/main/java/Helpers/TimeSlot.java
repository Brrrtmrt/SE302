package Helpers;

import Core.Course;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TimeSlot {
        private int m_id;            // Integer used on solver 0,1,2,.....n
        private LocalDate m_date;    //    2026-01-01
        private LocalTime m_time;    //    09:00

        public TimeSlot(int id, LocalDate date, LocalTime time) {
                this.m_id = id;
                this.m_date = date;
                this.m_time = time;
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

        /**
         * Public API enter slots here
         * TODO 1:Get input then create time slots
         *
         * @return slots that will be used in scheduler
         *
         */
        public static ArrayList<LocalTime> set_time_slots() {

                return new ArrayList<LocalTime>(List.of(
                        LocalTime.of(8, 30),
                        LocalTime.of(9,25),
                        LocalTime.of(10, 20),
                        LocalTime.of(11, 15),
                        LocalTime.of(12, 10),
                        LocalTime.of(13, 5),
                        LocalTime.of(14, 0),
                        LocalTime.of(14, 55),
                        LocalTime.of(15, 50),
                        LocalTime.of(16, 45),
                        LocalTime.of(17, 40),
                        LocalTime.of(18, 35),
                        LocalTime.of(19, 30)
                ));
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

        public static void main(String[] args) {
                final ArrayList<LocalTime> time_slots = set_time_slots();
                final ArrayList<TimeSlot> slots = slot_generator(2, LocalDate.now().plusDays(5), time_slots, true); //  Oki doki skip_weekend seems fine
                for (final var x : slots) {
                        System.out.println(x);
                }
        }
}
