package com.groceryshop.service;

import com.groceryshop.dto.AddressDTO;
import com.groceryshop.entity.Address;
import com.groceryshop.entity.User;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.AddressRepository;
import com.groceryshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AddressDTO> getAddressesByUserId(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(EntityMapper::toAddressDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressDTO addAddress(Long userId, AddressDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        List<Address> existing = addressRepository.findByUserId(userId);
        
        boolean isDefault = existing.isEmpty() || (dto.getIsDefault() != null && dto.getIsDefault());

        if (isDefault) {
            existing.forEach(addr -> {
                if (addr.getIsDefault()) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            });
        }

        Address address = EntityMapper.toAddressEntity(dto);
        address.setUser(user);
        address.setIsDefault(isDefault);

        Address saved = addressRepository.save(address);
        return EntityMapper.toAddressDTO(saved);
    }
}
