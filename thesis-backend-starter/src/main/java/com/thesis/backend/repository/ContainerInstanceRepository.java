
package com.thesis.backend.repository;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContainerInstanceRepository extends JpaRepository<ContainerInstance, Long> {
    List<ContainerInstance> findByOwner(User owner);
}
