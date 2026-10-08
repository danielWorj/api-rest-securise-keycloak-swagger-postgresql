package com.banque.transaction.Depot;

import com.banque.transaction.Client.Client;
import com.banque.transaction.Client.ClientService;
import com.banque.transaction.Compte.Compte;
import com.banque.transaction.Compte.CompteService;
import com.banque.transaction.Exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepotService {

    private final DepotRepository depotRepository;
    private final ClientService clientService;
    private final CompteService compteService;

    /**
     * Tout est dans UNE transaction : soit le dépôt est enregistré ET le solde mis à jour,
     * soit rien ne change.
     */
    @Transactional
    public DepotDto create(DepotDto dto) {
        Client client = clientService.getEntity(dto.getClientId());               // 404 si inconnu
        Compte compte = compteService.getEntityForUpdate(dto.getCompteId());      // 404 + verrou

        compte.setSolde(compte.getSolde().add(dto.getMontant()));                 // crédit

        Depot depot = new Depot();
        depot.setClient(client);
        depot.setCompte(compte);
        depot.setMontant(dto.getMontant());
        depot.setSecretaire(dto.getSecretaire());
        depot.setDate(LocalDateTime.now());                                       // jamais fournie par l'appelant

        return DepotDto.from(depotRepository.save(depot));
        // le compte étant "managed", son nouveau solde est enregistré au commit
    }

    @Transactional(readOnly = true)
    public List<DepotDto> findAll() {
        return depotRepository.findAll().stream().map(DepotDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DepotDto findById(UUID id) {
        return depotRepository.findById(id)
                .map(DepotDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Dépôt introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<DepotDto> findByCompte(UUID compteId) {
        compteService.getEntity(compteId);                                        // 404 si le compte n'existe pas
        return depotRepository.findByCompteIdOrderByDateDesc(compteId).stream().map(DepotDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DepotDto> findByClient(UUID clientId) {
        clientService.getEntity(clientId);
        return depotRepository.findByClientIdOrderByDateDesc(clientId).stream().map(DepotDto::from).toList();
    }

    // Volontairement : PAS de update() ni de delete() (R6).
}