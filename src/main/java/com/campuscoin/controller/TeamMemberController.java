package com.campuscoin.controller;

import com.campuscoin.dao.TeamMemberDao;
import com.campuscoin.model.TeamMember;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端：团队成员管理
 */
@RestController
@RequestMapping("/api/teams/me/members")
@Validated
public class TeamMemberController {

    private static final Logger logger = LoggerFactory.getLogger(TeamMemberController.class);

    private final TeamMemberDao teamMemberDao;

    public TeamMemberController(TeamMemberDao teamMemberDao) {
        this.teamMemberDao = teamMemberDao;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        List<TeamMember> list = teamMemberDao.listByTeam(teamId);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeamMember>> add(@RequestBody @Validated CreateMemberRequest req,
                                                       HttpServletRequest request,
                                                       HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setMemberName(req.getMemberName().trim());
        member.setRole((req.getRole() == null || req.getRole().trim().isEmpty()) ? "MEMBER" : req.getRole().trim());
        member.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        member.setStatus("ACTIVE");

        teamMemberDao.insert(member);
        logger.info("新增团队成员: teamId={}, memberId={}, name={}", teamId, member.getId(), member.getMemberName());
        return ResponseEntity.ok(ApiResponse.ok("新增成功", member));
    }

    public static class CreateMemberRequest {
        @NotBlank
        @Size(max = 64)
        private String memberName;
        @Size(max = 32)
        private String role;
        @Size(max = 32)
        private String phone;

        public String getMemberName() {
            return memberName;
        }

        public void setMemberName(String memberName) {
            this.memberName = memberName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}


