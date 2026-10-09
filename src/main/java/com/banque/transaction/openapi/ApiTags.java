package com.banque.transaction.openapi;

/**
 * Noms et descriptions des tags Swagger, centralisés pour être réutilisés
 * dans les interfaces de contrôleurs : @Tag(name = ApiTags.DEPOTS, ...)
 */
public final class ApiTags {

    public static final String CLIENTS = "Clients";
    public static final String COMPTES = "Comptes";
    public static final String DEPOTS = "Dépôts";
    public static final String RETRAITS = "Retraits";

    public static final String CLIENTS_DESC = "Gestion des clients de la banque";
    public static final String COMPTES_DESC = "Gestion des comptes bancaires";
    public static final String DEPOTS_DESC = "Versements d'argent sur un compte (réservé à la secrétaire)";
    public static final String RETRAITS_DESC = "Retraits d'argent (réservé au client, depuis son propre compte)";

    private ApiTags() {
    }
}