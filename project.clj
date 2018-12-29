(defproject bcbio.run "0.0.6"
  :description "Idempotent, transactional runs of external command line programs."
  :url "http://github.com/chapmanb/bcbio.run"
  :license {:name "MIT" :url "http://www.opensource.org/licenses/mit-license.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.codehaus.jsr166-mirror/jsr166y "1.7.0"] ;; reducers support for Java 1.6
                 [org.clojure/core.incubator "0.1.4"]
                 [amalloy/ring-buffer "1.3.0"]
                 [org.clojure/tools.reader "1.3.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [me.raynes/fs "1.4.6"]]
  :plugins [[lein-midje "3.2.1"]]
  :profiles {:dev {:dependencies
                   [[midje "1.9.4"]]}})
