package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.event.ConnectionTimeoutEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.InternalUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static java.lang.Byte.MAX_VALUE;
import static java.lang.Byte.MIN_VALUE;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class TimeOutHandler extends ReadTimeoutHandler {

	private final ApplicationImpl application;
	private Timer timer = new Timer(true);
	private byte counter = MIN_VALUE;
	private ByteBuf buffer;
	
	public TimeOutHandler(@NotNull ApplicationImpl application,
						  @NotNull ConnectionImpl connection, long timeout) {
		super(timeout, TimeUnit.MILLISECONDS);
		this.application = application;

		final long delay = timeout / 2;
		final boolean client = application.isClient();
		final Channel channel = connection.getChannel();

		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				if ((!client && !connection.isStable())
						|| (client && !((ClientImpl) application).hasReceivedHandshake())) {
					return;
				}

				if (counter == MAX_VALUE) {
					counter = MIN_VALUE;
				} else if (buffer == null) {
					buffer = Unpooled.buffer(application.getCompressionSetting().isVarIntCompression() ? 2 : 5);
					InternalUtil.writeIntUnchecked(application, buffer, 0x1);
					buffer.markWriterIndex();
				}

				buffer.resetWriterIndex();
				buffer.writeByte(counter++);

				buffer.retain();
				channel.writeAndFlush(buffer);
			}
		}, delay, delay);
	}

	@Override
	protected void readTimedOut(@NotNull ChannelHandlerContext ctx) throws Exception {
		if (!callTimeOut(ctx.channel())) {
			cancel();
			super.readTimedOut(ctx);
		}
	}
	
	@Override
	public void handlerRemoved(@NotNull ChannelHandlerContext ctx) throws Exception {
		cancel();
		super.handlerRemoved(ctx);
	}
	
	@Override
	public void close(@NotNull ChannelHandlerContext ctx, @NotNull ChannelPromise promise) throws Exception {
		cancel();
		super.close(ctx, promise);
	}
	
	private void cancel() {
		if (timer != null) {
			timer.cancel();
			timer = null;
			if(buffer != null && buffer.refCnt() > 0) {
				buffer.release();
			}
		}
	}

	private boolean callTimeOut(@NotNull Channel channel) {
		final Connection connection = application.getConnection(channel);

		if (connection == null) {
			return false;
		}

		return application.getEventManager().callEvent(ListenerType.TIMEOUT, new ConnectionTimeoutEvent(connection));
	}

}
