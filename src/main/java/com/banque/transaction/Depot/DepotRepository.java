package com.banque.transaction.Depot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepotRepository extends JpaRepository<Depot, UUID> {   // ← UUID, pas Integer

    List<Depot> findByCompteIdOrderByDateDesc(UUID compteId);

    List<Depot> findByClientIdOrderByDateDesc(UUID clientId);
}