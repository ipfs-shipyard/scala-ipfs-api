package io.ipfs.api.example;


import io.ipfs.api.Add;
import io.ipfs.api.Client;
import io.ipfs.api.ConfigShow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;

public class Example {
    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0]: "localhost";
        System.out.println("Using host " + host);

        Client client = new Client(host, 5001, "/api/v0", "http");

        ConfigShow configShow = client.configShow();
        System.out.println("Config");
        System.out.println(configShow);

        //create test file
        Path addPath = Paths.get("ipfs.put.tmp.txt");
        Files.write(addPath, "Hello IPFS!".getBytes(), StandardOpenOption.CREATE);

        //add to IPFS
        Add[] add = client.add(new Path[]{addPath});
        Add added = add[0];
        System.out.println("Added " + added.Name() + " with hash " +  added.Hash());

        //get from IPFS
        Path getPath = Paths.get("ipfs.get.tmp.txt");
        try (InputStream inputStream = client.cat(added.Hash())) {
            Files.copy(inputStream, getPath, StandardCopyOption.REPLACE_EXISTING);
        }

        boolean equals = Arrays.equals(Files.readAllBytes(getPath), Files.readAllBytes(addPath));
        System.out.println("Get data equals put data  ? " + equals);
    }
}
