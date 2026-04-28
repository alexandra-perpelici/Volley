package com.example.volley_reservations.controller;

import com.example.volley_reservations.dto.ReservationRequest;
import com.example.volley_reservations.security.CustomUserDetails;
import com.example.volley_reservations.service.BookingPricing;
import com.example.volley_reservations.service.ReservationService;
import com.example.volley_reservations.service.TemporaryReservationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/field")
public class FieldController {

    private final ReservationService reservationService;
    private final TemporaryReservationService tempService;

    public FieldController(ReservationService reservationService, TemporaryReservationService tempService) {
        this.reservationService = reservationService;
        this.tempService = tempService;
    }



    @GetMapping("/{fieldNumber}")
    public String showField(@PathVariable int fieldNumber, Model model, Authentication authentication)
    {
        if (!isSupportedCourt(fieldNumber)) {
            return "redirect:/home";
        }

        populateModel(model, fieldNumber, authentication);
        if (fieldNumber == 1)
            return "field1";
        if(fieldNumber == 2)
            return "field2";
        return "/";
    }

    @GetMapping("/{fieldNumber}/cart")
    public String goToCart(@PathVariable int fieldNumber) {
        if (!isSupportedCourt(fieldNumber)) {
            return "redirect:/home";
        }

        return "redirect:/cart/view?fieldNumber=" + fieldNumber;
    }

    @GetMapping("/{fieldNumber}/matrix")
    @ResponseBody
    public Map<String, Boolean> getReservationMatrix(@PathVariable int fieldNumber) {
        if (!isSupportedCourt(fieldNumber)) {
            return Collections.emptyMap();
        }

        Map<String, Boolean> reservationMatrix = new HashMap<>();
        LocalDate today = LocalDate.now();
        List<LocalDate> nextDays = createScheduleDays(today);
        List<String> timeSlots = createTimeSlots();
        reservationMatrix.putAll(buildReservationMatrix(fieldNumber, nextDays, timeSlots));

        return reservationMatrix;
    }



    @PostMapping("/{fieldNumber}")
    public String reserveField(@ModelAttribute ReservationRequest request,
                                @PathVariable int fieldNumber,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("message", "Trebuie sa fii autentificat pentru a face o rezervare.");
            return "redirect:/login";
        }

        if (!isSupportedCourt(fieldNumber)) {
            redirectAttributes.addFlashAttribute("message", "Teren invalid.");
            return "redirect:/home";
        }

        request.setUser_id(currentUserId(authentication));
        request.setField_number(fieldNumber);

        String resultMessage = reservationService.createNewReservation(request);
        redirectAttributes.addFlashAttribute("message", resultMessage);

        return "redirect:/field/"+fieldNumber; // refresh the page
    }



//    private void populateModel(Model model, int fieldNumber) {
//        // days
//        LocalDate today = LocalDate.now();
//        List<LocalDate> nextDays = new ArrayList<>();
//        for (int i = 0; i < 7; i++) {
//            nextDays.add(today.plusDays(i));
//        }
//        // time slots
//        List<String> timeSlots = new ArrayList<>();
//        LocalTime startTime = LocalTime.of(8, 0);
//        LocalTime endTime = LocalTime.of(20, 0);
//        while (startTime.isBefore(endTime)) {
//            LocalTime nextTime = startTime.plusMinutes(60);
//            timeSlots.add(startTime + " - " + nextTime);
//            startTime = nextTime;
//        }
//
//        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//
//        Map<String, Boolean> reservationMatrix = new HashMap<>();
//
//        for (LocalDate day : nextDays) {
//            String dayString = day.format(dateFormatter);
//            for (String timeSlot : timeSlots) {
//                String key = dayString + " " + timeSlot;
//                boolean reserved = fieldNumber == 1
//                        ? reservationService.isReservedField1(key)
//                        : reservationService.isReservedField2(key);
//                reservationMatrix.put(key, reserved);
//            }
//        }
//
//
//        // the thymeleaf template will look for the attributes timeSlots, days and reservationMatrix
//        model.addAttribute("timeSlots", timeSlots);
//        model.addAttribute("days", nextDays);
//        model.addAttribute("reservation_matrix", reservationMatrix);
//        model.addAttribute("fieldNumber",fieldNumber);
//
//    }


    private void populateModel(Model model, int fieldNumber, Authentication authentication) {
        LocalDate today = LocalDate.now();
        List<LocalDate> nextDays = createScheduleDays(today);
        List<String> timeSlots = createTimeSlots();
        Map<String, Boolean> reservationMatrix = buildReservationMatrix(fieldNumber, nextDays, timeSlots);
        List<com.example.volley_reservations.model.TemporaryReservation> cart = findCurrentCart(authentication);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        Set<String> selectedSlotKeys = cart.stream()
                .map(reservation -> reservation.getDate().format(dateFormatter)
                        + " " + reservation.getTime()
                        + " " + reservation.getFieldNumber())
                .collect(Collectors.toSet());

        model.addAttribute("timeSlots", timeSlots);
        model.addAttribute("timeSlotGroups", createTimeSlotGroups(timeSlots));
        model.addAttribute("days", nextDays);
        model.addAttribute("dayLabels", createDayLabels(nextDays));
        model.addAttribute("dayDateLabels", createDayDateLabels(nextDays));
        model.addAttribute("reservation_matrix", reservationMatrix);
        model.addAttribute("fieldNumber", fieldNumber);
        model.addAttribute("cart", cart);
        model.addAttribute("selected_slot_keys", selectedSlotKeys);
        model.addAttribute("selectedCount", cart.size());
        model.addAttribute("selectedTotal", BookingPricing.totalPriceRon(cart));
        model.addAttribute("slotPrice", BookingPricing.defaultSlotPriceRon());
        model.addAttribute("pricePerHour", BookingPricing.PRICE_PER_HOUR_RON);
    }

    private List<LocalDate> createScheduleDays(LocalDate startDate) {
        List<LocalDate> nextDays = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            nextDays.add(startDate.plusDays(i));
        }
        return nextDays;
    }

    private List<String> createTimeSlots() {
        List<String> timeSlots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(20, 0);
        while (startTime.isBefore(endTime)) {
            LocalTime nextTime = startTime.plusMinutes(60);
            timeSlots.add(startTime + " - " + nextTime);
            startTime = nextTime;
        }
        return timeSlots;
    }

    private List<TimeSlotGroup> createTimeSlotGroups(List<String> timeSlots) {
        List<String> morning = new ArrayList<>();
        List<String> afternoon = new ArrayList<>();
        List<String> evening = new ArrayList<>();

        for (String timeSlot : timeSlots) {
            int startHour = Integer.parseInt(timeSlot.substring(0, 2));
            if (startHour < 12) {
                morning.add(timeSlot);
            } else if (startHour < 18) {
                afternoon.add(timeSlot);
            } else {
                evening.add(timeSlot);
            }
        }

        return List.of(
                new TimeSlotGroup("Dimineata", "08:00 - 12:00", morning),
                new TimeSlotGroup("Dupa-amiaza", "12:00 - 18:00", afternoon),
                new TimeSlotGroup("Seara", "18:00 - 20:00", evening)
        );
    }

    private Map<String, String> createDayLabels(List<LocalDate> days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("ro-RO"));
        DateTimeFormatter keyFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, String> labels = new HashMap<>();
        for (int i = 0; i < days.size(); i++) {
            labels.put(days.get(i).format(keyFormatter), i == 0 ? "Azi" : capitalize(formatter.format(days.get(i))));
        }
        return labels;
    }

    private Map<String, String> createDayDateLabels(List<LocalDate> days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("ro-RO"));
        DateTimeFormatter keyFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        return days.stream().collect(Collectors.toMap(day -> day.format(keyFormatter), day -> formatter.format(day).replace(".", "")));
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private List<com.example.volley_reservations.model.TemporaryReservation> findCurrentCart(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }

        return tempService.getUserReservations(currentUserId(authentication));
    }

    private Integer currentUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        throw new IllegalStateException("Authenticated user details unavailable");
    }

    private Map<String, Boolean> buildReservationMatrix(int fieldNumber, List<LocalDate> days, List<String> timeSlots) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate startDate = days.getFirst();
        LocalDate endDate = days.getLast();
        Set<String> persistedReservations = reservationService.findReservedSlotKeys(fieldNumber, startDate, endDate);
        Set<String> temporaryReservations = tempService.findTemporaryReservedSlotKeys(fieldNumber, startDate, endDate);
        Map<String, Boolean> reservationMatrix = new HashMap<>();

        for (LocalDate day : days) {
            String dayString = day.format(dateFormatter);
            for (String timeSlot : timeSlots) {
                String key = dayString + " " + timeSlot;
                reservationMatrix.put(key, persistedReservations.contains(key) || temporaryReservations.contains(key));
            }
        }

        return reservationMatrix;
    }

    private boolean isSupportedCourt(int fieldNumber) {
        return fieldNumber == 1 || fieldNumber == 2;
    }

    public record TimeSlotGroup(String label, String window, List<String> slots) {
    }

}
