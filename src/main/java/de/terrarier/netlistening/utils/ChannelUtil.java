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
	 * Applies the options chosen by the user or set by default to the channel.
	 *
	 * @param channel the channel getting optimized.
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
