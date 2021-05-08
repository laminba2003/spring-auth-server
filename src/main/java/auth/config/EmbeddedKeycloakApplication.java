package auth.config;

import java.util.NoSuchElementException;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import auth.config.KeycloakServerProperties.AdminUser;

public class EmbeddedKeycloakApplication extends KeycloakApplication {

	private static final Logger logger = LoggerFactory.getLogger(EmbeddedKeycloakApplication.class);

	static KeycloakServerProperties keycloakServerProperties;
	
	public EmbeddedKeycloakApplication() {	
		super();
		createMasterRealmAdminUser();
		createDefaultRealm();
	}
	
	protected void loadConfig() {
        JsonConfigProviderFactory factory = new RegularJsonConfigProviderFactory();
        Config.init(factory.create()
            .orElseThrow(() -> new NoSuchElementException("No value present")));
    }

	private void createMasterRealmAdminUser() {
		KeycloakSession session = getSessionFactory().create();
		ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
		try {
			session.getTransactionManager().begin();
			AdminUser admin = keycloakServerProperties.getAdmin();
			applianceBootstrap.createMasterRealmUser(admin.getUsername(), admin.getPassword());
			session.getTransactionManager().commit();
		} catch (Exception ex) {
			logger.warn("Couldn't create keycloak master admin user: {}", ex.getMessage());
			session.getTransactionManager().rollback();
		}
		session.close();
	}

	private void createDefaultRealm() {
		KeycloakSession session = getSessionFactory().create();
		try {
			session.getTransactionManager().begin();
			RealmManager manager = new RealmManager(session);
			Resource realm = new ClassPathResource(keycloakServerProperties.getRealm());
			manager.importRealm(
					JsonSerialization.readValue(realm.getInputStream(), RealmRepresentation.class));
			session.getTransactionManager().commit();
		} catch (Exception ex) {
			logger.warn("Failed to import Realm json file: {}", ex.getMessage());
			session.getTransactionManager().rollback();
		}
		session.close();
	}
	
}