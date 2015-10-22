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

(defn ex-info? [ex]
  (instance? clojure.lang.ExceptionInfo ex))

(defn parse-exception
  "Parses execption data for Honeybadger. If an ex-info exception is modified
  from the original exception and rethrown, it can attach :rethrown to its map
  and this function will, instead, parse its parent exception, preserving the
  stack trace."
  [ex]
  (if (and (ex-info? ex)
           (:rethrown (ex-data ex)))
    (st/parse-exception (.getCause ex))
    (st/parse-exception ex)))

(defn honeybadger-map [ex options]
  (let [parsed-ex (parse-exception ex)]
    {:notifier
     {:name "Clojure Honeybadger"
      :url "https://github.com/mayvenn/clj-honeybadger"
      :version "0.3.1"}

     :error
     {:class     (.getName (:class parsed-ex))
      :message   (if (ex-info? ex)
                   (str (:message parsed-ex) " -- " (pr-str (.getData ex)))
                   (:message parsed-ex))
      :backtrace (for [trace (:trace-elems parsed-ex)]
                   {:number (:line trace)
                    :file   (:file trace)
                    :method (or (:method trace) (:fn trace))})}

     :server
     {:project_root     {:path current-dir}
      :environment_name (:env options)
      :hostname         hostname}}))

(defn send-exception! [ex options]
  (when-not (= (:env options "development") "development")
    (http/post endpoint {:content-type :json
                         :accept :json
                         :headers {"X-API-Key" (:api-key options)}
                         :body (json/generate-string
                                (honeybadger-map ex options))}))
  ex)
