package fr.sorbonne_u.gutenDAAR.configuration;

import fr.sorbonne_u.gutenDAAR.Settings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration of the executors (thread pools) of the application.
 *
 * <p>
 * Inject an executor by its bean name, e.g.
 * {@code @Qualifier(ExecutorConfig.FULL_TEXT_EXECUTOR) AsyncTaskExecutor executor}.
 * </p>
 */
@Configuration
public class ExecutorConfig {

    /**
     * Executor scanning book indexed terms: each task loads a book postings from the database
     * and applies a regex on its terms.
     * @return          The executor, initialized and shut down by Spring.
     */
    @Bean(Settings.REGEX_EXECUTOR)
    public ThreadPoolTaskExecutor regexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Settings.REGEX_THREADS);
        executor.setMaxPoolSize(Settings.REGEX_THREADS);
        if (Settings.REGEX_QUEUE_CAPACITY != null) {
            executor.setQueueCapacity(Settings.REGEX_QUEUE_CAPACITY);
        }
        executor.setThreadNamePrefix(Settings.REGEX_THREAD_PREFIX);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * Executor scanning book full texts: each task loads a book text from the database
     * and applies a regex on its lines.
     * @return          The executor, initialized and shut down by Spring.
     */
    @Bean(Settings.FULL_TEXT_EXECUTOR)
    public ThreadPoolTaskExecutor fullTextExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Settings.FULL_TEXT_THREADS);
        executor.setMaxPoolSize(Settings.FULL_TEXT_THREADS);
        if (Settings.FULL_TEXT_QUEUE_CAPACITY != null) {
            executor.setQueueCapacity(Settings.FULL_TEXT_QUEUE_CAPACITY);
        }
        executor.setThreadNamePrefix(Settings.FULL_TEXT_THREAD_PREFIX);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
