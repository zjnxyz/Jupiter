/*
 * Copyright (c) 2016 The Jupiter Project
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

package org.jupiter.rpc.consumer.invoker;

import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.JFuture;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public class FutureGenericInvoker implements GenericInvoker {

    private static final ThreadLocal<JFuture> futureThreadLocal = new ThreadLocal<>();

    private final JClient client;
    private final Dispatcher dispatcher;

    public FutureGenericInvoker(JClient client, Dispatcher dispatcher) {
        this.client = client;
        this.dispatcher = dispatcher;
    }

    public static JFuture future() {
        JFuture future = checkNotNull(futureThreadLocal.get(), "future");
        futureThreadLocal.remove();
        return future;
    }

    @Override
    public Object $invoke(String methodName, Object... args) throws Throwable {
        JFuture future = dispatcher.dispatch(client, methodName, args);
        futureThreadLocal.set(future);
        return null;
    }
}
