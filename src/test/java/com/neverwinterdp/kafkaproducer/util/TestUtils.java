package com.neverwinterdp.kafkaproducer.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import kafka.admin.PreferredReplicaLeaderElectionCommand;
import kafka.common.TopicAndPartition;
import kafka.producer.KeyedMessage;
import kafka.producer.Producer;
import kafka.producer.ProducerConfig;
import kafka.utils.ZKStringSerializer$;

import org.I0Itec.zkclient.ZkClient;

import scala.collection.mutable.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class TestUtils {

  public static boolean isPortOpen(int port) {
    Socket socket = new Socket();
    try {
      socket.connect(new InetSocketAddress("127.0.0.1", port));
      socket.close();
      return true;
    } catch (IOException IOE) {
      return false;
    } finally {
      try {
        socket.close();
      } catch (IOException IOE) {
        // ignore
      }
    }
  }

  // TODO ensure all messages are read
  public static List<String> readMessages(String topic, String zkURL) {
    int numPartitions = 0;
    List<String> messages = new LinkedList<>();
    ZookeeperHelper helper = null;
    try {
      helper = new ZookeeperHelper(zkURL);
      waitUntilMetadataIsPropagated(zkURL, topic);
      numPartitions = helper.getBrokersForTopic(topic).keySet().size();
      System.out.println("Leader for topic --> " + helper.getLeaderForTopicAndPartition(topic, 0));
      for (int i = 0; i < numPartitions; i++) {
        try (Consumer consumer = new Consumer(zkURL, topic, i)) {
          messages.addAll(consumer.read());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        System.out.println("helper.getLeader " + helper.getLeaderForTopicAndPartition(topic, 0));
        helper.close();
      } catch (Exception e) {
      }
    }
    return messages;
  }

  public static String createRandomTopic() {
    return Long.toHexString(Double.doubleToLongBits(Math.random()));
  }


  public static List<Integer> convert(List<String> messages) {
    return Lists.transform(messages,
        new Function<String, Integer>() {
          @Override
          public Integer apply(String t) {
            return Integer.parseInt(t);
          }
        });
  }

  public static void waitUntilMetadataIsPropagated(String zkURL, String topic) {
    ZkClient zkClient = new ZkClient(zkURL, 30000, 30000, ZKStringSerializer$.MODULE$);
    TopicAndPartition partition = new TopicAndPartition(topic, 0);
    Set<TopicAndPartition> x =
        scala.collection.JavaConversions.asScalaSet(Collections.singleton(partition));
    PreferredReplicaLeaderElectionCommand command =
        new PreferredReplicaLeaderElectionCommand(zkClient, x);
    command.moveLeaderToPreferredReplica();
    zkClient.close();
  }

  public static void writeRandomData(String topic, int kafkaPort, int writes) {
    Properties props = kafka.utils.TestUtils.getProducerConfig("localhost:" + kafkaPort);
    ProducerConfig config = new ProducerConfig(props);
    Producer<String, byte[]> producer = new Producer<>(config);
    List<KeyedMessage<String, byte[]>> messages = new ArrayList<>();
    for (int i = 0; i < writes; i++) {
      KeyedMessage<String, byte[]> data = new KeyedMessage<>(topic, String.valueOf(i).getBytes());
      messages.add(data);
      System.out.println("wrote " + i);
    }
    producer.send(scala.collection.JavaConversions.asScalaBuffer(messages));

    producer.close();

  }

  public static java.util.Set<Integer> createRange(int start, int count) {

    java.util.Set<Integer> ints = new LinkedHashSet<Integer>();
    for (int i = start; i < count; ++i) {
      ints.add(i);
    }
    return ints;
  }
}
