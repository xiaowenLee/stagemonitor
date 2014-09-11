package org.stagemonitor.ehcache;

import com.codahale.metrics.MetricRegistry;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.rest.RestClient;

import java.util.ArrayList;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.core.util.GraphiteSanitizer.sanitizeGraphiteMetricSegment;

public class EhCachePlugin implements StageMonitorPlugin {

	private final ConfigurationOption<String> ehCacheNameOption = ConfigurationOption.stringOption()
			.key("stagemonitor.ehcache.name")
			.dynamic(false)
			.label("EhCache cache name")
			.description("The name of the ehcache to instrument (the value of the 'name' attribute of the " +
					"'ehcache' tag in ehcache.xml)")
			.defaultValue(null)
			.pluginName("EhCache Plugin")
			.build();

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		List<ConfigurationOption<?>> config = new ArrayList<ConfigurationOption<?>>();
		config.add(ehCacheNameOption);
		return config;
	}

	public String getEhcacheName() {
		return ehCacheNameOption.getValue();
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		final CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		final CacheManager cacheManager = CacheManager.getCacheManager(getEhcacheName());
		for (String cacheName : cacheManager.getCacheNames()) {
			final Cache cache = cacheManager.getCache(cacheName);
			cache.setStatisticsEnabled(true);

			final String metricPrefix = name("cache", sanitizeGraphiteMetricSegment(cache.getName()));
			final StagemonitorCacheUsageListener cacheUsageListener = new StagemonitorCacheUsageListener(metricPrefix, metricRegistry);
			cache.registerCacheUsageListener(cacheUsageListener);
			metricRegistry.registerAll(new EhCacheMetricSet(metricPrefix, cache, cacheUsageListener));
		}

		RestClient.sendGrafanaDashboardAsync(corePlugin.getElasticsearchUrl(), "EhCache.json");
	}

}
