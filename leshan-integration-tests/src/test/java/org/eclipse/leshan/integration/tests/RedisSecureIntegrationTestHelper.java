/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;

import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.CaliforniumObservationRegistryImpl;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.demo.cluster.RedisClientRegistry;
import org.eclipse.leshan.server.demo.cluster.RedisObservationStore;
import org.eclipse.leshan.server.demo.cluster.RedisSecurityRegistry;
import org.eclipse.leshan.server.model.StaticModelProvider;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

public class RedisSecureIntegrationTestHelper extends SecureIntegrationTestHelper {
    @Override
    public void createServer() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        StaticModelProvider modelProvider = new StaticModelProvider(createObjectModels());
        builder.setObjectModelProvider(modelProvider);
        DefaultLwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);
        builder.setLocalAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        builder.setLocalSecureAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

        // Create redis store
        String redisURI = System.getenv("REDIS_URI");
        if (redisURI == null)
            redisURI = "";
        Pool<Jedis> jedis = new JedisPool(redisURI);
        ClientRegistry clientRegistry = new RedisClientRegistry(jedis);
        builder.setClientRegistry(clientRegistry);
        builder.setObservationRegistry(new CaliforniumObservationRegistryImpl(new RedisObservationStore(jedis),
                clientRegistry, modelProvider, decoder));
        builder.setSecurityRegistry(new RedisSecurityRegistry(jedis, null, null));

        // Build server !
        server = builder.build();
        // monitor client registration
        resetLatch();
        server.getClientRegistry().addListener(new ClientRegistryListener() {
            @Override
            public void updated(ClientUpdate update, Client clientUpdated) {
                if (clientUpdated.getEndpoint().equals(getCurrentEndpoint())) {
                    updateLatch.countDown();
                }
            }

            @Override
            public void unregistered(Client client) {
                if (client.getEndpoint().equals(getCurrentEndpoint())) {
                    deregisterLatch.countDown();
                }
            }

            @Override
            public void registered(Client client) {
                if (client.getEndpoint().equals(getCurrentEndpoint())) {
                    last_registration = client;
                    registerLatch.countDown();
                }
            }
        });
    }

    @Override
    public void createServerWithRPK() {
        throw new UnsupportedOperationException("Not implemeneted");
    }

    @Override
    public void createServerWithX509Cert(Certificate[] trustedCertificates) {
        throw new UnsupportedOperationException("Not implemeneted");
    }

}
