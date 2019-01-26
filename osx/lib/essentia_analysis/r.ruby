#!/usr/bin/env ruby

def otool(path)
  `otool -L "#{path}"`.split("\n").grep(/compatibility version/).map { |l| l.strip.scan(/(.*?) \(compatibility version/)[0][0] }
end

def recurse(starting_point, found = {})
  otool(starting_point).each do |shared_lib|
    if !found[shared_lib]
      found[shared_lib] = true
      recurse(shared_lib, found)
    end
  end

  found
end

found = recurse(ARGV[0])
puts found.keys.join("\n")
