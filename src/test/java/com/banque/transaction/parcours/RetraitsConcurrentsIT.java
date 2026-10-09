package com.banque.transaction.parcours;

import com.banque.transaction.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class RetraitsConcurrentsIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("R7 : 5 retraits simultanés de 30 sur un solde de 100 -> 3 succès, 2 refus, solde = 10")
    void retraitsSimultanes() throws Exception {
        creerClient(CLIENT_ID, "Client Un");
        UUID compte = creerCompte(CLIENT_ID);
        deposer(CLIENT_ID, compte, "100.00");

        rawToken(CLIENT); // token récupéré avant de lancer les threads

        int nbRetraits = 5;
        ExecutorService pool = Executors.newFixedThreadPool(nbRetraits);
        CountDownLatch depart = new CountDownLatch(1);
        List<Future<Integer>> resultats = new ArrayList<>();

        for (int i = 0; i < nbRetraits; i++) {
            resultats.add(pool.submit(() -> {
                depart.await();   // tout le monde attend le top départ
                return retirer(CLIENT, compte, "30.00").andReturn().getResponse().getStatus();
            }));
        }
        depart.countDown();

        int succes = 0;
        int refus = 0;
        for (Future<Integer> resultat : resultats) {
            int statut = resultat.get();
            if (statut == 201) succes++;
            else if (statut == 422) refus++;
        }
        pool.shutdown();

        assertThat(succes).isEqualTo(3);
        assertThat(refus).isEqualTo(2);
        assertThat(solde(compte)).isEqualByComparingTo("10.00");
        assertThat(retraitRepository.count()).isEqualTo(3);
    }
}
