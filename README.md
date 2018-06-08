xpusher
=======

A Pusher client for coin exchanges based on [core.async](https://github.com/clojure/core.async).

[![CircleCI](https://circleci.com/gh/druids/xpusher.svg?style=svg)](https://circleci.com/gh/druids/xpusher)
[![Dependencies Status](https://jarkeeper.com/druids/xpusher/status.png)](https://jarkeeper.com/druids/xpusher)
[![License](https://img.shields.io/badge/MIT-Clause-blue.svg)](https://opensource.org/licenses/MIT)


Leiningen/Boot
--------------

```clojure
[xpusher "0.0.0"]
```

Documentation
-------------

This library is a thin wrapper around `com.pusher/pusher-java-client` within a simple usage. A function `new-pusher`
 opens a new connection with the Pusher server, subscribes a requested channel and returns a following tuple
`[pusher pusher-channel status-ch data-ch]` where

- `pusher` a Pusher instance
- `pusher` a subscribed Channel instance
- `status-ch` an async channel containing a pusher and channel messages as a tuple `[action data]`
- `data-ch` an async channel containing data for the subscribed channel as a tuple `[channel-name event-name data]`

The function takes an option object:
- `channel-name` a channel name to subscribe, required
- `pusher-key` a Pusher key, default de504dc5763aeef9ff52, optional
- `event-name` an event name to bind on the subscribed channel, optional
- `status-buffer-or-n` a buffer-or-n for the status channel, optional
- `data-buffer-or-n` a buffer-or-n for the data channel, optional
- `str-big-decimals?` when `true` coerce a volume and price (in strings) as `BigDecimal`, default `true`

Data channel returns a tuple of channel-name, event-name (as `keyword`), and data.

Status channel returns a tuple of event-name (as `keyword`), and data. Expected events:

- `:change`
- `:connecting`
- `:connected`
- `:disconnecting`
- `:disconnected`


### Bitstamp

```clojure
(require '[clojure.core.async :as async])
(require '[xpusher.async :as xpusher])

(let [[pusher pusher-channel status-ch data-ch]
      (xpusher/new-bitstamp-pusher {:channel-name "order_book_btceur" ;; required
                                    :event-name "data" ;; required
                                    :data-buffer-or-n (async/sliding-buffer 16) ;; optinal
                                    :status-buffer-or-n 16})] ;; optional
  (async/go-loop []
                 (let [[channel-name event-name data] (async/<! data-ch)]
                   (println channel-name event-name data)) ;; <-- put you logic here
                 (recur))
  (async/go-loop []
                 (let [[event-name data] (async/<! status-ch)]
                   (println event-name data)) ;; <-- put status handler here
                 (recur)))
```

An established connection can be disconnected by

```clojure
(xpusher/disconnect pusher)
```

and again connected within same `pusher` instance

```clojure
(xpusher/connect pusher)
```


### Coinmate

```clojure
(require '[clojure.core.async :as async])
(require '[xpusher.async :as xpusher])

(let [[pusher pusher-channel status-ch data-ch]
      (xpusher/new-coinmate-pusher {:channel-name "order_book-BTC_EUR" ;; required
                                    :event-name "order_book" ;; required
                                    :data-buffer-or-n (async/sliding-buffer 16) ;; optinal
                                    :status-buffer-or-n 16})] ;; optional
  (async/go-loop []
                 (let [[channel-name event-name data] (async/<! data-ch)]
                   (println channel-name event-name data)) ;; <-- put you logic here
                 (recur))
  (async/go-loop []
                 (let [[event-name data] (async/<! status-ch)]
                   (println event-name data)) ;; <-- put status handler here
                 (recur)))
```


#### Notes

- floating-point numbers are parsed as `BigDecimal`s
