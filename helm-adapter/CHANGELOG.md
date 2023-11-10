## v1.1.1
  - perf: #194 - Avoided using the regex expression and `String.format`
  - perf: #194 - Avoided reading chart file for each method in `TgzArchive`

## v1.0.1
  - dep: #182 - Excluded and removed some dependencies
  - refactor: #113 - Used tokenizer instead of copying index for Helm SDK API (`add` and `remove` methods)
  - refactor: #113 - Used tokenizer instead of copying index for obtaining version of existed charts from index file
  - fix: #180 - Use a safe constructor for `ChartYaml`

## v1.0
  - feat: #141 - Finished implementation of Helm SDK API (`add` and `remove` methods)  
  - BREAKING CHANGE: #110 - Public class `IndexByDirectory` was removed  
  - feat: #115 - Reindex all packages in whole repo  
  - feat: #106 - Added slice for deleting chart by name or by name and version 
  - BREAKING CHANGE: #110 - Public class `IndexMergingSlice` was removed  
