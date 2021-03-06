/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.rpc.consumer.dispatcher;

import org.jupiter.common.util.Function;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

import java.util.List;

import static org.jupiter.rpc.DispatchMode.BROADCAST;
import static org.jupiter.rpc.Status.CLIENT_ERROR;
import static org.jupiter.serialization.SerializerHolder.serializerImpl;

/**
 * 组播方式派发消息
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public class DefaultBroadcastDispatcher extends AbstractDispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultBroadcastDispatcher.class);

    public DefaultBroadcastDispatcher(ServiceMetadata metadata) {
        super(metadata);
    }

    @Override
    public InvokeFuture dispatch(JClient proxy, String methodName, Object[] args) {
        final ServiceMetadata _metadata = metadata; // stack copy

        MessageWrapper message = new MessageWrapper(_metadata);
        message.setAppName(proxy.appName());
        message.setMethodName(methodName);
        message.setArgs(args);

        List<JChannelGroup> groupList = proxy.directory(_metadata);
        List<JChannel> channels = Lists.transform(groupList, new Function<JChannelGroup, JChannel>() {

            @Override
            public JChannel apply(JChannelGroup input) {
                return input.next();
            }
        });

        final JRequest request = new JRequest();
        request.message(message);
        request.bytes(serializerImpl().writeObject(message));

        int timeoutMillis = getMethodSpecialTimeoutMillis(methodName);
        final ConsumerHook[] _hooks = getHooks();
        JListener _listener = getListener();
        for (JChannel ch : channels) {
            final InvokeFuture future = asFuture(ch, request, timeoutMillis)
                    .hooks(_hooks)
                    .listener(_listener);

            ch.write(request, new JFutureListener<JChannel>() {

                @Override
                public void operationSuccess(JChannel channel) throws Exception {
                    future.chalkUpSentTimestamp();

                    if (_hooks != null) {
                        for (ConsumerHook h : _hooks) {
                            h.before(request, channel);
                        }
                    }
                }

                @Override
                public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                    logger.warn("Writes {} fail on {}, {}.", request, channel, cause);

                    ResultWrapper result = new ResultWrapper();
                    result.setError(cause);

                    JResponse response = JResponse.getInstance(request.invokeId(), CLIENT_ERROR, result);
                    DefaultInvokeFuture.received(channel, response);
                }
            });
        }

        return null;
    }

    @Override
    protected InvokeFuture asFuture(JChannel channel, JRequest request, int timeoutMillis) {
        return new DefaultInvokeFuture(channel, request, timeoutMillis, BROADCAST);
    }
}
