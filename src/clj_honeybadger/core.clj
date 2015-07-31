(ns clj-honeybadger.core
  (:require [clj-stacktrace.core :as st]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]))

(def endpoint
  "https://api.honeybadger.io/v1/notices")

(def hostname
  (.getHostName (java.net.InetAddress/getLocalHost)))

(def current-dir
  (.getCanonicalPath (io/file ".")))

(defn honeybadger-map [ex options]
  {:notifier
   {:name "Clojure Honeybadger"
    :url "https://github.com/mayvenn/clj-honeybadger"
    :version "0.3.1"}

   :error
   {:class     (.getName (:class ex))
    :message   (:message ex)
    :backtrace (for [trace (:trace-elems ex)]
                 {:number (:line trace)
                  :file   (:file trace)
                  :method (or (:method trace) (:fn trace))})}

   :server
   {:project_root     {:path current-dir}
    :environment_name (:env options)
    :hostname         hostname}})

(defn send-exception! [ex options]
  (when-not (= (:env options "development") "development")
    (http/post endpoint {:content-type :json
                         :accept :json
                         :headers {"X-API-Key" (:api-key options)}
                         :body (json/generate-string
                                (honeybadger-map (st/parse-exception ex) options))}))
  ex)
