/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.cloudera.flume.conf.avro;

@SuppressWarnings("all")
public class AvroFlumeConfigDataMap extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"AvroFlumeConfigDataMap\",\"namespace\":\"com.cloudera.flume.conf.avro\",\"fields\":[{\"name\":\"configs\",\"type\":{\"type\":\"map\",\"values\":{\"type\":\"record\",\"name\":\"AvroFlumeConfigData\",\"fields\":[{\"name\":\"timestamp\",\"type\":\"long\"},{\"name\":\"sourceConfig\",\"type\":\"string\"},{\"name\":\"sinkConfig\",\"type\":\"string\"},{\"name\":\"sourceVersion\",\"type\":\"long\"},{\"name\":\"sinkVersion\",\"type\":\"long\"},{\"name\":\"flowID\",\"type\":\"string\"}]}}}]}");
  public java.util.Map<java.lang.CharSequence,com.cloudera.flume.conf.avro.AvroFlumeConfigData> configs;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return configs;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: configs = (java.util.Map<java.lang.CharSequence,com.cloudera.flume.conf.avro.AvroFlumeConfigData>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}
