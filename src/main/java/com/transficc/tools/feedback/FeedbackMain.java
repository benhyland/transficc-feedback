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
package com.transficc.tools.feedback;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offbytwo.jenkins.JenkinsServer;
import com.transficc.tools.feedback.ci.ContinuousIntegrationServer;
import com.transficc.tools.feedback.ci.JobService;
import com.transficc.tools.feedback.ci.jenkins.JenkinsFacade;
import com.transficc.tools.feedback.dao.IterationDao;
import com.transficc.tools.feedback.util.ClockService;
import com.transficc.tools.feedback.util.FeedbackProperties;
import com.transficc.tools.feedback.util.LoggingThreadFactory;
import com.transficc.tools.feedback.util.SafeSerialisation;
import com.transficc.tools.feedback.web.messaging.JobUpdateSubscriber;
import com.transficc.tools.feedback.web.messaging.MessageBus;
import com.transficc.tools.feedback.web.routes.Routes;
import com.transficc.tools.feedback.web.routes.WebSocketPublisher;
import com.transficc.tools.feedback.web.routes.websocket.OutboundWebSocketFrame;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;

public class FeedbackMain
{
    private static final String SERVICE_NAME = "transficc-feedback";
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackMain.class);

    public static void main(final String[] args)
    {
        final Optional<File> propertiesFile = args.length == 1 ? Optional.of(new File(args[0])) : Optional.empty();
        final FeedbackProperties feedbackProperties = new FeedbackProperties(propertiesFile);
        System.setProperty("vertx.cacheDirBase", feedbackProperties.getVertxCacheDir());
        final Vertx vertx = Vertx.vertx();
        final HttpServer server = vertx.createHttpServer();
        final ObjectMapper mapper = Json.mapper;
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        final SafeSerialisation safeSerialisation = new SafeSerialisation(mapper);
        final ClockService clockService = System::currentTimeMillis;
        final JobRepository jobRepository = new JobRepository(feedbackProperties.getJobsWithPriorities());
        final long startUpTime = clockService.currentTimeMillis();
        final WebSocketPublisher webSocketPublisher = new WebSocketPublisher(vertx.eventBus(), safeSerialisation, clockService, jobRepository, startUpTime);
        final JenkinsServer jenkins = createJenkinsServer(feedbackProperties);
        final BlockingQueue<OutboundWebSocketFrame> messageQueue = new LinkedBlockingQueue<>();

        final JdbcConnectionPool dataSource = JdbcConnectionPool.create("jdbc:h2:~/data/feedback", "feedback", "");

        final Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();

        final ThreadFactory threadFactory = new LoggingThreadFactory(SERVICE_NAME);
        final ExecutorService statusCheckerService = Executors.newFixedThreadPool(1, threadFactory);
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        statusCheckerService.submit(new JobUpdateSubscriber(messageQueue, webSocketPublisher));
        final MessageBus messageBus = new MessageBus(messageQueue);
        final ContinuousIntegrationServer ciServer = new JenkinsFacade(jenkins, clockService, feedbackProperties.getVersionControl());
        final JobService jobService = new JobService(jobRepository, messageBus, scheduledExecutorService, ciServer, feedbackProperties.getMasterJobName());
        final IterationRepository iterationRepository = new IterationRepository(messageBus, new IterationDao(dataSource));
        Routes.setup(server, jobRepository, iterationRepository, new BreakingNewsService(messageBus), webSocketPublisher, Router.router(vertx), startUpTime);

        scheduledExecutorService.scheduleAtFixedRate(jobService, 0, 5, TimeUnit.MINUTES);
        server.listen(feedbackProperties.getFeedbackPort());
    }

    private static JenkinsServer createJenkinsServer(final FeedbackProperties feedbackProperties)
    {
        final JenkinsServer jenkins;
        final String jenkinsUrl = feedbackProperties.getJenkinsUrl();
        if (feedbackProperties.getJenkinsUsername() == null)
        {
            jenkins = new JenkinsServer(URI.create(jenkinsUrl));
        }
        else
        {
            jenkins = new JenkinsServer(URI.create(jenkinsUrl), feedbackProperties.getJenkinsUsername(), feedbackProperties.getJenkinsPassword());
        }
        LOGGER.info("Connecting to Jenkins server at {}", jenkinsUrl);
        return jenkins;
    }

}
