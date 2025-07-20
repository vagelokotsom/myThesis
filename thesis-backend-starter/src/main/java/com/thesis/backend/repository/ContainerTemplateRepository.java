package com.thesis.backend.repository;

import com.thesis.backend.entity.ContainerTemplate;
import com.thesis.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContainerTemplateRepository extends JpaRepository<ContainerTemplate, Long> {
    
    List<ContainerTemplate> findByCreatedBy(User user);
    
    List<ContainerTemplate> findByIsPublicTrue();
    
    List<ContainerTemplate> findByCategory(String category);
    
    List<ContainerTemplate> findByDifficultyLevel(String difficultyLevel);
    
    @Query("SELECT ct FROM ContainerTemplate ct WHERE ct.isPublic = true OR ct.createdBy = :user")
    List<ContainerTemplate> findAvailableTemplates(@Param("user") User user);
    
    @Query("SELECT ct FROM ContainerTemplate ct WHERE ct.sshEnabled = true AND (ct.isPublic = true OR ct.createdBy = :user)")
    List<ContainerTemplate> findSshEnabledTemplates(@Param("user") User user);
    
    List<ContainerTemplate> findByNameContainingIgnoreCase(String name);
}
