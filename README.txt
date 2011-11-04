Watches a directory of ClojureScript (.cljs) files and Clojure (.clj) macros, and
recompiles them whenever a change is detected.

This is what cljs-watch does (https://github.com/ibdknox/cljs-watch), but with the
following differences:

 * Uses jpathwatch (http://sourceforge.net/projects/jpathwatch) to monitor changes
   to files

 * Expects Clojure files to be in the same source tree as ClojureScript files, e.g.:
     src/
       my_ns/
         core.cljs
         my_macros.clj
     
Installation:

(Note: once the JDK file watch API is standardised, the jpathwatch dependency can
be removed.)

 1. Download jpathwatch-0.94.jar from http://sourceforge.net/projects/jpathwatch
 2. Install jpathwatch to your local Maven repository:

      $ mvn install:install-file    \
            -Dfile=/path/to/jar     \
            -DgroupId=jpathwatch    \
            -DartifactId=jpathwatch \
            -Dversion=0.94
            -Dpackaging=jar

 3. Put bin/cljsc-auto on your $PATH.
 4. Ensure $CLOJURESCRIPT_HOME is set.

Usage:

 $ cljsc-auto src/ >foo.js
