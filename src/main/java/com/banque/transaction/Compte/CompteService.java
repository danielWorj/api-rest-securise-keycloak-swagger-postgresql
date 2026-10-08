package com.banque.transaction.Compte;

import com.banque.transaction.Client.Client;
import com.banque.transaction.Client.ClientService;
import com.banque.transaction.Exceptions.ConflictException;
import com.banque.transaction.Exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompteService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CompteRepository compteRepository;
    private final ClientService clientService;

    @Transactional
    public CompteDto create(CompteDto dto) {
        Client client = clientService.getEntity(dto.getClientId());   // 404 si inconnu

        Compte compte = new Compte();
        compte.setNumero(generateNumero());
        compte.setSolde(BigDecimal.ZERO);
        compte.setDateCreation(LocalDateTime.now());
        compte.setClient(client);
        return CompteDto.from(compteRepository.save(compte));
    }

    @Transactional(readOnly = true)
    public List<CompteDto> findAll() {
        return compteRepository.findAll().stream().map(CompteDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CompteDto findById(UUID id) {
        return CompteDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<CompteDto> findByClient(UUID clientId) {
        clientService.getEntity(clientId);                            // 404 si le client n'existe pas
        return compteRepository.findByClientId(clientId).stream().map(CompteDto::from).toList();
    }

    @Transactional
    public void delete(UUID id) {
        Compte compte = getEntity(id);
        if (compte.getSolde().compareTo(BigDecimal.ZERO) != 0) {      // R8
            throw new ConflictException("Impossible de supprimer un compte dont le solde n'est pas nul");
        }
        compteRepository.delete(compte);
        compteRepository.flush();   // refuse la suppression si des dépôts/retraits y sont rattachés
    }

    /** Lecture simple, utilisable par les autres modules. */
    @Transactional(readOnly = true)
    public Compte getEntity(UUID id) {
        return compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + id));
    }

    /**
     * Lecture AVEC verrou (R7). À appeler uniquement depuis une transaction déjà ouverte
     * (dépôt / retrait) : MANDATORY lève une erreur si ce n'est pas le cas.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Compte getEntityForUpdate(UUID id) {
        return compteRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + id));
    }

    private String generateNumero() {
        String numero;
        do {
            numero = "CPT" + String.format("%010d", RANDOM.nextLong(10_000_000_000L));
        } while (compteRepository.existsByNumero(numero));
        return numero;
    }
}