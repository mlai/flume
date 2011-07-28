# FullLogAgentDecorator.rb
require 'java'
java_import 'com.cloudera.flume.core.EventSinkDecorator'
java_import 'com.cloudera.flume.core.Event'
java_import 'com.cloudera.flume.core.EventImpl'

# Full log decorator.
# 
# Fields:
# AgentIP\tLicenseType\tTimeStamp\tRating\tURL
# 
# A sample log record:
# 93.186.135.116 TRECSM70  1304406740  90  www.n-rjya.it/80//DSC_1312.jp
#
# It filters some specified fields from the passed events: agent ip.
class FullLogAgentDecorator < EventSinkDecorator
  def append(e)
    pattern = /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})\t([a-zA-Z0-9])+\t([0-9])+\t(\d{1,3})*\t(.)*/
    
    body = String.from_java_bytes e.getBody
    newBody = ""
    if (pattern.match(body)) 
      substrings = body.split(/\t/)
      newBody = substrings[1] + "\t" + substrings[2] + "\t" + substrings[3] +
        "\t" + substrings[4]
      super EventImpl.new(newBody.to_java_bytes)
    end
  end
end

FullLogAgentDecorator.new(nil)

