## Scripting support

Artipie provides custom scripting support. It allows running custom logic server-side without Artipie source code modifications.
Artipie relies on the JVM scripting engine for this functionality.

### Configuration

To run the script, add `crontab` section to Artipie main configuration file, then add the script as a key/value pair:
```
meta:
...
  crontab: 
    - path: path/to/script1.groovy
      cronexp: */3 * * * * ?
```
`cronexp` value here means 'every 3 minutes'. The value must be in crontab [Quartz definition format](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)

### File extensions

Scrips must have a file extension corresponding to one of the supported scripting languages.

| Scripting language | File extension |
|--------------------|----------------|
| Groovy             | .groovy        |
| Mvel               | .mvel          |
| Python             | .py            |
| Ruby               | .rb            |

### Accessing Artipie objects

Some Artipie objects could be accessed from the scripts. Such objects have names starting with underscore `_`.
The table with available objects is given below.

| Object name       | Artipie type                                                                                                                                       |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `_settings`       | [com.artipie.settings.Settings](https://github.com/artipie/artipie/blob/master/src/main/java/com/artipie/settings/Settings.java)                   |
| `_repositories`   | [com.artipie.settings.repo.Repositories](https://github.com/artipie/artipie/blob/master/src/main/java/com/artipie/settings/repo/Repositories.java) |

Groovy snippet using Artipie `_repositories` objects, example:
```groovy
File file = new File('/my-repo/info/cfg.log')
cfg = _repositories.config('my-repo').toCompletableFuture().join()
file.write cfg.toString()
```
