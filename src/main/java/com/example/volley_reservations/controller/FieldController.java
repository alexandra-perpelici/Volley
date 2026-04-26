package com.example.volley_reservations.controller;

import com.example.volley_reservations.dto.ReservationRequest;
import com.example.volley_reservations.model.User;
import com.example.volley_reservations.repository.UserRepository;
import com.example.volley_reservations.service.ReservationService;
import com.example.volley_reservations.service.TemporaryReservationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/field")
public class FieldController {

    private final ReservationService reservationService;
    private final TemporaryReservationService tempService;
    private UserRepository userRepository;

    public FieldController(ReservationService reservationService,UserRepository userRepository,TemporaryReservationService tempService) {
        this.reservationService = reservationService;
        this.userRepository = userRepository;
        this.tempService = tempService;
    }



    @GetMapping("/{fieldNumber}")
    public String showField(@PathVariable int fieldNumber, Model model)
    {
        populateModel(model, fieldNumber);
        reservationService.deleteOldReservations();
        if (fieldNumber == 1)
            return "field1";
        if(fieldNumber == 2)
            return "field2";
        return "/";
    }

    @GetMapping("/{fieldNumber}/cart")
    public String goToCart(@PathVariable int fieldNumber) {
        return "redirect:/cart/view?fieldNumber=" + fieldNumber;
    }

    @GetMapping("/{fieldNumber}/matrix")
    @ResponseBody
    public Map<String, Boolean> getReservationMatrix(@PathVariable int fieldNumber) {
        Map<String, Boolean> reservationMatrix = new HashMap<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        // same time slots logic as populateModel
        List<String> timeSlots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(20, 0);
        while (startTime.isBefore(endTime)) {
            LocalTime nextTime = startTime.plusMinutes(90);
            timeSlots.add(startTime + " - " + startTime.plusMinutes(90));
            startTime = startTime.plusMinutes(90);
        }

        for (int i = 0; i < 7; i++) {
            LocalDate day = today.plusDays(i);
            String dayString = day.format(dateFormatter);
            for (String timeSlot : timeSlots) {
                String key = dayString + " " + timeSlot;
                boolean reserved = fieldNumber == 1
                        ? reservationService.isReservedField1(key)
                        : reservationService.isReservedField2(key);

                // check temp reservations
                if (!reserved && tempService.isTemporarilyReserved(day, timeSlot, fieldNumber)) {
                    reserved = true;
                }

                reservationMatrix.put(key, reserved);
            }
        }

        return reservationMatrix;
    }



    @PostMapping("/{fieldNumber}")
    public String reserveField(@ModelAttribute ReservationRequest request,
                                @PathVariable int fieldNumber,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("message", "You must be logged in to make a reservation!");
            return "redirect:/login";
        }

        // 1. Get username from authentication
        String username = authentication.getName();

        // 2. Fetch user from DB
        Optional<User> dbUser = userRepository.findByUsername(username);
        if (dbUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "User not found!");
            return "redirect:/login";
        }

        // 3. Use the real user_id
        request.setUser_id(dbUser.get().getUser_id());

        // 4. Create reservation
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
//            LocalTime nextTime = startTime.plusMinutes(90);
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


    private void populateModel(Model model, int fieldNumber) {
        LocalDate today = LocalDate.now();
        List<LocalDate> nextDays = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            nextDays.add(today.plusDays(i));
        }

        List<String> timeSlots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(20, 0);
        while (startTime.isBefore(endTime)) {
            LocalTime nextTime = startTime.plusMinutes(90);
            timeSlots.add(startTime + " - " + nextTime);
            startTime = nextTime;
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        Map<String, Boolean> reservationMatrix = new HashMap<>();

        for (LocalDate day : nextDays) {
            String dayString = day.format(dateFormatter);
            for (String timeSlot : timeSlots) {
                String key = dayString + " " + timeSlot;
                boolean reserved = fieldNumber == 1
                        ? reservationService.isReservedField1(key)
                        : reservationService.isReservedField2(key);

                // Check temporary reservations
                if (!reserved && tempService.isTemporarilyReserved(day, timeSlot, fieldNumber)) {
                    reserved = true;
                }

                reservationMatrix.put(key, reserved);
            }
        }

        model.addAttribute("timeSlots", timeSlots);
        model.addAttribute("days", nextDays);
        model.addAttribute("reservation_matrix", reservationMatrix);
        model.addAttribute("fieldNumber", fieldNumber);
    }

}
