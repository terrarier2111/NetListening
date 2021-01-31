package de.terrarier.netlistening.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ChannelUtil {
	
	private ChannelUtil() {}

	/**
	 * Applies options chosen by the user or default to the channel.
	 *
	 * @param channel the channel getting optimized.
	 * @see <a href="https://en.wikipedia.org/wiki/Type_of_service">https://en.wikipedia.org/wiki/Type_of_service</a>
	 */
	@SuppressWarnings("unchecked")
	public static void prepare(@NotNull Channel channel, @NotNull Map<ChannelOption<?>, Object> options) {
		try {
			final ChannelConfig config = channel.config();
			for(ChannelOption option : options.keySet()) {
				config.setOption(option, options.get(option));
			}
		} catch (ChannelException ignored) {}
	}
	
}
