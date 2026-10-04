package com.gamenight.repository;

import com.gamenight.model.GroupGameSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupGameSelectionRepository extends JpaRepository<GroupGameSelection, Long> {
    List<GroupGameSelection> findTop3ByGroupIdOrderBySelectedAtDescIdDesc(Long groupId);
    List<GroupGameSelection> findTop5ByGroupIdOrderBySelectedAtDescIdDesc(Long groupId);
}
