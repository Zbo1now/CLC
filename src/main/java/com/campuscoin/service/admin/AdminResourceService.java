package com.campuscoin.service.admin;

import com.campuscoin.dao.*;
import com.campuscoin.model.*;
import com.campuscoin.payload.PagedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一资源管理服务
 */
@Service
public class AdminResourceService {

    private static final Logger logger = LoggerFactory.getLogger(AdminResourceService.class);

    @Autowired
    private WorkstationDao workstationDao;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private EquipmentDao equipmentDao;

    @Autowired
    private VenueDao venueDao;

    /**
     * 获取资源统计数据
     */
    public Map<String, Object> getResourceStats() {
        logger.info("开始获取资源统计数据");
        Map<String, Object> stats = new HashMap<>();
        
        // 工位统计
        Map<String, Integer> workstationStats = new HashMap<>();
        int workstationTotal = workstationDao.countAll();
        logger.info("工位总数: {}", workstationTotal);
        workstationStats.put("total", workstationTotal);
        workstationStats.put("available", workstationDao.countByStatus("AVAILABLE"));
        workstationStats.put("rented", workstationDao.countByStatus("RENTED"));
        workstationStats.put("maintenance", workstationDao.countByStatus("MAINTENANCE"));
        stats.put("workstation", workstationStats);
        
        // 设备统计
        Map<String, Integer> deviceStats = new HashMap<>();
        int deviceTotal = deviceDao.countAll();
        logger.info("设备总数: {}", deviceTotal);
        deviceStats.put("total", deviceTotal);
        deviceStats.put("available", deviceDao.countByStatus("AVAILABLE"));
        deviceStats.put("inUse", deviceDao.countByStatus("IN_USE"));
        deviceStats.put("maintenance", deviceDao.countByStatus("MAINTENANCE"));
        stats.put("device", deviceStats);
        
        // 器材统计
        Map<String, Integer> equipmentStats = new HashMap<>();
        int equipmentTotal = equipmentDao.countAll();
        logger.info("器材总数: {}", equipmentTotal);
        equipmentStats.put("total", equipmentTotal);
        equipmentStats.put("available", equipmentDao.countByStatus("AVAILABLE"));
        equipmentStats.put("borrowed", equipmentDao.countByStatus("BORROWED"));
        equipmentStats.put("maintenance", equipmentDao.countByStatus("MAINTENANCE"));
        stats.put("equipment", equipmentStats);
        
        // 场地统计
        Map<String, Integer> venueStats = new HashMap<>();
        int venueTotal = venueDao.countAll();
        logger.info("场地总数: {}", venueTotal);
        venueStats.put("total", venueTotal);
        venueStats.put("available", venueDao.countByStatus("AVAILABLE"));
        venueStats.put("booked", venueDao.countByStatus("BOOKED"));
        venueStats.put("maintenance", venueDao.countByStatus("MAINTENANCE"));
        stats.put("venue", venueStats);
        
        logger.info("资源统计数据: {}", stats);
        return stats;
    }

    /**
     * 分页查询资源
     */
    public PagedResponse<?> listResources(String type, String status, String keyword, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        
        switch (type.toLowerCase()) {
            case "workstation":
                List<Workstation> workstations = workstationDao.listPaged(status, keyword, offset, pageSize);
                int totalWorkstations = workstationDao.countByCondition(status, keyword);
                PagedResponse<Workstation> workstationResponse = new PagedResponse<>();
                workstationResponse.setItems(workstations);
                workstationResponse.setTotal(totalWorkstations);
                workstationResponse.setPage(page);
                workstationResponse.setPageSize(pageSize);
                return workstationResponse;
            case "device":
                List<Device> devices = deviceDao.listPaged(status, keyword, offset, pageSize);
                int totalDevices = deviceDao.countByCondition(status, keyword);
                PagedResponse<Device> deviceResponse = new PagedResponse<>();
                deviceResponse.setItems(devices);
                deviceResponse.setTotal(totalDevices);
                deviceResponse.setPage(page);
                deviceResponse.setPageSize(pageSize);
                return deviceResponse;
            case "equipment":
                List<Equipment> equipments = equipmentDao.listPaged(status, keyword, offset, pageSize);
                int totalEquipments = equipmentDao.countByCondition(status, keyword);
                PagedResponse<Equipment> equipmentResponse = new PagedResponse<>();
                equipmentResponse.setItems(equipments);
                equipmentResponse.setTotal(totalEquipments);
                equipmentResponse.setPage(page);
                equipmentResponse.setPageSize(pageSize);
                return equipmentResponse;
            case "venue":
                List<Venue> venues = venueDao.listPaged(status, keyword, offset, pageSize);
                int totalVenues = venueDao.countByCondition(status, keyword);
                PagedResponse<Venue> venueResponse = new PagedResponse<>();
                venueResponse.setItems(venues);
                venueResponse.setTotal(totalVenues);
                venueResponse.setPage(page);
                venueResponse.setPageSize(pageSize);
                return venueResponse;
            default:
                throw new IllegalArgumentException("不支持的资源类型: " + type);
        }
    }

    /**
     * 获取资源详情
     */
    public Map<String, Object> getResourceDetail(String type, Integer id) {
        Map<String, Object> result = new HashMap<>();
        
        switch (type.toLowerCase()) {
            case "workstation":
                Workstation workstation = workstationDao.findById(id);
                if (workstation != null) {
                    result.put("id", workstation.getId());
                    result.put("stationCode", workstation.getStationCode());
                    result.put("area", workstation.getArea());
                    result.put("location", workstation.getLocation());
                    result.put("monthlyRent", workstation.getMonthlyRent());
                    result.put("status", workstation.getStatus());
                    result.put("createdAt", workstation.getCreatedAt());
                }
                break;
            case "device":
                Device device = deviceDao.findById(id);
                if (device != null) {
                    result.put("id", device.getId());
                    result.put("deviceName", device.getDeviceName());
                    result.put("model", device.getModel());
                    result.put("deviceType", device.getDeviceType());
                    result.put("location", device.getLocation());
                    result.put("ratePerHour", device.getRatePerHour());
                    result.put("status", device.getStatus());
                    result.put("createdAt", device.getCreatedAt());
                }
                break;
            case "equipment":
                Equipment equipment = equipmentDao.findById(id);
                if (equipment != null) {
                    result.put("id", equipment.getId());
                    result.put("equipmentName", equipment.getEquipmentName());
                    result.put("model", equipment.getModel());
                    result.put("equipmentType", equipment.getEquipmentType());
                    result.put("ratePerDay", equipment.getRatePerDay());
                    result.put("quantity", equipment.getQuantity());
                    result.put("availableQuantity", equipment.getAvailableQuantity());
                    result.put("status", equipment.getStatus());
                    result.put("createdAt", equipment.getCreatedAt());
                }
                break;
            case "venue":
                Venue venue = venueDao.findById(id);
                if (venue != null) {
                    result.put("id", venue.getId());
                    result.put("venueName", venue.getVenueName());
                    result.put("venueType", venue.getVenueType());
                    result.put("capacity", venue.getCapacity());
                    result.put("ratePerHour", venue.getRatePerHour());
                    result.put("status", venue.getStatus());
                    result.put("createdAt", venue.getCreatedAt());
                }
                break;
        }
        
        return result.isEmpty() ? null : result;
    }

    /**
     * 创建资源
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createResource(String type, Map<String, Object> params, String adminUsername) {
        logger.info("创建资源: type={}, admin={}", type, adminUsername);
        
        switch (type.toLowerCase()) {
            case "workstation":
                return workstationDao.create(
                    (String) params.get("stationCode"),
                    (String) params.get("area"),
                    (String) params.get("location"),
                    (Integer) params.get("monthlyRent")
                ) > 0;
            case "device":
                return deviceDao.create(
                    (String) params.get("deviceName"),
                    (String) params.get("model"),
                    (String) params.get("deviceType"),
                    (String) params.get("location"),
                    (Integer) params.get("ratePerHour")
                ) > 0;
            case "equipment":
                return equipmentDao.create(
                    (String) params.get("equipmentName"),
                    (String) params.get("model"),
                    (String) params.get("equipmentType"),
                    (Integer) params.get("ratePerDay"),
                    (Integer) params.get("quantity")
                ) > 0;
            case "venue":
                return venueDao.create(
                    (String) params.get("venueName"),
                    (String) params.get("venueType"),
                    (Integer) params.get("capacity"),
                    (Integer) params.get("ratePerHour")
                ) > 0;
            default:
                return false;
        }
    }

    /**
     * 更新资源
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateResource(String type, Integer id, Map<String, Object> params, String adminUsername) {
        logger.info("更新资源: type={}, id={}, admin={}", type, id, adminUsername);
        
        switch (type.toLowerCase()) {
            case "workstation":
                return workstationDao.update(id, params) > 0;
            case "device":
                return deviceDao.update(id, params) > 0;
            case "equipment":
                return equipmentDao.update(id, params) > 0;
            case "venue":
                return venueDao.update(id, params) > 0;
            default:
                return false;
        }
    }

    /**
     * 删除资源
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteResource(String type, Integer id, String adminUsername) {
        logger.info("删除资源: type={}, id={}, admin={}", type, id, adminUsername);
        
        // 检查是否被占用
        boolean inUse = checkResourceInUse(type, id);
        if (inUse) {
            logger.warn("资源正在使用中，无法删除: type={}, id={}", type, id);
            return false;
        }
        
        switch (type.toLowerCase()) {
            case "workstation":
                return workstationDao.delete(id) > 0;
            case "device":
                return deviceDao.delete(id) > 0;
            case "equipment":
                return equipmentDao.delete(id) > 0;
            case "venue":
                return venueDao.delete(id) > 0;
            default:
                return false;
        }
    }

    /**
     * 更新资源状态
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateResourceStatus(String type, Integer id, String status, String adminUsername) {
        logger.info("更新资源状态: type={}, id={}, status={}, admin={}", type, id, status, adminUsername);
        
        switch (type.toLowerCase()) {
            case "workstation":
                return workstationDao.updateStatus(id, status) > 0;
            case "device":
                return deviceDao.updateStatus(id, status) > 0;
            case "equipment":
                return equipmentDao.updateStatus(id, status) > 0;
            case "venue":
                return venueDao.updateStatus(id, status) > 0;
            default:
                return false;
        }
    }

    /**
     * 获取资源使用历史
     */
    public PagedResponse<?> getResourceHistory(String type, Integer id, Integer page, Integer pageSize) {
        // 这里返回空实现，实际需要查询对应的booking/lease表
        PagedResponse<?> response = new PagedResponse<>();
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotal(0);
        return response;
    }

    /**
     * 检查资源是否正在使用
     */
    private boolean checkResourceInUse(String type, Integer id) {
        // 简化实现，实际需要检查各种booking表
        return false;
    }
}
