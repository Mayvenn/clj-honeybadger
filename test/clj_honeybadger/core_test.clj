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
    (send-exception! {:api-key "XXXXXXX" :env "production"} (Exception. "Something went wrong"))
    (let [data (first @sent-data)]
      (testing "data exists"
        (is (map? (:notifier data)))
        (is (map? (:error data)))
        (is (map? (:server data))))

      (testing "notifier"
        (is (= "Clojure Honeybadger" (get-in data [:notifier :name])))
        (is (= "java.lang.Exception" (get-in data [:error :class])))
        (is (= "Something went wrong" (get-in data [:error :message])))))))

(deftest test-returns-exception
  (with-fake-endpoint sent-data
    (let [exception (Exception. "Something went very wrong")]
      (is (= exception (send-exception! {:api-key "XXXXXXX"} exception))))))

(deftest test-skips-sending-in-development
  (with-fake-endpoint sent-data
    (send-exception! {:api-key "XXXXXXX"} (Exception. "Something went wrong"))
    (is (empty? @sent-data))))

(deftest test-send-exception-with-context
  (with-fake-endpoint sent-data
    (send-exception! {:api-key "XXXXXXX" :env "production"}
                     (Exception. "Something went wrong")
                     {:content-type :json
                      :accept :json
                      :scheme :http
                      :query-params {:corey "awesome"
                                     :belvita "delicious"}
                      :query-string "corey=awesome&belvita=delicious"
                      :headers {"host" "mayvenn-dev.com"}
                      :session "That one"
                      :uri "/honeybadger/test"
                      :clj-honeybadger.core/context {:user_id "1"
                                                     :user_email "sheel@mayvenn.com"}})
    (let [data (first @sent-data)]
      (testing "data exists"
        (is (map? (:notifier data)))
        (is (map? (:error data)))
        (is (map? (:server data)))
        (is (map? (:request data))))

      (testing "notifier"
        (is (= "Clojure Honeybadger" (get-in data [:notifier :name]))))

      (testing "error"
        (is (= "java.lang.Exception" (get-in data [:error :class])))
        (is (= "Something went wrong" (get-in data [:error :message]))))

      (testing "request"
        (is (= "http://mayvenn-dev.com/honeybadger/test?corey=awesome&belvita=delicious"
               (get-in data [:request :url])))
        (is (= "awesome" (get-in data [:request :params :corey])))
        (is (= "delicious" (get-in data [:request :params :belvita])))
        (is (= "That one" (get-in data [:request :session])))
        (is (= {:user_id "1"
                :user_email "sheel@mayvenn.com"}
               (get-in data [:request :context])))))))


