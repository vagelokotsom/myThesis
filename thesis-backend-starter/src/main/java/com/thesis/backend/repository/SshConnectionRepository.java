package com.thesis.backend.repository;

import com.thesis.backend.entity.SshConnection;
import com.thesis.backend.entity.User;
import com.thesis.backend.entity.ContainerInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SshConnectionRepository extends JpaRepository<SshConnection, Long> {
    
    List<SshConnection> findByUserAndStatus(User user, String status);
    
    List<SshConnection> findByContainerInstanceAndStatus(ContainerInstance containerInstance, String status);
    
    Optional<SshConnection> findByUserAndContainerInstanceAndStatus(User user, ContainerInstance containerInstance, String status);
    
    @Query("SELECT sc FROM SshConnection sc WHERE sc.sshUsername = :username AND sc.status = 'ACTIVE'")
    Optional<SshConnection> findActiveBySshUsername(@Param("username") String username);
    
    @Query("SELECT sc FROM SshConnection sc WHERE sc.expiresAt < :now")
    List<SshConnection> findExpiredConnections(@Param("now") LocalDateTime now);
    
    @Query("SELECT sc FROM SshConnection sc WHERE sc.user = :user AND sc.status = 'ACTIVE'")
    List<SshConnection> findActiveConnectionsByUser(@Param("user") User user);
    
    @Query("SELECT sc FROM SshConnection sc WHERE sc.containerInstance.kubernetesPodName = :podName AND sc.status = 'ACTIVE'")
    List<SshConnection> findActiveConnectionsByPodName(@Param("podName") String podName);
}
