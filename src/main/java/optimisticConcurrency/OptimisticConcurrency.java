package optimisticConcurrency;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

public class OptimisticConcurrency {

    public static void main(String[] args) {
        // Load the configuration and build the SessionFactory
        Configuration configuration = HibernateConfig.getConfig();
        configuration.addAnnotatedClass(Person.class);
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();
        SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        System.out.println("SessionFactory created successfully.");

        // Initialize data
        initializeData(sessionFactory);

        // Create sessions
        Session session1 = sessionFactory.openSession();
        Session session2 = sessionFactory.openSession();
        Transaction tx1 = session1.beginTransaction();
        Transaction tx2 = session2.beginTransaction();

        try {
            // Both sessions load the same Person entity
            Person person1 = session1.get(Person.class, 1L);
            Person person2 = session2.get(Person.class, 1L);

            // Null check
            if (person1 == null || person2 == null) {
                System.out.println("No Person found with ID 1. Please ensure data is initialized.");
                return;
            }

            // Session1 modifies the entity
            person1.setName("John Updated");
            session1.update(person1);

            // Commit session1 transaction (increments the version)
            tx1.commit();

            // Session2 tries to modify the entity with the old version
            person2.setName("John Conflict");
            session2.update(person2);

            // Commit session2 transaction (throws StaleObjectStateException)
            tx2.commit();
        } catch (org.hibernate.StaleObjectStateException e) {
            System.out.println("Optimistic Lock Exception: Entity version conflict.");
            tx2.rollback();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            tx2.rollback();
        } finally {
            session1.close();
            session2.close();
        }
    }

    private static void initializeData(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Person person = new Person();
            person.setName("John Doe");
            session.save(person);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            System.out.println("Data Initialization Error: " + e.getMessage());
        } finally {
            session.close();
        }
    }
}
