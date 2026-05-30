package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusmart.dto.AddressDTO;
import com.campusmart.entity.Address;
import com.campusmart.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService extends ServiceImpl<AddressMapper, Address> {
    
    public List<Address> getAddressList(Long userId) {
        return this.lambdaQuery()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreateTime)
                .list();
    }
    
    public Address getDefaultAddress(Long userId) {
        return this.lambdaQuery()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .one();
    }
    
    @Transactional
    public Address addAddress(Long userId, AddressDTO addressDTO) {
        Address address = new Address();
        address.setUserId(userId);
        address.setReceiverName(addressDTO.getReceiverName());
        address.setPhone(addressDTO.getPhone());
        address.setProvince(addressDTO.getProvince());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setDetailAddress(addressDTO.getDetailAddress());
        
        // 如果设置为默认地址，先取消其他默认
        if (Boolean.TRUE.equals(addressDTO.getIsDefault())) {
            this.lambdaUpdate()
                    .eq(Address::getUserId, userId)
                    .set(Address::getIsDefault, 0)
                    .update();
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }
        
        this.save(address);
        return address;
    }
    
    @Transactional
    public Address updateAddress(Long addressId, Long userId, AddressDTO addressDTO) {
        Address address = this.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在");
        }
        
        address.setReceiverName(addressDTO.getReceiverName());
        address.setPhone(addressDTO.getPhone());
        address.setProvince(addressDTO.getProvince());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setDetailAddress(addressDTO.getDetailAddress());
        
        // 如果设置为默认地址，先取消其他默认
        if (Boolean.TRUE.equals(addressDTO.getIsDefault())) {
            this.lambdaUpdate()
                    .eq(Address::getUserId, userId)
                    .set(Address::getIsDefault, 0)
                    .update();
            address.setIsDefault(1);
        }
        
        this.updateById(address);
        return address;
    }
    
    @Transactional
    public boolean deleteAddress(Long addressId, Long userId) {
        Address address = this.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在");
        }
        return this.removeById(addressId);
    }
    
    @Transactional
    public Address setDefaultAddress(Long addressId, Long userId) {
        Address address = this.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在");
        }
        
        // 取消其他默认
        this.lambdaUpdate()
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0)
                .update();
        
        // 设置当前为默认
        address.setIsDefault(1);
        this.updateById(address);
        return address;
    }
}
