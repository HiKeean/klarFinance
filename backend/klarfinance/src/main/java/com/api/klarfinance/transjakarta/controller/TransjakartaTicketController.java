package com.api.klarfinance.transjakarta.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.NasabahAnnotation;
import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.transjakarta.dto.request.PurchaseTicketRequest;
import com.api.klarfinance.transjakarta.dto.response.TicketResponse;
import com.api.klarfinance.transjakarta.service.TransjakartaTicketService;

import java.security.Principal;
import java.util.List;

@NasabahAnnotation
@RequestMapping("/transjakarta/tickets")
@RequiredArgsConstructor
public class TransjakartaTicketController {
    private final TransjakartaTicketService service;

    @PostMapping
    public ResponseEntity<ApiResponse<List<TicketResponse>>> purchase(
            @RequestBody PurchaseTicketRequest request, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Tickets purchased successfully", service.purchase(request, principal)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponse>>> myTickets(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully", service.myTickets(principal)));
    }

    @PostMapping("/{ticketCode}/toggle-used")
    public ResponseEntity<ApiResponse<TicketResponse>> toggleUsed(
            @PathVariable String ticketCode, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Ticket updated successfully", service.toggleUsed(ticketCode, principal)));
    }
}
