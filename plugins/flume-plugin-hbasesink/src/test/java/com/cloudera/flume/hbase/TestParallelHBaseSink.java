package com.cloudera.flume.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudera.flume.core.Event;
import com.cloudera.flume.core.EventImpl;
import com.cloudera.flume.core.Event.Priority;
import com.cloudera.flume.hbase.ParallelHBaseSink.QualifierSpec;
import com.cloudera.util.Clock;

public class TestParallelHBaseSink {
  static final Logger LOG = LoggerFactory.getLogger(TestParallelHBaseSink.class);
  public static final String DEFAULT_HOST = "qwigibo";

  
  void shipThreeEvents(ParallelHBaseSink snk) throws IOException {
    snk.open();
    try {
      Event e1 = new EventImpl("message0".getBytes(), Clock.unixTime(),
          Priority.INFO, 0, DEFAULT_HOST);
      e1.set("rowkey", Bytes.toBytes("row-key0"));
      e1.set("attr1", Bytes.toBytes("attr1_val0"));
      e1.set("attr2", Bytes.toBytes("attr2_val0"));
      e1.set("other", Bytes.toBytes("other_val0"));
      snk.append(e1);

      Event e2 = new EventImpl("message1".getBytes(), Clock.unixTime(),
          Priority.INFO, 1, DEFAULT_HOST);
      e2.set("rowkey", Bytes.toBytes("row-key1"));
      e2.set("attr1", Bytes.toBytes("attr1_val1"));
      e2.set("attr2", Bytes.toBytes("attr2_val1"));
      e2.set("other", Bytes.toBytes("other_val1"));
      snk.append(e2);

      Event e3 = new EventImpl("message2".getBytes(), Clock.unixTime(),
          Priority.INFO, 2, DEFAULT_HOST);
      e3.set("rowkey", Bytes.toBytes("row-key2"));
      e3.set("attr1", Bytes.toBytes("attr1_val2"));
      e3.set("attr2", Bytes.toBytes("attr2_val2"));
      e3.set("other", Bytes.toBytes("other_val2"));
      snk.append(e3);
      try {
        Thread.sleep(5000);
      } catch (Exception e) {
        
      }
      
    } finally {
      snk.close();
    }
  }
  
  @Test
  public void testSink() throws IOException, InterruptedException {
    final String tableName = "testSink";
    final String tableFamily1 = "family1";
    final String tableFamily2 = "family2";

    List<QualifierSpec> spec = new ArrayList<QualifierSpec>();
    spec.add(new QualifierSpec(tableFamily1, "col1", "%{attr1}"));
    spec.add(new QualifierSpec(tableFamily2, "col2", "%{attr2}"));

    ParallelHBaseSink snk = new ParallelHBaseSink(tableName, "%{rowkey}", spec);
    shipThreeEvents(snk);
  }
}

