package com.neverwinterdp.kafka.producer.servers;

import java.util.HashSet;
import java.util.Set;

public class Servers {

  private Set<Server> kafkaServers;
  private Set<Server> zookeeperServers;
  private int zkPort;
  private int kafkaPort;
  private int kafkaBrokers;
  private String dataDir;

  public Servers(String dataDir, int zkPort, int kafkaPort, int kafkaBrokers) {
    super();
    this.dataDir = dataDir;
    this.zkPort = zkPort;
    this.kafkaPort = kafkaPort;
    this.kafkaBrokers = kafkaBrokers;
    zookeeperServers = new HashSet<>();
    kafkaServers= new HashSet<>(kafkaBrokers);
  }


  public void start() throws Exception {
    ZookeeperServerLauncher zookeeper = new ZookeeperServerLauncher(dataDir, zkPort);
    zookeeper.start();
    zookeeperServers.add(zookeeper);
    
    KafkaServerLauncher kafka;
    for (int i = 0; i < kafkaBrokers; i++) {
      kafka = new KafkaServerLauncher(i, dataDir+"/"+i, kafkaPort++);
      kafka.start();
      kafkaServers.add(kafka);
    }    
  }


  public Set<Server> getKafkaServers() {
    return kafkaServers;
  }

  public Set<Server> getzookeeperServers() {
    return zookeeperServers;
  }

  public void shutdown() throws Exception {
    for (Server server : kafkaServers) {
      server.shutdown();
    }

    for (Server server : zookeeperServers) {
      server.shutdown();
    }
  }
}