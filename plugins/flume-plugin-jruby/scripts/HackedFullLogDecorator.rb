# FullLogDecorator.rb
require 'java'
java_import 'com.cloudera.flume.core.EventSinkDecorator'
java_import 'com.cloudera.flume.core.Event'
java_import 'com.cloudera.flume.core.EventImpl'

java_import 'org.slf4j.Logger'
java_import 'org.slf4j.LoggerFactory'

# Full log decorator: changed the timestamp to use current time. 
# This decorator is for performance test purpose. 
class HackedFullLogDecorator < EventSinkDecorator
  
  # @@log = LoggerFactory.getLogger("FullLogAgentDecorator")
  
  def getReverseDomainName(domainName)
    ret = ""
    substring = domainName.split(".")
    substring = substring.reverse
    substring.each_with_index { |y, x| 
      ret = (x == substring.size - 1) ? (ret + y.to_s) : (ret + y.to_s + ".")
    }
    ret
  end
  
  def getDateTimeByTimeStamp(ts)
    t = Time.at(ts)
    return t.year.to_s + "%02d" % t.month.to_s + "%02d" % t.day.to_s + 
      "%02d" % t.hour.to_s + "%02d" % t.min.to_s + "%02d" % t.sec.to_s
  end
  
  def append(e)
    body = String.from_java_bytes e.getBody
    pattern  = /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})\t([a-zA-Z0-9]+)\t([0-9]+)\t([0-9, ]*)\t(.*)/
    if (pattern.match(body)) 
      substrings = body.split(/\t/)
      
      newEvent = EventImpl.new("".to_java_bytes)
      newEvent.set("lic", substrings[1].to_java_bytes)
      newTS = Time.at(Time.now()) - Time.at(0)
      #newEvent.set("ts", substrings[2].to_java_bytes)
      newEvent.set("ts", newTS.to_s.to_java_bytes)
      newEvent.set("rating", substrings[3].to_java_bytes)
      
      urlsubstring = substrings[4].split("/")
      
      newEvent.set("url", substrings[4].to_java_bytes)
      newEvent.set("key", (getDateTimeByTimeStamp(newTS) + ":" + 
        getReverseDomainName(urlsubstring[0].split(":")[0])).to_java_bytes)
      super newEvent
    else
      super e  
    end
  end
end

HackedFullLogDecorator.new(nil)
