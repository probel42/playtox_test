package ru.ibelan;

import jakarta.persistence.NoResultException;
import org.flywaydb.core.Flyway;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ibelan.model.Account;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Я не буду разбивать на классы, т.к. задача не про архитектуру, а про многопоточку и уровни изоляции.
 */
public class PlaytoxTest {
	public static final int ACCOUNTS_NUMBER = 50;
	public static final int THREADS_NUMBER = 10;
	public static final int TRANSACTIONS_NUMBER = 30;
	public static final int SLEEP_FROM = 1000;
	public static final int SLEEP_TO = 2000;

	private static final Logger log = LoggerFactory.getLogger(PlaytoxTest.class);

	private static final SessionFactory factory;

	private static final List<Thread> threadPool = new ArrayList<>();

	static {
		factory = new Configuration()
				.setInterceptor(new LoggingInterceptor())
				.addAnnotatedClass(Account.class)
				.configure().buildSessionFactory();
	}

	private static void wrapInSession(Consumer<Session> transaction) {
		try (Session session = factory.openSession()) {
			transaction.accept(session);
		} catch (Exception ex) {
			log.warn(ex.getMessage());
		}
	}

	public static void main(String[] args) {
		init();

		for (int i = 0; i < THREADS_NUMBER; i++) {
			threadPool.add(new Thread(new TransferTask()));
		}
		threadPool.forEach(Thread::start);
	}

	private static void init() {
		Flyway.configure()
				.dataSource( "jdbc:postgresql://localhost:5432/playtox_db" , "playtox_app" , "playtox" )
				.locations("classpath:db/migrations")
				.load()
				.migrate();

		try (Session session = factory.openSession()) {
			Transaction tx = session.beginTransaction();
			for (int i = 0; i < ACCOUNTS_NUMBER; i++) {
				Account account = new Account();
				account.setId(UUID.randomUUID());
				account.setMoney(10000L);
				session.persist(account);
			}
			tx.commit();
		} catch (Exception ex) {
			log.warn(ex.getMessage());
		}
	}

	static class TransferTask implements Runnable {
		private static final AtomicInteger counter = new AtomicInteger(TRANSACTIONS_NUMBER);

		// проще всего заблокировать через for update и skip locked нативным postgres, но тогда будет завязка на postgres
		private static final String queryFrom = "SELECT * FROM account a WHERE money > 0 ORDER BY random() LIMIT 1 FOR UPDATE SKIP LOCKED";
		private static final String queryTo = "SELECT * FROM account a WHERE id <> :exceptId ORDER BY random() LIMIT 1 FOR UPDATE SKIP LOCKED";

		@Override
		public void run() {
			try {
				do {
					int time = ThreadLocalRandom.current().nextInt(SLEEP_TO - SLEEP_FROM) + SLEEP_FROM;
					Thread.sleep(time);
					if (counter.getAndDecrement() <= 0) {
						break;
					}
					transfer();
				} while (true);
			} catch (InterruptedException ignored) {
			}
		}

		private static void transfer() {
			wrapInSession(session -> {
				Transaction tx = session.beginTransaction();
				try {
					Account fromAccount = session.createNativeQuery(queryFrom, Account.class)
							.getSingleResult();
					Account toAccount = session.createNativeQuery(queryTo, Account.class)
							.setParameter("exceptId", fromAccount.getId())
							.getSingleResult();
					long money = ThreadLocalRandom.current().nextLong(fromAccount.getMoney()) + 1;
					log.info("Thread \"{}\" : from account \"{}\": ({}-{}={}) to account \"{}\": ({}+{}={})",
							Thread.currentThread().getName(),
							fromAccount.getId(),
							fromAccount.getMoney(),
							money,
							fromAccount.getMoney() - money,
							toAccount.getId(),
							toAccount.getMoney(),
							money,
							toAccount.getMoney() + money);
					fromAccount.setMoney(fromAccount.getMoney() - money);
					toAccount.setMoney(toAccount.getMoney() + money);
					tx.commit();
				} catch (NoResultException noResultException) {
					tx.rollback();
					log.warn("can't find account for transfer"); // todo в ТЗ нет описания что делать в таких ситуациях
				}
			});
		}
	}
}
