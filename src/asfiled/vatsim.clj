(ns ^{:author "Daniel Leong"
      :doc "Vatsim interactions"}
  asfiled.vatsim
  (:require [org.httpkit.client :as http]
            [clojure.string :refer [split-lines]]
            ))

;;;
;;; Constants
;;;

(def status-url "http://status.vatsim.net/status.txt")

;; this *should* be loaded from status-url, but....
(def metar-url "http://metar.vatsim.net/metar.php")


;;;
;;; Globals
;;;

(def data-urls (atom false))

;;
;; Functions
;;

(defn- load-data-urls
  "Load the list of data urls. No caching"
  [& _]
  (let [{:keys [error body]} @(http/get status-url)
        url-start (.length "url0=")
        ]
    (if error
      []
      (->> (split-lines body)
           (filter #(.startsWith % "url0"))
           (map #(.substring % url-start))))))

(defn get-data-urls
  "Get the list of data urls, cached."
  []
  (let [cached @data-urls]
    (if cached
      cached
      (swap! data-urls load-data-urls))))

(defn load-data
  "Load the raw vatsim data from a random data url"
  []
  (let [urls (get-data-urls)
        url (rand-nth urls)
        {:keys [error body]} @(http/get url)]
    (if error
      (throw (IllegalStateException. error))
      body)))

(defn load-metar
  "Load the metar for an airport"
  [icao]
  (let [{:keys [error body]} @(http/get (str metar-url "?id=" icao))]
    (if error
      ""
      (.trim body))))


