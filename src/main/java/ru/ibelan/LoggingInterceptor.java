package ru.ibelan;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingInterceptor implements Interceptor {
	private static Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

	@Override
	public void onCollectionUpdate(Object collection, Object key) throws CallbackException {
		log.info("onCollectionUpdate : Thread \"{}\" : {} : {}", Thread.currentThread().getName(), collection, key);
	}
}
