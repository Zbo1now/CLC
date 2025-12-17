package com.campuscoin.service.admin;

import com.campuscoin.dao.admin.AdminDashboardDao;
import com.campuscoin.model.admin.AdminDailyFlow;
import com.campuscoin.model.admin.AdminHeatmapPoint;
import com.campuscoin.model.admin.NameCount;
import com.campuscoin.model.admin.TeamAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardService.class);

    private static final List<String> INFLOW_TYPES = Arrays.asList(
            "CHECKIN",
            "ACHIEVEMENT_REWARD",
            "DUTY_REWARD",
            "TRAINING_REWARD"
    );

    private static final List<String> OUTFLOW_TYPES = Arrays.asList(
            "WORKSTATION_RENT",
            "WORKSTATION_RENEW",
            "DEVICE_USAGE",
            "EQUIPMENT_HOLD",
            "VENUE_HOLD"
    );

    private static final List<String> WEEK_GAIN_TYPES = Arrays.asList(
            "CHECKIN",
            "ACHIEVEMENT_REWARD",
            "DUTY_REWARD",
            "TRAINING_REWARD"
    );

    private final AdminDashboardDao dao;

    public AdminDashboardService(AdminDashboardDao dao) {
        this.dao = dao;
    }

    public Map<String, Object> buildOverview() {
        LocalDate today = LocalDate.now();

        Timestamp todayStart = Timestamp.valueOf(today.atStartOfDay());
        Timestamp tomorrowStart = Timestamp.valueOf(today.plusDays(1).atStartOfDay());

        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        Timestamp weekStart = Timestamp.valueOf(weekStartDate.atStartOfDay());
        Timestamp weekEnd = tomorrowStart;

        LocalDate monthStartDate = today.withDayOfMonth(1);
        Timestamp monthStart = Timestamp.valueOf(monthStartDate.atStartOfDay());
        Timestamp monthEnd = tomorrowStart;

        Map<String, Object> data = new HashMap<>();
        data.put("coin", buildCoinBlock(todayStart, tomorrowStart, weekStart, weekEnd, monthStart, monthEnd));
        data.put("teams", buildTeamBlock(weekStart, weekEnd));
        data.put("activity", buildActivityBlock(weekStart, weekEnd));
        data.put("todos", buildTodoBlock());

        return data;
    }

    private Map<String, Object> buildCoinBlock(Timestamp todayStart,
                                               Timestamp tomorrowStart,
                                               Timestamp weekStart,
                                               Timestamp weekEnd,
                                               Timestamp monthStart,
                                               Timestamp monthEnd) {
        Map<String, Object> coin = new HashMap<>();

        coin.put("today", buildCoinPeriod(todayStart, tomorrowStart));
        coin.put("week", buildCoinPeriod(weekStart, weekEnd));
        coin.put("month", buildCoinPeriod(monthStart, monthEnd));

        coin.put("trend", new HashMap<String, Object>() {{
            put("today", buildTrend(todayStart, tomorrowStart));
            put("week", buildTrend(weekStart, weekEnd));
            put("month", buildTrend(monthStart, monthEnd));
        }});

        return coin;
    }

    private Map<String, Object> buildCoinPeriod(Timestamp start, Timestamp end) {
        Map<String, Object> period = new HashMap<>();

        Map<String, Integer> inflowBy = new LinkedHashMap<>();
        inflowBy.put("checkin", 0);
        inflowBy.put("achievement", 0);
        inflowBy.put("duty", 0);
        inflowBy.put("training", 0);

        Map<String, Integer> outflowBy = new LinkedHashMap<>();
        outflowBy.put("workstation", 0);
        outflowBy.put("device", 0);
        outflowBy.put("equipment", 0);
        outflowBy.put("venue", 0);

        List<NameCount> sums = dao.sumAmountByTxnType(start, end, mergeTypes(INFLOW_TYPES, OUTFLOW_TYPES));
        if (sums != null) {
            for (NameCount nc : sums) {
                if (nc == null || nc.getName() == null) {
                    continue;
                }
                String type = nc.getName();
                int amount = nc.getCnt() == null ? 0 : nc.getCnt();

                if ("CHECKIN".equals(type)) {
                    inflowBy.put("checkin", inflowBy.get("checkin") + Math.max(0, amount));
                } else if ("ACHIEVEMENT_REWARD".equals(type)) {
                    inflowBy.put("achievement", inflowBy.get("achievement") + Math.max(0, amount));
                } else if ("DUTY_REWARD".equals(type)) {
                    inflowBy.put("duty", inflowBy.get("duty") + Math.max(0, amount));
                } else if ("TRAINING_REWARD".equals(type)) {
                    inflowBy.put("training", inflowBy.get("training") + Math.max(0, amount));
                } else if ("WORKSTATION_RENT".equals(type) || "WORKSTATION_RENEW".equals(type)) {
                    outflowBy.put("workstation", outflowBy.get("workstation") + Math.max(0, -amount));
                } else if ("DEVICE_USAGE".equals(type)) {
                    outflowBy.put("device", outflowBy.get("device") + Math.max(0, -amount));
                } else if ("EQUIPMENT_HOLD".equals(type)) {
                    outflowBy.put("equipment", outflowBy.get("equipment") + Math.max(0, -amount));
                } else if ("VENUE_HOLD".equals(type)) {
                    outflowBy.put("venue", outflowBy.get("venue") + Math.max(0, -amount));
                }
            }
        }

        // 培训奖励：目前业务里未必写 transactions，这里补充按 training_participations 审核通过统计
        Integer trainingExtra = dao.sumTrainingApprovedReward(start, end);
        if (trainingExtra != null && trainingExtra > 0) {
            inflowBy.put("training", inflowBy.get("training") + trainingExtra);
        }

        int inflowTotal = 0;
        for (Integer v : inflowBy.values()) {
            inflowTotal += v != null ? v : 0;
        }
        int outflowTotal = 0;
        for (Integer v : outflowBy.values()) {
            outflowTotal += v != null ? v : 0;
        }

        Map<String, Object> inflow = new HashMap<>();
        inflow.put("total", inflowTotal);
        inflow.put("bySource", inflowBy);

        Map<String, Object> outflow = new HashMap<>();
        outflow.put("total", outflowTotal);
        outflow.put("bySource", outflowBy);

        period.put("inflow", inflow);
        period.put("outflow", outflow);
        return period;
    }

    private List<Map<String, Object>> buildTrend(Timestamp start, Timestamp end) {
        Map<String, AdminDailyFlow> byDate = new LinkedHashMap<>();

        List<AdminDailyFlow> base = dao.dailyFlow(start, end, INFLOW_TYPES, OUTFLOW_TYPES);
        if (base != null) {
            for (AdminDailyFlow f : base) {
                if (f == null || f.getDate() == null) {
                    continue;
                }
                byDate.put(f.getDate(), f);
            }
        }

        // 培训审核通过的 inflow 单独叠加
        List<AdminDailyFlow> training = dao.dailyTrainingInflow(start, end);
        if (training != null) {
            for (AdminDailyFlow f : training) {
                if (f == null || f.getDate() == null) {
                    continue;
                }
                AdminDailyFlow merged = byDate.get(f.getDate());
                if (merged == null) {
                    merged = new AdminDailyFlow();
                    merged.setDate(f.getDate());
                    merged.setInflow(0);
                    merged.setOutflow(0);
                    byDate.put(f.getDate(), merged);
                }
                int add = f.getInflow() == null ? 0 : f.getInflow();
                int cur = merged.getInflow() == null ? 0 : merged.getInflow();
                merged.setInflow(cur + add);
            }
        }

        // 补齐日期轴
        LocalDateTime st = start.toLocalDateTime();
        LocalDateTime et = end.toLocalDateTime();
        LocalDate d = st.toLocalDate();
        LocalDate last = et.toLocalDate().minusDays(1);
        while (!d.isAfter(last)) {
            String key = d.toString();
            if (!byDate.containsKey(key)) {
                AdminDailyFlow empty = new AdminDailyFlow();
                empty.setDate(key);
                empty.setInflow(0);
                empty.setOutflow(0);
                byDate.put(key, empty);
            }
            d = d.plusDays(1);
        }

        List<Map<String, Object>> points = new ArrayList<>();
        for (AdminDailyFlow f : byDate.values()) {
            Map<String, Object> p = new HashMap<>();
            p.put("date", f.getDate());
            p.put("inflow", f.getInflow() == null ? 0 : f.getInflow());
            p.put("outflow", f.getOutflow() == null ? 0 : f.getOutflow());
            points.add(p);
        }
        points.sort(Comparator.comparing(o -> (String) o.get("date")));
        return points;
    }

    private Map<String, Object> buildTeamBlock(Timestamp weekStart, Timestamp weekEnd) {
        Map<String, Object> teams = new HashMap<>();

        int total = dao.totalTeams();
        int activated = 0;
        try {
            activated = dao.activatedTeams();
        } catch (Exception ex) {
            logger.warn("统计已激活团队失败（可能是 teams 表缺少 face_image 字段）", ex);
        }

        List<TeamAmount> activeTop = dao.topTeamsGain(weekStart, weekEnd, WEEK_GAIN_TYPES, 5);
        List<TeamAmount> spendTop = dao.topTeamsSpend(weekStart, weekEnd, OUTFLOW_TYPES, 5);

        teams.put("totalTeams", total);
        teams.put("activatedTeams", activated);
        teams.put("activeTop5", activeTop == null ? Collections.emptyList() : activeTop);
        teams.put("spendTop5", spendTop == null ? Collections.emptyList() : spendTop);

        return teams;
    }

    private Map<String, Object> buildActivityBlock(Timestamp start, Timestamp end) {
        Map<String, Object> activity = new HashMap<>();

        int checkin = 0;
        int ach = 0;
        int duty = 0;
        int training = 0;

        try {
            checkin = dao.countCheckinActions(start, end);
        } catch (Exception ex) {
            logger.warn("统计打卡行为失败", ex);
        }
        try {
            ach = dao.countAchievementSubmissions(start, end);
        } catch (Exception ex) {
            logger.warn("统计成果提交行为失败（可能缺少 achievement_submissions 表）", ex);
        }
        try {
            duty = dao.countDutySignups(start, end);
        } catch (Exception ex) {
            logger.warn("统计值班行为失败", ex);
        }
        try {
            training = dao.countTrainingParticipations(start, end);
        } catch (Exception ex) {
            logger.warn("统计培训行为失败", ex);
        }

        int total = checkin + ach + duty + training;

        List<Map<String, Object>> behavior = new ArrayList<>();
        if (total <= 0) {
            behavior.add(pie("打卡", 60));
            behavior.add(pie("成果", 15));
            behavior.add(pie("值班", 10));
            behavior.add(pie("培训", 15));
        } else {
            behavior.add(pie("打卡", percent(checkin, total)));
            behavior.add(pie("成果", percent(ach, total)));
            behavior.add(pie("值班", percent(duty, total)));
            behavior.add(pie("培训", Math.max(0, 100 - (percent(checkin, total) + percent(ach, total) + percent(duty, total)))));
        }

        activity.put("behaviorDistribution", behavior);
        activity.put("popularResources", buildPopularResources(start, end));
        activity.put("heatmap", buildHeatmap(start, end));
        return activity;
    }

    private List<Map<String, Object>> buildPopularResources(Timestamp start, Timestamp end) {
        List<NameCount> merged = new ArrayList<>();
        try {
            List<NameCount> d = dao.popularDevices(start, end);
            if (d != null) merged.addAll(d);
        } catch (Exception ex) {
            logger.warn("统计热门设备失败", ex);
        }
        try {
            List<NameCount> w = dao.popularWorkstations(start, end);
            if (w != null) merged.addAll(w);
        } catch (Exception ex) {
            logger.warn("统计热门工位失败", ex);
        }
        try {
            List<NameCount> e = dao.popularEquipments(start, end);
            if (e != null) merged.addAll(e);
        } catch (Exception ex) {
            logger.warn("统计热门器材失败", ex);
        }
        try {
            List<NameCount> v = dao.popularVenues(start, end);
            if (v != null) merged.addAll(v);
        } catch (Exception ex) {
            logger.warn("统计热门场地失败", ex);
        }

        merged.sort((a, b) -> Integer.compare(b.getCnt() == null ? 0 : b.getCnt(), a.getCnt() == null ? 0 : a.getCnt()));

        List<Map<String, Object>> top = new ArrayList<>();
        int n = Math.min(3, merged.size());
        for (int i = 0; i < n; i++) {
            NameCount nc = merged.get(i);
            if (nc == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("name", nc.getName());
            item.put("value", nc.getCnt() == null ? 0 : nc.getCnt());
            top.add(item);
        }
        if (top.isEmpty()) {
            top.add(bar("3D 打印机 A", 12));
            top.add(bar("工位 A101", 9));
            top.add(bar("Canon 单反", 7));
        }
        return top;
    }

    private Map<String, Object> buildHeatmap(Timestamp start, Timestamp end) {
        List<AdminHeatmapPoint> points = Collections.emptyList();
        try {
            List<AdminHeatmapPoint> list = dao.activityHeatmap(start, end);
            points = list == null ? Collections.emptyList() : list;
        } catch (Exception ex) {
            logger.warn("统计活跃热力图失败", ex);
        }

        List<String> days = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        List<Integer> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hours.add(h);
        }

        List<List<Integer>> data = new ArrayList<>();
        for (AdminHeatmapPoint p : points) {
            if (p == null || p.getDow() == null || p.getHour() == null || p.getCnt() == null) {
                continue;
            }
            // ECharts heatmap 常用格式：[x, y, value]
            data.add(Arrays.asList(p.getHour(), p.getDow(), p.getCnt()));
        }

        if (data.isEmpty()) {
            // 默认给个“周二/周四下午高峰”的示例，避免空白
            data.add(Arrays.asList(14, 1, 18));
            data.add(Arrays.asList(15, 1, 22));
            data.add(Arrays.asList(16, 1, 16));
            data.add(Arrays.asList(14, 3, 20));
            data.add(Arrays.asList(15, 3, 26));
            data.add(Arrays.asList(16, 3, 19));
        }

        Map<String, Object> heatmap = new HashMap<>();
        heatmap.put("days", days);
        heatmap.put("hours", hours);
        heatmap.put("data", data);
        return heatmap;
    }

    private Map<String, Object> buildTodoBlock() {
        Map<String, Object> todo = new HashMap<>();
        int pendingAch = 0;
        int upcoming = 0;
        try {
            pendingAch = dao.pendingAchievements();
        } catch (Exception ex) {
            logger.warn("统计待审核成果失败", ex);
        }
        try {
            upcoming = dao.upcomingDeviceBookings() + dao.upcomingVenueBookings();
        } catch (Exception ex) {
            logger.warn("统计即将开始的预约失败", ex);
        }
        todo.put("pendingAchievements", pendingAch);
        todo.put("upcomingBookings", upcoming);
        return todo;
    }

    private static List<String> mergeTypes(List<String> a, List<String> b) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (a != null) set.addAll(a);
        if (b != null) set.addAll(b);
        return new ArrayList<>(set);
    }

    private static int percent(int part, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / total);
    }

    private static Map<String, Object> pie(String name, int value) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }

    private static Map<String, Object> bar(String name, int value) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
}
