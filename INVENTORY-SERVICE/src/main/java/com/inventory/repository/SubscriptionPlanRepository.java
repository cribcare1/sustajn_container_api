package com.inventory.repository;

import java.util.List;
import java.util.Optional;

import com.inventory.dto.SubscriptionPlanSummary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.inventory.entity.SubscriptionPlan;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {

	// Declare repository methods again so we can attach an EntityGraph to avoid n+1 when
	// there are associations. Currently no associations exist, but this prevents regressions
	// if relationships are added later.
	@Override
	@EntityGraph(attributePaths = {})
	List<SubscriptionPlan> findAll();

	@Override
	@EntityGraph(attributePaths = {})
	Optional<SubscriptionPlan> findById(Integer id);

	// JPQL constructor expression to project minimal fields into SubscriptionPlanSummary DTO
	@Query("SELECT new com.inventory.dto.SubscriptionPlanSummary(s.planId, s.planName, s.planStatus, s.totalContainers, s.billingCycle) FROM SubscriptionPlan s WHERE (:status IS NULL OR s.planStatus = :status)")
	java.util.List<SubscriptionPlanSummary> findSummariesByStatus(@Param("status") SubscriptionPlan.PlanStatus status);

}
