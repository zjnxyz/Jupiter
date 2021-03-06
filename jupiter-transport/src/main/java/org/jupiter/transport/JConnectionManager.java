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

package org.jupiter.transport;

import org.jupiter.common.util.Maps;
import org.jupiter.rpc.UnresolvedAddress;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public class JConnectionManager {

    private static final ConcurrentMap<UnresolvedAddress, CopyOnWriteArrayList<JConnection>> connections = Maps.newConcurrentHashMap();

    public static void manage(JConnection connection) {
        UnresolvedAddress address = connection.getAddress();
        CopyOnWriteArrayList<JConnection> list = connections.get(address);
        if (list == null) {
            CopyOnWriteArrayList<JConnection> newList = new CopyOnWriteArrayList<>();
            list = connections.putIfAbsent(address, newList);
            if (list == null) {
                list = newList;
            }
        }
        list.add(connection);
    }

    public static void cancelReconnect(UnresolvedAddress address) {
        CopyOnWriteArrayList<JConnection> list = connections.get(address);
        if (list != null) {
            for (JConnection c : list) {
                c.setReconnect(false);
            }
        }
    }
}
