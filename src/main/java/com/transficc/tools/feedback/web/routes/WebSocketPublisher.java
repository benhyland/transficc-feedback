/*
 * Copyright 2017 TransFICC Ltd.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.
 */
package com.transficc.tools.feedback.web.routes;

import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.transficc.tools.feedback.util.ClockService;
import com.transficc.tools.feedback.util.SafeSerialisation;
import com.transficc.tools.feedback.web.routes.websocket.OutboundWebSocketFrame;
import com.transficc.tools.feedback.web.routes.websocket.WebSocketFrameHandler;


import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.ServerWebSocket;


public final class WebSocketPublisher implements Handler<ServerWebSocket>
{
    private final Deque<String> sessions = new ConcurrentLinkedDeque<>();
    private final EventBus eventBus;
    private final SafeSerialisation safeSerialisation;
    private final ClockService clockService;
    private final JobStatusSnapshot jobStatusSnapshot;
    private final long startUpTime;

    public WebSocketPublisher(final EventBus eventBus, final SafeSerialisation safeSerialisation, final ClockService clockService, final JobStatusSnapshot jobStatusSnapshot, final long startUpTime)
    {
        this.eventBus = eventBus;
        this.safeSerialisation = safeSerialisation;
        this.clockService = clockService;
        this.jobStatusSnapshot = jobStatusSnapshot;
        this.startUpTime = startUpTime;
    }

    public void onMessage(final OutboundWebSocketFrame message)
    {
        broadcastMessage(message);
    }

    @Override
    public void handle(final ServerWebSocket socket)
    {
        final String id = socket.textHandlerID();
        sessions.addLast(id);
        socket.closeHandler(event -> sessions.remove(id));
        socket.frameHandler(new WebSocketFrameHandler(id, eventBus, safeSerialisation, clockService, jobStatusSnapshot, startUpTime));
    }

    private void broadcastMessage(final OutboundWebSocketFrame outboundWebSocketFrame)
    {
        final Iterator<String> iterator = sessions.iterator();
        final String outbound = safeSerialisation.serisalise(outboundWebSocketFrame);
        while (iterator.hasNext())
        {
            eventBus.send(iterator.next(), outbound);
        }
    }
}
