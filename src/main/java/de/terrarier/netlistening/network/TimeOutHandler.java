/*
Copyright 2021 Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.api.event.ConnectionTimeoutEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internal.AssumeNotNull;
import de.terrarier.netlistening.internal.InternalUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jetbrains.annotations.ApiStatus;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static java.lang.Byte.MAX_VALUE;
import static java.lang.Byte.MIN_VALUE;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class TimeOutHandler extends ReadTimeoutHandler {

    private final ApplicationImpl application;
    private final ConnectionImpl connection;
    private Timer timer = new Timer(true);
    private byte counter = MIN_VALUE;
    private ByteBuf buffer;

    public TimeOutHandler(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                          long timeout) {
        super(timeout, TimeUnit.MILLISECONDS);
        this.application = application;
        this.connection = connection;

        final long delay = timeout / 2;
        final boolean client = application instanceof Client;
        final Channel channel = connection.getChannel();
        final ChannelPromise voidPromise = channel.voidPromise();

        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (client ? !((ClientImpl) application).hasReceivedHandshake() : !connection.isStable()) {
                    return;
                }

                if (counter == MAX_VALUE) {
                    counter = MIN_VALUE;
                } else if (buffer == null) {
                    buffer = Unpooled.buffer(InternalUtil.singleOctetIntSize(application) + 1);
                    InternalUtil.writeIntUnchecked(application, buffer, 0x1);
                    buffer.markWriterIndex();
                }

                buffer.resetWriterIndex();
                buffer.writeByte(++counter);
                buffer.retain();
                channel.writeAndFlush(buffer, voidPromise);
            }
        }, delay, delay);
    }

    @Override
    protected void readTimedOut(@AssumeNotNull ChannelHandlerContext ctx) throws Exception {
        if (!callTimeOut()) {
            cancel();
            super.readTimedOut(ctx);
        }
    }

    @Override
    public void handlerRemoved(@AssumeNotNull ChannelHandlerContext ctx) throws Exception {
        cancel();
        super.handlerRemoved(ctx);
    }

    @Override
    public void close(@AssumeNotNull ChannelHandlerContext ctx, @AssumeNotNull ChannelPromise promise)
            throws Exception {
        cancel();
        super.close(ctx, promise);
    }

    private void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            if (buffer != null && buffer.refCnt() > 0) {
                buffer.release();
            }
        }
    }

    private boolean callTimeOut() {
        return application.getEventManager().callEvent(ListenerType.TIMEOUT, new ConnectionTimeoutEvent(connection));
    }

}
