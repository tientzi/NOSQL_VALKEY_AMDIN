package com.groceryshop.controller;

import com.groceryshop.dto.ReportOverviewDTO;
import com.groceryshop.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/overview")
    public ResponseEntity<ReportOverviewDTO> getOverviewReport(
            @org.springframework.web.bind.annotation.RequestParam(value = "range", defaultValue = "30days") String range,
            @org.springframework.web.bind.annotation.RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @org.springframework.web.bind.annotation.RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getOverviewReport(range, startDate, endDate));
    }

    @GetMapping("/expired-batches")
    public ResponseEntity<java.util.Map<String, Object>> getExpiredBatchesReport() {
        return ResponseEntity.ok(reportService.getExpiredBatchesReport());
    }
}
