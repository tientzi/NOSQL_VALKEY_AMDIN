package com.groceryshop.service;

import com.groceryshop.entity.Inventory;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    public List<Inventory> getLowStockWarnings() {
        return inventoryRepository.findLowStockWarnings();
    }

    /**
     * Cập nhật thông tin phụ trợ (vị trí kệ, ngưỡng tồn tối thiểu).
     * Tuyệt đối KHÔNG cho phép sửa trực tiếp currentStock tại đây.
     */
    @Transactional
    public Inventory updateInventoryInfo(Long id, String location, Integer minimumStock) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin kho hàng với id: " + id));

        if (location != null) {
            inventory.setLocation(location.trim());
        }
        if (minimumStock != null) {
            if (minimumStock < 0) {
                throw new BadRequestException("Ngưỡng tồn kho tối thiểu không được âm");
            }
            inventory.setMinimumStock(minimumStock);
        }
        return inventoryRepository.save(inventory);
    }

    /**
     * Khóa thao tác cộng dồn trực tiếp. Bắt buộc tạo Phiếu nhập kho.
     */
    public Inventory addStock(Long id, Integer quantity, String location) {
        throw new BadRequestException("Thao tác bị khóa! Tồn kho chỉ được gia tăng tự động thông qua Phiếu Nhập Kho (Goods Receipt) để đảm bảo tính minh bạch kế toán.");
    }

    /**
     * Giữ lại updateStock cho tương thích nhưng khóa thay đổi currentStock
     */
    @Transactional
    public Inventory updateStock(Long id, Integer currentStock, String location) {
        return updateInventoryInfo(id, location, null);
    }
}
