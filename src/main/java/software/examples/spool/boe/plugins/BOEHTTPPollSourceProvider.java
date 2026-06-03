package software.examples.spool.boe.plugins;

import software.spool.crawler.api.port.source.PollSource;
import software.spool.infrastructure.spi.SpoolPlugin;
import software.spool.infrastructure.spi.provider.PluginConfiguration;
import software.spool.infrastructure.spi.provider.PollSourceProvider;

@SpoolPlugin(PollSourceProvider.class)
public class BOEHTTPPollSourceProvider implements PollSourceProvider {
    @Override
    public String name() {
        return "BOE_HTTP";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean supports(PluginConfiguration configuration) {
        return configuration.has("url");
    }

    @Override
    public PollSource<?> create(PluginConfiguration configuration) {
        return new BOEHTTPPollSource(configuration.require("url"));
    }
}
