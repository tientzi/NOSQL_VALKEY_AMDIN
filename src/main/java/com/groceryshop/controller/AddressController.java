package com.groceryshop.controller;

import com.groceryshop.dto.AddressDTO;
import com.groceryshop.security.UserPrincipal;
import com.groceryshop.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getMyAddresses(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(addressService.getAddressesByUserId(userPrincipal.getId()));
    }

    @PostMapping
    public ResponseEntity<AddressDTO> addAddress(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody AddressDTO dto) {
        return ResponseEntity.ok(addressService.addAddress(userPrincipal.getId(), dto));
    }
}
