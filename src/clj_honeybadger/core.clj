(ns clj-honeybadger.core
  (:require [clj-stacktrace.core :as st]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [ring.util.request :as req]))

(def endpoint
  "https://api.honeybadger.io/v1/notices")

(def hostname
  (.getHostName (java.net.InetAddress/getLocalHost)))

(def current-dir
  (.getCanonicalPath (io/file ".")))

(defn honeybadger-map
  ([ex options]
   {:notifier
    {:name "Clojure Honeybadger"
     :url "mayvenn.com"
     :version "1.0.0"}

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
  ([ex options request]
   (merge (honeybadger-map ex options)
          {:request {:url     (req/request-url request)
                     :params  (merge (:query-params request)
                                     (:form-params request))
                     :session (:session request)
                     :context (::context request)}})))

(defn send-exception! [config ex & [ctx]]
  (let [mapper (partial honeybadger-map (st/parse-exception ex) config)]
    (when-not (= (:env config "development") "development")
      (http/post endpoint {:content-type :json
                           :accept :json
                           :headers {"X-API-Key" (:api-key config)}
                           :body (json/generate-string (if ctx (mapper ctx) (mapper)))}))
    ex))
