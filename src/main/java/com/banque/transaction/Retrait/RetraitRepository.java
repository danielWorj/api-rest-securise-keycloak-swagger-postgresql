package com.banque.transaction.Retrait;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RetraitRepository extends JpaRepository<Retrait, UUID> {

    List<Retrait> findByCompteIdOrderByDateDesc(UUID compteId);

    List<Retrait> findByClientIdOrderByDateDesc(UUID clientId);

    boolean existsByIdAndClientId(UUID id, UUID clientId);
}