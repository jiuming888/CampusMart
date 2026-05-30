package com.campusmart.controller;

import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.dto.AddressDTO;
import com.campusmart.entity.Address;
import com.campusmart.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
public class AddressController {
    
    @Autowired
    private AddressService addressService;
    
    @GetMapping("/list")
    public Result<List<Address>> getAddressList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        return Result.success(addressService.getAddressList(userId));
    }
    
    @GetMapping("/default")
    public Result<?> getDefaultAddress(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        return Result.success(addressService.getDefaultAddress(userId));
    }
    
    @PostMapping("/add")
    public Result<?> addAddress(HttpServletRequest request, 
                                @Valid @RequestBody AddressDTO addressDTO) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        return Result.success(addressService.addAddress(userId, addressDTO));
    }
    
    @PutMapping("/update/{id}")
    public Result<?> updateAddress(HttpServletRequest request,
                                  @PathVariable Long id,
                                  @Valid @RequestBody AddressDTO addressDTO) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            return Result.success(addressService.updateAddress(id, userId, addressDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteAddress(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            boolean success = addressService.deleteAddress(id, userId);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/setDefault/{id}")
    public Result<?> setDefaultAddress(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            return Result.success(addressService.setDefaultAddress(id, userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
