(defproject xpusher "0.1.0"
  :description "A Pusher client for coin exchanges based on core.async"
  :url "https://github.com/druids/xpusher"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/core.async "0.4.474"]
                 [cheshire "5.8.0"]
                 [com.pusher/pusher-java-client "1.8.0"]
                 [tol "0.8.0"]]

  :profiles {:dev {:plugins [[lein-cloverage "1.0.10"]
                             [lein-kibit "0.1.6"]
                             [jonase/eastwood "0.2.6"]
                             [venantius/ultra "0.5.2"]]
                   :dependencies [[org.clojure/clojure "1.9.0"]]
                   :source-paths ["src" "dev/src"]}
             :test {:dependencies [[org.clojure/clojure "1.9.0"]
                                   [clj-async-test "0.0.5"]]}}
  :aliases {"coverage" ["with-profile" "test" "cloverage" "--fail-threshold" "75" "-e" "dev|user"]})
