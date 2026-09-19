package com.cdc.agent.repository;

import com.cdc.agent.entity.AppClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppClientRepository extends JpaRepository<AppClientEntity, Long> {

    Optional<AppClientEntity> findByAppId(String appId);

    boolean existsByAppId(String appId);

    void deleteByAppId(String appId);
}
