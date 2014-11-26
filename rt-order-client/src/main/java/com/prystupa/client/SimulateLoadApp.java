package com.prystupa.client;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.prystupa.core.Event;
import com.prystupa.core.EventIngester;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulateLoadApp {

    private static Logger logger = LoggerFactory.getLogger(SimulateLoadApp.class);
    private static Random rand = new Random();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, ParseException {

        final Options options = new Options();
        options.addOption("n", true, "number of order events to emulate");

        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);

        int total = Integer.parseInt(cmd.getOptionValue("n", "1"));

        final ClientConfig config = ClientUtils.buildConfig();
        final HazelcastInstance client = HazelcastClient.newHazelcastClient(config);
        final EventIngester ingester = new EventIngester(client);

        final List<Event> events = generateEvents(total);
        Collections.shuffle(events);

        final AtomicInteger batch = new AtomicInteger(0);
        for (Event event : events) {
            final CompletableFuture<Object> future = ingester.ingest(event);
            future.thenRun(() -> {
                int ingested = batch.addAndGet(1);
                if (ingested % 10000 == 0 || ingested == total) {
                    logger.info("Ingested {} event(s)", ingested);
                }
            });
        }

        for (; ; ) {
            if (batch.get() >= total) {
                break;
            }
        }
        client.shutdown();
    }

    private static List<Event> generateEvents(final int count) {
        List<Event> list = new ArrayList<>(count);

        int remaining = count;
        int chains = 0;
        while (remaining > 0) {
            int chain = Math.min(randInt(1, 20), remaining);
            remaining -= chain;

            String parent = UUID.randomUUID().toString();
            String primeId = "PrimeID-" + randInt(1, 5000);
            list.add(new Event(parent, parent, primeId));
            --chain;
            while (--chain >= 0) {
                String child = UUID.randomUUID().toString();
                list.add(new Event(child, parent, primeId));
                parent = child;
            }

            chains++;
        }

        logger.info("Created {} chain(s)", chains);
        return list;
    }

    private static int randInt(int min, int max) {

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return rand.nextInt((max - min) + 1) + min;
    }
}
