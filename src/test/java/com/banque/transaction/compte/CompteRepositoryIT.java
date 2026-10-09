package com.banque.transaction.compte;

import com.banque.transaction.Client.Client;
import com.banque.transaction.Client.ClientRepository;
import com.banque.transaction.Compte.Compte;
import com.banque.transaction.Compte.CompteRepository;
import com.banque.transaction.support.Containers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
// pas de base embarquée, on garde le vrai postgres
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CompteRepositoryIT {

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", Containers.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", Containers.POSTGRES::getUsername);
        registry.add("spring.datasource.password", Containers.POSTGRES::getPassword);
    }

    @Autowired ClientRepository clientRepository;
    @Autowired CompteRepository compteRepository;

    @Test
    void existsByIdAndClientId_distingueLesProprietaires() {
        Client proprietaire = nouveauClient();
        Client autre = nouveauClient();
        Compte compte = compteRepository.save(nouveauCompte(proprietaire, "CPT0000000001"));

        assertThat(compteRepository.existsByIdAndClientId(compte.getId(), proprietaire.getId())).isTrue();
        assertThat(compteRepository.existsByIdAndClientId(compte.getId(), autre.getId())).isFalse();
    }

    @Test
    void numeroDeCompteUniqueEnBase() {
        Client client = nouveauClient();
        compteRepository.saveAndFlush(nouveauCompte(client, "CPT0000000002"));

        // même numéro une 2e fois -> la contrainte unique doit sauter
        assertThatThrownBy(() -> compteRepository.saveAndFlush(nouveauCompte(client, "CPT0000000002")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByIdForUpdate_retrouveLeCompte() {
        Compte compte = compteRepository.save(nouveauCompte(nouveauClient(), "CPT0000000003"));

        assertThat(compteRepository.findByIdForUpdate(compte.getId())).isPresent();
    }

    // helpers

    private Client nouveauClient() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setNom("Test");
        return clientRepository.save(client);
    }

    private Compte nouveauCompte(Client client, String numero) {
        Compte compte = new Compte();
        compte.setNumero(numero);
        compte.setSolde(BigDecimal.ZERO);
        compte.setDateCreation(LocalDateTime.now());
        compte.setClient(client);
        return compte;
    }
}
