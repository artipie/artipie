# The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
# https://github.com/artipie/artipie/blob/master/LICENSE.txt

require 'rubygems/package'
require 'time'
require 'tmpdir'
require 'java'

class Dependencies
    def self.dependencies(gems)
        resgems = []
        gems.each do |gem|
            puts(gem)
            resdep = []
            spec = Gem::Package.new(gem).spec
            deps = spec.dependencies
            deps.each do |item|
                if item.type == :runtime
                    resdep.append([item.name, item.requirements_list()[0]])
                end
            end
            resgems.append({:name => spec.name, :number=>spec.version.version, :platform=>spec.original_platform, :dependencies=>resdep})
        end
        return Marshal.dump(resgems)
    end
end