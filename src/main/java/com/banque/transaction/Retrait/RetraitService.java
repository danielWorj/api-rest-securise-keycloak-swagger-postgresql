package com.banque.transaction.Retrait;

import com.banque.transaction.Client.Client;
import com.banque.transaction.Client.ClientService;
import com.banque.transaction.Compte.Compte;
import com.banque.transaction.Compte.CompteService;
import com.banque.transaction.Exceptions.OperationNonAutoriseeException;
import com.banque.transaction.Exceptions.ResourceNotFoundException;
import com.banque.transaction.Exceptions.SoldeInsuffisantException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RetraitService {

    private final RetraitRepository retraitRepository;
    private final ClientService clientService;
    private final CompteService compteService;

    @Transactional
    public RetraitDto create(RetraitDto dto) {
        Client client = clientService.getEntity(dto.getClientId());               // 404 si inconnu
        Compte compte = compteService.getEntityForUpdate(dto.getCompteId());      // 404 + verrou

        // R4 : le compte doit appartenir au client
        if (!compte.getClient().getId().equals(client.getId())) {
            throw new OperationNonAutoriseeException("Un client ne peut retirer que depuis son propre compte");
        }

        // R5 : le solde ne peut pas devenir négatif
        if (compte.getSolde().compareTo(dto.getMontant()) < 0) {
            throw new SoldeInsuffisantException(
                    "Solde insuffisant : solde actuel " + compte.getSolde()
                            + ", montant demandé " + dto.getMontant());
        }

        compte.setSolde(compte.getSolde().subtract(dto.getMontant()));            // débit

        Retrait retrait = new Retrait();
        retrait.setClient(client);
        retrait.setCompte(compte);
        retrait.setMontant(dto.getMontant());
        retrait.setDate(LocalDateTime.now());

        return RetraitDto.from(retraitRepository.save(retrait));
    }

    @Transactional(readOnly = true)
    public List<RetraitDto> findAll() {
        return retraitRepository.findAll().stream().map(RetraitDto::from).toList();
    }

    @Transactional(readOnly = true)
    public RetraitDto findById(UUID id) {
        return retraitRepository.findById(id)
                .map(RetraitDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Retrait introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<RetraitDto> findByCompte(UUID compteId) {
        compteService.getEntity(compteId);
        return retraitRepository.findByCompteIdOrderByDateDesc(compteId).stream().map(RetraitDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RetraitDto> findByClient(UUID clientId) {
        clientService.getEntity(clientId);
        return retraitRepository.findByClientIdOrderByDateDesc(clientId).stream().map(RetraitDto::from).toList();
    }

    // Volontairement : PAS de update() ni de delete() (R6).
}