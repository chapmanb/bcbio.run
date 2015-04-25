(defproject bcbio.run "0.0.5"
  :description "Idempotent, transactional runs of external command line programs."
  :url "http://github.com/chapmanb/bcbio.run"
  :license {:name "MIT" :url "http://www.opensource.org/licenses/mit-license.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.codehaus.jsr166-mirror/jsr166y "1.7.0"] ;; reducers support for Java 1.6
                 [org.clojure/core.incubator "0.1.3"]
                 [amalloy/ring-buffer "1.0"]
                 [com.taoensso/timbre "3.0.0-RC2"]
                 [me.raynes/fs "1.4.6"]]
  :plugins [[lein-midje "3.1.3"]]
  :profiles {:dev {:dependencies
                   [[midje "1.6.0"]]}})
