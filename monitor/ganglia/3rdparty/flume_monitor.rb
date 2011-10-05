#!/usr/bin/env ruby


# from https://gist.github.com/1223602

require 'rubygems'
require 'getoptlong'
require 'socket'
require 'json'
require 'timeout'
require 'open-uri'

host = "localhost"
port = 35862
node = "localhost"

def usage(port)
  puts
  puts "flume_monitor.rb [options]"
  puts "options:"
  puts "    -h <hostname>   connect to another host (default: localhost)"
  puts "    -p <port>       connect to another port (default: #{port})"
  puts "    -n <node>       logical node to check"
  puts
end

opts = GetoptLong.new(
  [ '--help', GetoptLong::NO_ARGUMENT ],
  [ '-h', GetoptLong::OPTIONAL_ARGUMENT ],
  [ '-p', GetoptLong::REQUIRED_ARGUMENT ],
  [ '-n', GetoptLong::REQUIRED_ARGUMENT ]
)

opts.each do |opt, arg|
  case opt
  when '--help'
    usage(port)
    exit 0
  when '-h'
    host = arg
  when '-p'
    port = arg.to_i
  when '-n'
    node = arg
  end
end

metrics = Hash.new(0)

Timeout::timeout(10) do
  begin
    url = "http://#{host}:#{port}/node/reports/#{node}"
    data = open("#{url}").read
  rescue
    #puts "Could not connect #{host}"
    exit 1
  end
  stats = JSON.parse(data)

  stats.each do |k,v|
    v = 0 if v.nil?
    puts "#{k} = #{v}" 
  end
end

