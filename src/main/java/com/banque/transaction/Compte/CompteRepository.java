package com.banque.transaction.Compte;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompteRepository extends JpaRepository<Compte, UUID> {

    boolean existsByNumero(String numero);

    List<Compte> findByClientId(UUID clientId);

    /** SELECT ... FOR UPDATE : bloque la ligne tant que la transaction n'est pas terminée (R7). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Compte c where c.id = :id")
    Optional<Compte> findByIdForUpdate(@Param("id") UUID id);
}