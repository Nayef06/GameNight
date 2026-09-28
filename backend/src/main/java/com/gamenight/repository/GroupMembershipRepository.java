package com.gamenight.repository;

import com.gamenight.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    List<GroupMembership> findByUserIdOrderByGroupNameAsc(Long userId);
    List<GroupMembership> findByGroupIdOrderByUserUsernameAsc(Long groupId);
    boolean existsByUserIdAndGroupId(Long userId, Long groupId);
    long countByGroupId(Long groupId);
    long deleteByUserIdAndGroupId(Long userId, Long groupId);
}
