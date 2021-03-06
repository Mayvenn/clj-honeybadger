(ns clj-honeybadger.core-test
  (:use clojure.test
        clj-honeybadger.core)
  (:require [clj-http.fake :as fake]
            [cheshire.core :as json]))

(defn fake-endpoint [sent-data request]
  (swap! sent-data conj (-> request :body slurp (json/parse-string true)))
  {:status 200, :headers {}, :body ""})

(defmacro with-fake-endpoint [sent-data & body]
  `(let [~sent-data (atom [])]
     (fake/with-fake-routes {endpoint (partial fake-endpoint ~sent-data)} ~@body)))

(deftest test-send-exception!
  (with-fake-endpoint sent-data
    (send-exception! (Exception. "Something went wrong") {:api-key "XXXXXXX" :env "production"})
    (let [data (first @sent-data)]
      (testing "data exists"
        (is (map? (:notifier data)))
        (is (map? (:error data)))
        (is (map? (:server data))))

      (testing "notifier"
        (is (= "Clojure Honeybadger" (get-in data [:notifier :name])))
        (is (= "java.lang.Exception" (get-in data [:error :class])))
        (is (= "Something went wrong" (get-in data [:error :message])))))))

(deftest test-send-exception!-with-exception-info
  (with-fake-endpoint sent-data
    (send-exception! (ex-info "Something went wrong" {:a 1 :sauce true}) {:api-key "XXXXXXX" :env "production"})
    (let [data (first @sent-data)]
      (testing "data exists"
        (is (map? (:notifier data)))
        (is (map? (:error data)))
        (is (map? (:server data))))

      (testing "notifier"
        (is (= "Clojure Honeybadger" (get-in data [:notifier :name])))
        (is (= "clojure.lang.ExceptionInfo" (get-in data [:error :class])))
        (is (= "Something went wrong -- {:sauce true, :a 1}" (get-in data [:error :message])))))))

(deftest test-returns-exception
  (with-fake-endpoint sent-data
    (let [exception (Exception. "Something went very wrong")]
      (is (= exception (send-exception! exception {:api-key "XXXXXXX"}))))))

(deftest test-skips-sending-in-development
  (with-fake-endpoint sent-data
    (send-exception! (Exception. "Something went wrong") {:api-key "XXXXXXX"})
    (is (empty? @sent-data))))
