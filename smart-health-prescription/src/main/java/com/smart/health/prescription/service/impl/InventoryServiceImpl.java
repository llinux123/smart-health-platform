package com.smart.health.prescription.service.impl;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.prescription.dto.InboundRequest;
import com.smart.health.prescription.dto.OutboundRequest;
import com.smart.health.prescription.dto.ReconcileRequest;
import com.smart.health.prescription.entity.InventoryLog;
import com.smart.health.prescription.entity.Medicine;
import com.smart.health.prescription.entity.PharmacyInventory;
import com.smart.health.prescription.mapper.InventoryLogMapper;
import com.smart.health.prescription.mapper.MedicineMapper;
import com.smart.health.prescription.mapper.PharmacyInventoryMapper;
import com.smart.health.prescription.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 药房库存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final PharmacyInventoryMapper pharmacyInventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final MedicineMapper medicineMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(InboundRequest request, Long operatorId) {
        PharmacyInventory inv = pharmacyInventoryMapper.selectByPharmacyAndMedicine(
                request.getPharmacyId(), request.getMedicineId());
        if (inv == null) {
            Medicine medicine = medicineMapper.selectById(request.getMedicineId());
            if (medicine == null) {
                throw new BusinessException("药品不存在");
            }
            inv = new PharmacyInventory();
            inv.setPharmacyId(request.getPharmacyId());
            inv.setMedicineId(request.getMedicineId());
            inv.setMedicineName(medicine.getName());
            inv.setStock(request.getQuantity());
            inv.setLockStock(0);
            inv.setUnit(medicine.getUnit());
            pharmacyInventoryMapper.insert(inv);

            InventoryLog logRecord = new InventoryLog();
            logRecord.setPharmacyId(request.getPharmacyId());
            logRecord.setMedicineId(request.getMedicineId());
            logRecord.setChangeType("INBOUND");
            logRecord.setQuantityChange(request.getQuantity());
            logRecord.setStockBefore(0);
            logRecord.setStockAfter(request.getQuantity());
            logRecord.setReason(request.getReason());
            logRecord.setOperatorId(operatorId);
            inventoryLogMapper.insert(logRecord);

            log.info("首次入库成功，pharmacyId={}, medicineId={}, quantity={}",
                    request.getPharmacyId(), request.getMedicineId(), request.getQuantity());
            return;
        }

        int before = inv.getStock();
        int affected = pharmacyInventoryMapper.increaseStock(
                request.getPharmacyId(), request.getMedicineId(), request.getQuantity());
        if (affected != 1) {
            throw new BusinessException("入库失败");
        }

        InventoryLog logRecord = new InventoryLog();
        logRecord.setPharmacyId(request.getPharmacyId());
        logRecord.setMedicineId(request.getMedicineId());
        logRecord.setChangeType("INBOUND");
        logRecord.setQuantityChange(request.getQuantity());
        logRecord.setStockBefore(before);
        logRecord.setStockAfter(before + request.getQuantity());
        logRecord.setReason(request.getReason());
        logRecord.setOperatorId(operatorId);
        inventoryLogMapper.insert(logRecord);

        log.info("入库成功，pharmacyId={}, medicineId={}, quantity={}", 
                request.getPharmacyId(), request.getMedicineId(), request.getQuantity());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outbound(OutboundRequest request, Long operatorId) {
        PharmacyInventory inv = pharmacyInventoryMapper.selectByPharmacyAndMedicine(
                request.getPharmacyId(), request.getMedicineId());
        if (inv == null) {
            throw new BusinessException("该药品在该药房无库存记录");
        }

        int before = inv.getStock();
        int affected = pharmacyInventoryMapper.deductStock(
                request.getPharmacyId(), request.getMedicineId(), request.getQuantity());
        if (affected != 1) {
            throw new BusinessException("库存不足，出库失败");
        }

        InventoryLog logRecord = new InventoryLog();
        logRecord.setPharmacyId(request.getPharmacyId());
        logRecord.setMedicineId(request.getMedicineId());
        logRecord.setChangeType("OUTBOUND");
        logRecord.setQuantityChange(-request.getQuantity());
        logRecord.setStockBefore(before);
        logRecord.setStockAfter(before - request.getQuantity());
        logRecord.setReason(request.getReason());
        logRecord.setOperatorId(operatorId);
        inventoryLogMapper.insert(logRecord);

        log.info("出库成功，pharmacyId={}, medicineId={}, quantity={}",
                request.getPharmacyId(), request.getMedicineId(), request.getQuantity());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcile(ReconcileRequest request, Long operatorId) {
        PharmacyInventory inv = pharmacyInventoryMapper.selectByPharmacyAndMedicine(
                request.getPharmacyId(), request.getMedicineId());
        if (inv == null) {
            throw new BusinessException("该药品在该药房无库存记录");
        }

        int before = inv.getStock();
        int diff = request.getActualStock() - before;

        int affected = pharmacyInventoryMapper.setStock(
                request.getPharmacyId(), request.getMedicineId(), request.getActualStock());
        if (affected != 1) {
            throw new BusinessException("盘点更新失败");
        }

        InventoryLog logRecord = new InventoryLog();
        logRecord.setPharmacyId(request.getPharmacyId());
        logRecord.setMedicineId(request.getMedicineId());
        logRecord.setChangeType("RECONCILE");
        logRecord.setQuantityChange(diff);
        logRecord.setStockBefore(before);
        logRecord.setStockAfter(request.getActualStock());
        logRecord.setReason(request.getReason());
        logRecord.setOperatorId(operatorId);
        inventoryLogMapper.insert(logRecord);

        log.info("盘点成功，pharmacyId={}, medicineId={}, before={}, after={}",
                request.getPharmacyId(), request.getMedicineId(), before, request.getActualStock());
    }

    @Override
    public List<PharmacyInventory> listByPharmacy(Long pharmacyId) {
        return pharmacyInventoryMapper.selectByPharmacyId(pharmacyId);
    }

    @Override
    public List<PharmacyInventory> listAll() {
        return pharmacyInventoryMapper.selectAll();
    }

    @Override
    public List<InventoryLog> listLogs(Long pharmacyId, Long medicineId, int page, int size) {
        int offset = (page - 1) * size;
        return inventoryLogMapper.selectByPage(pharmacyId, medicineId, offset, size);
    }
}
