(ns ^{:author "Daniel Leong"
      :doc "Vatsim interactions"}
  asfiled.vatsim
  (:require [clojure.string :refer [split-lines split lower-case]]
            [org.httpkit.client :as http] 
            [clj-time.core :as t]))

;;;
;;; Constants
;;;

(def status-url "http://status.vatsim.net")

;; this *should* be loaded from status-url, but....
(def metar-url "http://metar.vatsim.net/metar.php")

;; url type for only servers
(def server-url-type :url1)

;;;
;;; Globals
;;;

(def data-urls (atom false))
(def data-cache-expires (atom (t/now)))
(def data-cache (atom {}))

;;
;; Functions
;;

(defn into-seq-map
  [pairs]
  (let [k (keyword (-> pairs first first))]
    {k (map second pairs)}))

(defn load-data-urls
  "Load the list of data urls. No caching"
  [& _]
  (let [{:keys [error body]} @(http/get status-url)]
    (if error
      []
      (->> (split-lines body)
           (filter #(.startsWith % "url"))
           (map #(split % #"="))
           (split-with #(= "url0" (first %)))
           (map into-seq-map)
           (apply merge)))))

(defn clear-data-cache
  "Clear data cache; urls are kept"
  []
  (swap! data-cache-expires (constantly (t/now)))
  (swap! data-cache (constantly {})))

(defn clear-data-urls-cache
  "Clear cached urls"
  []
  (swap! data-urls (constantly false)))

(defn get-data-urls
  "Get the list of data urls, cached. You may pass
  the optional key :type to specify which type of
  url; if you only need the server names, for example,
  you should call as :type :server"
  [& {:keys [type]
      :or {type nil}}]
  (let [cached @data-urls]
    (let [all-urls (if cached
                     cached
                     (swap! data-urls load-data-urls))]
      (case type
        :server (get all-urls server-url-type)
        :url0 (get all-urls type)
        :url1 (get all-urls type)
        nil (get all-urls :url0)))))

(defn load-data
  "Load the raw vatsim data from a random data url"
  [& {:keys [only-server?]
      :or [only-server? false]}]
  (let [url-type (when only-server?
                   :server)
        urls (get-data-urls :type url-type)
        url (rand-nth urls)
        {:keys [error body]} @(http/get url)]
    (if error
      (throw (IllegalStateException. error))
      body)))

(defn parse-client-data
  "Parse a raw line of client data into a nice map"
  [raw]
  (let [parts (split raw #":")]
    {:callsign (first parts)
     :craft (nth parts 9)
     :depart (nth parts 11)
     :cruise (try
               (Integer/parseInt (nth parts 12))
               (catch NumberFormatException e (nth parts 12)))
     :arrive (nth parts 13)
     :remarks (nth parts 29)
     :route (nth parts 30)
     }))

(defn parse-server-data
  "Parse a raw line of client data into a nice map"
  [raw]
  (let [parts (split raw #":")]
    (zipmap
      [:id :ip :location :name]
      parts)))

(defn- parse-data-line
  [dict line]
  (cond
    ;; mode line
    (.startsWith line "!")
    [[:mode-] (.substring line 
                          0
                          (.indexOf line ":"))]
    ;; data line
    (not (or (empty? line) (.startsWith line ";")))
    (case (:mode- dict)
      "!GENERAL" 
      (let [[k v] (split line #" = ")
            key-symbol (-> k
                           lower-case
                           (.replace " " "-"))]
        [[(keyword key-symbol)] v])
      "!CLIENTS"
      (let [client (parse-client-data line)]
        [[:clients (:callsign client)] client])
      "!PREFILE"
      (let [client (parse-client-data line)]
        [[:prefile (:callsign client)] client])
      "!SERVERS"
      (let [server (parse-server-data line)]
        [[:servers (:id server)] server])
      ;; unhandled section
      nil)))

(defn parse-data
  "Parse raw data into a usable map"
  [data]
  (loop [lines (split-lines data)
         dict {}]
    (if (empty? lines)
      dict
      (if-let [[k v] (parse-data-line dict (first lines))]
        ; success!
        (recur (rest lines)
               (assoc-in dict k v))
        ; empty line
        (recur (rest lines)
               dict)))))

(defn- update-data
  "Ensure we have data cached and up-to-date"
  [& {:keys [only-server?]
      :or [only-server? false]}]
  (let [cache-expires @data-cache-expires
        cache-data @data-cache]
    (if (-> cache-expires (t/after? (t/now)))
      ;; data valid!
      cache-data
      (let [raw (load-data :only-server? only-server?)
            parsed (parse-data raw)]
        ; TODO use the "reload" general setting for expiry?
        (swap! data-cache-expires 
               (constantly (t/plus (t/now) (t/minutes 2))))
        (swap! data-cache (constantly parsed))))))

(defn get-aircraft
  "Load an aircraft by its callsign.
  The aircraft data may be prefile OR online.
  Cached data will be used when possible, and the
  cache will be updated as appropriate."
  [callsign]
  (when-let [data (update-data)]
    (or
      (get-in data [:clients callsign])
      (get-in data [:prefile callsign]))))

(defn get-servers
  "Load a seq of servers sorted by id. Cached
  data will be used when possible; we will NOT
  attempt to refresh cache, even if it's old,
  as long as we have it; the servers do not
  change frequently enough to necessitate the overhead.
  Note that you may pass the keyword param `:only? true`
  if you don't need any other data."
  [& {:keys [only?]
      :or [only? false]}]
  (when-let [data (update-data :only-server? only?)]
    (when-let [servers (seq (vals (:servers data)))]
      (sort-by :id servers))))

(defn load-metar
  "Load the metar for an airport"
  [icao]
  (let [{:keys [error body]} @(http/get (str metar-url "?id=" icao))]
    (if error
      ""
      (.trim body))))


