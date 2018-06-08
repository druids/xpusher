(ns xpusher-test.async-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [cheshire.core :as cheshire]
    [xpusher.async :as xpusher]
    [clj-async-test.core :refer :all])
  (:import
    com.pusher.client.Pusher
    com.pusher.client.connection.ConnectionState
    com.pusher.client.connection.ConnectionStateChange))


(definterface Client
  (connect [])
  (connect [listener _])
  (disconnect [])
  (subscribe [channel-name]))


(definterface Channel
  (bind [event-name listener]))


(defn- pusher-test
  [input output str-big-decimals?]
  (let [pusher-class (reify Client
                       (connect [this])

                       (connect [this listener _]
                         (.onConnectionStateChange listener (ConnectionStateChange. ConnectionState/CONNECTING
                                                                                    ConnectionState/CONNECTED)))
                       (disconnect [this])

                       (subscribe [this channel-name]
                         (reify Channel
                           (bind [this event-name listener]
                             (.onEvent listener
                                       channel-name
                                       "data"
                                       (cheshire/generate-string input))))))
        [pusher pusher-channel status-ch data-ch]
        (xpusher/pusher-factory pusher-class
                                (partial xpusher/bitstamp-coerce-orderbook str-big-decimals?)
                                {:channel-name "order_book_btceur"
                                 :event-name "data"
                                 :data-buffer-or-n 1
                                 :status-buffer-or-n 16})]
    (let [[_ event-name data] (async/<!! data-ch)]
      (is (eventually (= :data event-name)))
      (is (eventually (= output data)))
      (is (eventually (= [:change {:current :connected, :previous :connecting}] (async/<!! status-ch))))
      (xpusher/disconnect pusher)
      (xpusher/connect pusher))))


(deftest new-pusher-test
  (doseq [[input output str-big-decimals?] [[{:bids [[5555.0 4.0702]], :timestamp "1527842633"}
                                             {:bids [[5555.0M 4.0702M]], :timestamp 1527842633}
                                             false]
                                            [{:bids [["5555.00" "4.0702"]], :timestamp "1527842633"}
                                             {:bids [[5555.00M 4.0702M]], :timestamp 1527842633}
                                             true]
                                            [{:bids [["5555.00" "4.0702"]], :timestamp "1527842633"}
                                             {:bids [["5555.00" "4.0702"]], :timestamp 1527842633}
                                             false]]]
    (pusher-test input output str-big-decimals?)))


(deftest subscription-listener-test
  (let [assert-event (fn [expected-channel expected-event expected-data channel-name event-name data]
                       (is (= expected-channel channel-name))
                       (is (= expected-event event-name))
                       (is (= expected-data data)))]
    (doseq [[expected-channel expected-event expected-data channel-name event-name data str-big-decimals?]
            [["bar" :foo {:asks [[6480.99M 2.79500000M]], :bids [[6480.99M 2.79500000M]], :timestamp 1527842633}
              "bar" "foo" (str "{\"timestamp\": \"1527842633\", \"bids\": [[\"6480.99\", \"2.79500000\"]],"
                               "\"asks\": [[\"6480.99\", \"2.79500000\"]]}") true]
             ["bar" :foo {:asks [["6480.99" "2.79500000"]], :bids [["6480.99" "2.79500000"]], :timestamp 1527842633}
              "bar" "foo" (str "{\"timestamp\": \"1527842633\", \"bids\": [[\"6480.99\", \"2.79500000\"]],"
                               "\"asks\": [[\"6480.99\", \"2.79500000\"]]}") false]]]
      (.onEvent (xpusher/subscription-listener (partial assert-event expected-channel expected-event expected-data)
                                               (partial xpusher/bitstamp-coerce-orderbook str-big-decimals?))
                channel-name
                event-name
                data))))
