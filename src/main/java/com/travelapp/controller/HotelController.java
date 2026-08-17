package com.travelapp.controller;

import com.travelapp.dto.ApiResponse;
import com.travelapp.dto.hotel.HotelSearchResponse;
import com.travelapp.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    /**
     * Search hotels via Amadeus Hotel List + Hotel Offers APIs.
     *
     * @param cityCode   IATA city code (e.g. "PAR", "LON", "NYC") — required
     * @param checkIn    Check-in date (yyyy-MM-dd) — defaults to 7 days from now
     * @param checkOut   Check-out date (yyyy-MM-dd) — defaults to 10 days from now
     * @param adults     Number of adult guests — defaults to 1
     * @param sortBy     Sort order: price|price_desc|rating|luxury — defaults to price
     */
    @GetMapping("/search")
    public ApiResponse<HotelSearchResponse> search(
            @RequestParam(value = "cityCode", defaultValue = "PAR") String cityCode,
            @RequestParam(value = "checkIn", required = false) String checkIn,
            @RequestParam(value = "checkOut", required = false) String checkOut,
            @RequestParam(value = "adults", defaultValue = "1") Integer adults,
            @RequestParam(value = "sortBy", defaultValue = "price") String sortBy) {

        HotelSearchResponse response = hotelService.searchHotels(cityCode, checkIn, checkOut, adults, sortBy);
        return ApiResponse.ok(response);
    }
}
