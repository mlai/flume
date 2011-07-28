require 'java'
java_import 'com.cloudera.flume.core.EventSinkDecorator'
java_import 'com.cloudera.flume.core.Event'
java_import 'com.cloudera.flume.core.EventImpl'
class HBasePreprocessDecorator < EventSinkDecorator
  COLUMNS = ['license', 'timestamp', 'rating', 'url']
  def table
    "WCSFullLog"
  end

  def append(e)
    attrs = java::util::HashMap.new e.getAttrs
    attrs["table"] = table.to_java_bytes

    #values = String.from_java_bytes( e.getBody ).scan(/.../)
    pattern = /^([a-zA-Z0-9])+(\t)([0-9])+(\t)(\d{1,3})*(\t)(.)*/
    
    body = String.from_java_bytes e.getBody
    if (body.match(pattern))
      values = String.from_java_bytes( e.getBody ).split("\t")

      values.each_index { |i|
        attrs["2hb_#{COLUMNS[i]}:#{i}"] = values[i].to_java_bytes 
      }
      domain=""
      if (values[3])
        domain=values[3].split('/')[0]
      end
      attrs["2hb_"] = values[1].to_java_bytes + ":" + domain
      super EventImpl.new( String("").to_java_bytes, e.getTimestamp, e.getPriority, e.getNanos,
e.getHost, attrs )
    end
  end
end
HBasePreprocessDecorator.new(nil) 
