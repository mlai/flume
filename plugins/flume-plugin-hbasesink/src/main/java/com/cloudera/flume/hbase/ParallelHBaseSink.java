/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.flume.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.flume.conf.Context;
import com.cloudera.flume.conf.SinkFactory;
import com.cloudera.flume.conf.SinkFactory.SinkBuilder;
import com.cloudera.flume.core.Event;
import com.cloudera.flume.core.EventSink;
import com.cloudera.flume.handlers.hdfs.SeqfileEventSource;
import com.cloudera.flume.reporter.ReportEvent;
import com.cloudera.util.Pair;
import com.google.common.base.Preconditions;

/**
 * This is a straightforward and completely explicit hbase sink.
 * 
 * "writeBufferSize" - If provided, autoFlush for the HTable set to "false", and
 * writeBufferSize is set to its value. If not provided, by default autoFlush is
 * set to "true" (default HTable setting). This setting is valuable to boost
 * HBase write speed. The default is 2MB.
 * 
 * "writeToWal" - Determines whether WAL should be used during writing to HBase.
 * If not provided Puts are written to WAL by default This setting is valuable
 * to boost HBase write speed, but decreases reliability level. Use it if you
 * know what it does.
 * 
 * The Sink also implements method getSinkBuilders(), so it can be used as
 * Flume's extension plugin (see flume.plugin.classes property of flume-site.xml
 * config details)
 */
public class ParallelHBaseSink extends EventSink.Base {
  private static final Logger LOG = LoggerFactory.getLogger(HBaseSink.class);
  public static final String KW_BUFFER_SIZE = "writeBufferSize";
  public static final String KW_USE_WAL = "writeToWal";
  public static final String USAGE = "usage: phbase(\"table\", \"rowkey\", "
      + "\"cf1\"," + " \"c1\", \"val1\"[,\"cf2\", \"c2\", \"val2\", ....]{, "
      + KW_BUFFER_SIZE + "=int, " + KW_USE_WAL + "=true|false})";
  
  public static final String R_QUEUE_SIZE = "queueSize";
  
  // triples for what values to write
  public static class QualifierSpec {
    String colFam;
    String col;
    String value;

    QualifierSpec() {
    }

    QualifierSpec(String cf, String c, String v) {
      this.colFam = cf;
      this.col = c;
      this.value = v;
    }
  };

  final String tableName; // not escapable
  final String rowkey; // flume escapable string
  final List<QualifierSpec> spec;
  final long writeBufferSize;
  final boolean writeToWal;
  final Configuration config;
  
  private ExecutorService executor;  
  private int numberOfThread = 5;
  private HTablePool tablePool;
  
  private ConcurrentHashMap<Long, HTableInterface> pool3 
    = new ConcurrentHashMap<Long, HTableInterface>();
  
  private HTableInterface pool2[];
  
  
  private HBaseThread threads[];

  public ParallelHBaseSink(String tableName, String rowkey, List<QualifierSpec> spec) {
    this(tableName, rowkey, spec, 0L, true, HBaseConfiguration.create());
  }

  public ParallelHBaseSink(String tableName, String rowkey, List<QualifierSpec> spec,
      long writeBufferSize, boolean writeToWal, Configuration config) {
    Preconditions.checkNotNull(tableName, "Must specify table name.");
    Preconditions.checkNotNull(spec, "Must specify cols and values to write. ");
    this.tableName = tableName;
    this.rowkey = rowkey;
    this.spec = spec;
    this.writeBufferSize = writeBufferSize;
    this.writeToWal = writeToWal;
    this.config = config;
    this.numberOfThread = config.getInt("hbase.client.threadpool.size", 5);
  }

  @Override
  public void append(Event e) throws IOException {
    String rowVal = e.escapeString(rowkey);
    Put p = new Put(rowVal.getBytes());

    for (QualifierSpec q : spec) {
      String cf = q.colFam;
      String c = e.escapeString(q.col);
      String val = e.escapeString(q.value);
      p.add(cf.getBytes(), c.getBytes(), val.getBytes());
    }
//    p.setWriteToWAL(writeToWal);
//    table.put(p);
    //HTableInterface table = tablePool.getTable(this.tableName);
    executor.execute(new EventHandler(p, tablePool));
  }

  @Override
  synchronized public void close() throws IOException {
    // release the thread pool
    executor.shutdown();
//    for (int i = 0; i < threads.length; ++i) {
//      threads[i].stop("stop thread");
//    }
  }

  @Override
  synchronized public void open() throws IOException {
    // This instantiates an HTable object and perform some initial
    // validation.
    HTable table = new HTable(config, tableName);
    if (writeBufferSize > 0) {
      table.setAutoFlush(false);
      table.setWriteBufferSize(writeBufferSize);
    }
    validateColFams(table);
    LOG.info("HBase table can be opened properly." + numberOfThread);

    table.close();
    
    // Initialize HTablePool and set init
    int tablePoolSize = config.getInt("hbase.client.threadpool.size", 5);
    tablePool = new HTablePool(this.config, tablePoolSize);
    
    HTable[] tables = new HTable[tablePoolSize];
    
    pool2 = new HTableInterface[tablePoolSize];
    
    for (int i = 0; i < tablePoolSize; ++i ) {
      tables[i] = (HTable)tablePool.getTable(tableName);
      if (writeBufferSize > 0) {
        tables[i].setAutoFlush(false);
        tables[i].setWriteBufferSize(writeBufferSize);
      }
      pool2[i] = tables[i];
    }

    for (int i = 0; i < tablePoolSize; ++i ) {
      tablePool.putTable(tables[i]);
    }
    
    // create thread pool
    executor = Executors.newFixedThreadPool(numberOfThread);
    
    // create threads
  //threads = new HBaseThread[tablePoolSize];
    
//    for (int i = 0; i < tablePoolSize; ++i) {
//      threads[i] = new HBaseThread(tables[i]);
//      threads[i].start();
//    }
  }
  
  class HBaseThread extends Thread {
    HTable table;
    protected volatile boolean stopped = false;
    
    HBaseThread (HTable t) {
      thread = Thread.currentThread();
      table = t;
      stopped = false;      
    }
    Thread thread;
    
    @Override
    public void run() {
      thread = Thread.currentThread();
      LOG.info("++ Thread started: " + thread.getId() );
      Random i = new Random();
      while (!stopped) {
        //Date tm = new Date(); 
        //Time tt = new Time();
        long start = System.nanoTime();

        Put p = new Put(Bytes.toBytes(i.nextInt(5) + ":" + start));
        p.add(Bytes.toBytes("rt"), Bytes.toBytes("rt"), Bytes.toBytes("++++"));
        try {
          table.put(p);
        } catch (IOException ex) {
          
        }
      }
      
    }
    public void stop(final String msg) {
      this.stopped = true;
      LOG.info("STOPPED: " + msg);
      synchronized (this) {
        // Wakes run() if it is sleeping
        notifyAll();
      }
    }
  }

  /**
   * Column family validity check happens in open(), so we through an
   * IOException (ideally invalid column families would be a
   * IllegalArgumentException but this exn doesn't make sense on open)
   */
  void validateColFams(HTable ht) throws IOException {
    for (QualifierSpec q : spec) {
      String cf = q.colFam;
      HColumnDescriptor hcd = ht.getTableDescriptor().getFamily(cf.getBytes());
      // TODO check hbase semantics
      if (hcd == null) {
        throw new IOException("The column familiy '" + cf
            + "' does not exist in table '" + tableName + "'");
      }
    }
  }
  @Override
  public ReportEvent getMetrics() {
    ReportEvent rpt = super.getMetrics();
    return rpt;
  }

  private class EventHandler implements Runnable {
    final private Put put;
    final private HTablePool pool;
    final private Thread thread;
    
    EventHandler(Put put, HTablePool pool) {
      this.thread = Thread.currentThread();
      this.put = put;
      this.pool = pool;
    }
    
    @Override
    public void run() {
      // 
      try {
        
        //HTableInterface table = pool2[((int)thread.getId()) % 5];
        HTableInterface table = pool3.get(thread.getId());
        if (table == null) {
          pool3.putIfAbsent(thread.getId(), pool.getTable(tableName));
          pool.putTable(table);
        }
        table.put(put);
        //pool.putTable(table);
      } catch (IOException e) {
        LOG.warn("++  " + e.getMessage() );
      }
    }
  }

  public static SinkBuilder builder() {
    return new SinkBuilder() {

      @Override
      public EventSink build(Context context, String... argv) {
        // at least table, row, and one (cf,c,val)
        Preconditions.checkArgument(argv.length >= 2 + 3, USAGE);
        // guarantee table, row plus triples of (cf,c,val)
        Preconditions.checkArgument((argv.length % 3) == 2, USAGE);

        String tableName = argv[0];
        String rowKey = argv[1];

        List<QualifierSpec> spec = new ArrayList<QualifierSpec>();
        for (int i = 2; i < argv.length; i += 3) {
          QualifierSpec qs = new QualifierSpec();
          qs.colFam = argv[i];
          qs.col = argv[i + 1];
          qs.value = argv[i + 2];
          spec.add(qs);
        }

        String bufSzStr = context.getValue(KW_BUFFER_SIZE);
        String isWriteToWal = context.getValue(KW_USE_WAL);
        long bufSz = (bufSzStr == null ? 0 : Long.parseLong(bufSzStr));

        return new ParallelHBaseSink(tableName, rowKey, spec, bufSz,
            Boolean.parseBoolean(isWriteToWal), HBaseConfiguration.create());
      }
    };
  }
  @SuppressWarnings("unchecked")
  public static List<Pair<String, SinkFactory.SinkBuilder>> getSinkBuilders() {
    return Arrays.asList(new Pair<String, SinkFactory.SinkBuilder>("phbase",
        builder()));
  }
}
