(ns ^{:author "Daniel Leong"
      :doc "Sink implementation using nyartcc.org resources"}
  asfiled.nyartcc-sink
  (:require [clojure.string :refer [trim]]
            [org.httpkit.client :as http] 
            [hickory
             [core :refer [parse-fragment as-hickory]]
             [select :as s]]
            [asfiled.sink :as snk :refer [Sink]]))

(def url-ais "http://nyartcc.org/aacisa")

(defn- aviation-search 
  "Fire up the AIS, Aviation Information Search.
  Returns the html result parsed as as-hickory"
  [query]
  (let [options {:form-params {:s query}}
        {:keys [err body]} @(http/post url-ais options)]
    (if err
      nil
      (-> body parse as-hickory))))

(defn- aviation-table
  "Convenience around aviation-search; filters down
  to the table immediately following the header with
  the provided title"
  [query header-title]
  (when-let [tree (aviation-search query)]
    (-> (s/select (s/follow-adjacent 
                    (s/and
                      (s/tag :h3)
                      (s/find-in-text (re-pattern header-title)))
                    (s/tag :table))
                  tree)
        first)))

(defn- aviation-row
  "Convenience when you only need the contents of the first
  row of aviation-table"
  [query header-title]
  (when-let [table (aviation-table query header-title)]
    (->> (s/select 
           (s/child 
             (s/nth-child 2) 
             (s/tag :tr)
             (s/tag :td))
           table)
         (map #(-> % :content first trim)))))

(deftype NyArtccSink [my-icao]
  Sink
  (get-aircraft [this aircraft]
    (when-let [row (aviation-row aircraft "Aircraft Results")]
      (zipmap
        [:model :manufacturer :type :engines :weight] 
        (rest row))))
  (get-airline [this airline]
    (when-let [row (aviation-row airline "Airline Results")]
      (zipmap
        [:telephony :name] 
        (rest row))))
  (get-airport [this icao]
    (when-let [row (aviation-row icao "Airport Results")]
      (zipmap
        [:icao :name] 
        row)))
  (get-preferred-routes [this from to]
    [{:route "PARKE HYPER#"
      :area ""
      :aircraft "RNAV"
      :alititude "FL180-FL220"
      :preferred true }]))
