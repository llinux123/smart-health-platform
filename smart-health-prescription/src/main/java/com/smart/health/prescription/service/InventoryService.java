package com.smart.health.prescription.service;

import com.smart.health.prescription.dto.InboundRequest;
import com.smart.health.prescription.dto.OutboundRequest;
import com.smart.health.prescription.dto.ReconcileRequest;
import com.smart.health.prescription.entity.InventoryLog;
import com.smart.health.prescription.entity.PharmacyInventory;

import java.util.List;

/**
 * 药房库存服务接口
 */
public interface InventoryService {

    /**
     * 入库操作
     */
    void inbound(InboundRequest request, Long operatorId);

    /**
     * 出库操作
     */
    void outbound(OutboundRequest request, Long operatorId);

    /**
     * 盘点操作
     */
    void reconcile(ReconcileRequest request, Long operatorId);

    /**
     * 查询某个药房的所有库存
     */
    List<PharmacyInventory> listByPharmacy(Long pharmacyId);

    /**
     * 查询库存变动日志
     */
    List<InventoryLog> listLogs(Long pharmacyId, Long medicineId, int page, int size);
}
