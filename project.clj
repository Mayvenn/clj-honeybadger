(defproject clj-honeybadger "0.2.1-SNAPSHOT"
  :description "Sends exceptions to Honeybadger"
  :url "https://github.com/Mayvenn/clj-honeybadger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.4.0"]
                 [clj-http "1.0.1"]
                 [clj-stacktrace "0.2.8"]]
  
                              
                              
                              
  :plugins [[s3-wagon-private "1.1.2"]]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[pjstadig/humane-test-output "0.6.0"]
                        [org.clojure/tools.namespace "0.2.9"]
                        [clj-http-fake "1.0.1"]]
         :injections [(require 'pjstadig.humane-test-output)
                      (pjstadig.humane-test-output/activate!)]}})
