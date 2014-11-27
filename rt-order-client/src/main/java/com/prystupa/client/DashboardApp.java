package com.prystupa.client;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.prystupa.core.Event;
import com.prystupa.core.EventID;
import com.prystupa.core.EventStore;
import com.prystupa.core.command.StoreCommand;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class DashboardApp {

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {

        final String DEFAULT_PRIME_ID = "PrimeID";
        final ClientConfig config = ClientUtils.buildConfig();
        final HazelcastInstance client = HazelcastClient.newHazelcastClient(config);
        final EventStore store = new EventStore(client);
        final IExecutorService executorService = client.getExecutorService("default");

        System.out.println("Enter order pairs, empty string to exit:");
        Scanner scanner = new Scanner(System.in);
        String line;
        while (!(line = scanner.nextLine()).equals("")) {
            if (line.equals("clear")) {
                store.clear();
                continue;
            }
            if (line.equals("count")) {
                System.out.println(store.chainCount());
                continue;
            }
            if (line.startsWith("chain")) {
                String[] parts = line.split("\\s");
                String chainId = parts[1];
                String primeId = parts.length > 2 ? parts[2] : DEFAULT_PRIME_ID;
                System.out.println(store.chain(new EventID(chainId, primeId)));
                continue;
            }

            String[] order = line.split("\\s");
            String id = order[0];
            String parentId = order[1];
            String primeId = order.length > 2 ? order[2] : DEFAULT_PRIME_ID;
            Event event = new Event(id, parentId, primeId);

            executorService.submitToKeyOwner(new StoreCommand(event), primeId, new ExecutionCallback() {
                @Override
                public void onResponse(Object response) {

                }

                @Override
                public void onFailure(Throwable t) {

                }
            });
            store.save(event);
        }

        client.shutdown();
    }
}

