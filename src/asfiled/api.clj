(ns ^{:author "Daniel Leong"
      :doc "Public API for programmatic access to asFiled"}
  asfiled.api
  (:require [asfiled.sink :as snk :refer [Sink]]))

;;
;; Constants
;;

(def equipment-suffixes
  {"/W" {:rvsm true, :transponder :mode-c, :nav "No RNAV, No GNSS"}
   "/Z" {:rvsm true, :transponder :mode-c, :nav "RNAV, No GNSS"}
   "/L" {:rvsm true, :transponder :mode-c, :nav "GNSS"}
   ;
   "/X" {:nav "No DME", :transponder :none}
   "/T" {:nav "No DME", :transponder :no-mode-c}
   "/U" {:nav "No DME", :transponder :mode-c}
   ;
   "/D" {:nav "DME", :transponder :none}
   "/B" {:nav "DME", :transponder :no-mode-c}
   "/A" {:nav "DME", :transponder :mode-c}
   ;
   "/M" {:nav "TACAN", :transponder :none}
   "/N" {:nav "TACAN", :transponder :no-mode-c}
   "/P" {:nav "TACAN", :transponder :mode-c}
   ;
   "/Y" {:nav "RNAV, No GNSS", :transponder :none}
   "/C" {:nav "RNAV, No GNSS", :transponder :no-mode-c}
   "/I" {:nav "RNAV, No GNSS", :transponder :mode-c}
   ;
   "/V" {:nav "GNSS", :transponder :none}
   "/S" {:nav "GNSS", :transponder :no-mode-c}
   "/G" {:nav "GNSS", :transponder :mode-c}})

(def aircraft-type-regex #"(.{1,3}?/)?([^/]+)(/.+)?")
(def aircraft-type-regex-offset 2)

(def cached-airlines (atom {}))
(def cached-aircraft-types (atom {}))
(def cached-airports (atom {}))
(def cached-routes (atom {}))

;;
;; Util functions
;;

(defn airline-name [client]
  (re-find #"[a-zA-Z]*" (:callsign client)))

(defn aircraft-type [client]
  (nth 
    (re-find aircraft-type-regex (:craft client))
    aircraft-type-regex-offset))

(defn equipment-type [client]
  (when-let [equip (last (re-find aircraft-type-regex (:craft client)))]
    (get equipment-suffixes equip)))

;;; These cached-* functions also wrap the appropriate
;;;  call and make it a future, so we can evaluate everything
;;;  in parallel

(defn- cached-airline [sink airline]
  (future
    (let [cache @cached-airlines]
      (if-let [cached (get cache airline)]
        ;; easy, already cached!
        cached
        ;; bummer; fetch and cache
        (let [resolved (snk/get-airline sink airline)]
          (swap! cached-airlines #(assoc % airline resolved))
          resolved)))))

(defn- cached-aircraft [sink aircraft]
  (future
    (let [cache @cached-aircraft-types]
      (if-let [cached (get cache aircraft)]
        ;; easy, already cached!
        cached
        ;; bummer; fetch and cache
        (let [resolved (snk/get-aircraft sink aircraft)]
          (swap! cached-aircraft-types #(assoc % aircraft resolved))
          resolved)))))

(defn- cached-airport [sink icao]
  (future
    (let [cache @cached-airports]
      (if-let [cached (get cache icao)]
        ;; easy, already cached!
        cached
        ;; bummer; fetch and cache
        (let [resolved (snk/get-airport sink icao)]
          (swap! cached-airports #(assoc % icao resolved))
          resolved)))))

(defn- cached-preferred-routes [sink from to]
  (future
    (let [cache @cached-routes
          k (str from "-" to)]
      (if-let [cached (get cache k)]
        ;; easy, already cached!
        cached
        ;; bummer; fetch and cache
        (let [resolved (snk/get-preferred-routes sink from to)]
          (swap! cached-routes #(assoc % k resolved))
          resolved)))))

;;
;; Main public API
;;

(defn analyze
  "Analyze the given client data. It should be a map resembling:
  {:callsign 'ACA123'
  :craft 'A321/L'
  :depart 'KLGA'
  :arrive 'KIAD'
  :exits {:bearing 238 :gate :west :exits ['PARKE' 'LANNA']}
  :route 'Route'} ; optional"
  [sink client]
  {:pre [(satisfies? Sink sink)]}
  (let [airline (cached-airline sink (airline-name client))
        craft (cached-aircraft sink (aircraft-type client))
        ;; presumably you know the departure airport...
        arrive (cached-airport sink (:arrive client))
        exits (future (snk/get-valid-exits sink (:arrive client))) ;; should we cache this?
        preferred-routes (cached-preferred-routes
                           sink 
                           (:depart client)
                           (:arrive client))
        equip (equipment-type client)]
    {:airline @airline
     :craft @craft
     :exits @exits
     :preferred-routes @preferred-routes
     :arrive @arrive
     :equip equip}))

