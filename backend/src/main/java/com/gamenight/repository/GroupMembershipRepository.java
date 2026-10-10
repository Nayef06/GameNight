package com.gamenight.repository;

import com.gamenight.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    List<GroupMembership> findByUserIdOrderByGroupNameAsc(Long userId);
    List<GroupMembership> findByGroupIdOrderByUserUsernameAsc(Long groupId);
    boolean existsByUserIdAndGroupId(Long userId, Long groupId);
    long countByGroupId(Long groupId);
    @Modifying
    @Query("delete from GroupMembership membership where membership.user.id = :userId and membership.group.id = :groupId")
    int deleteByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);
}
