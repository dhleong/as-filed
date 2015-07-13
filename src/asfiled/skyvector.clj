(ns ^{:author "Daniel Leong"
      :doc "Utility methods using skyvector.com"}
  asfiled.skyvector
  (:require [org.httpkit.client :as http] 
            [cheshire.core :refer [parse-string]]))

(def calculate-url "http://skyvector.com/api/dataLayer")

(defn- calculate-path
  [from to]
  (let [path (str from " " to)
        options {:query-params {:cmd "planPts" :d path}}
        {:keys [err body]} @(http/get calculate-url options)]
    (when-let [json (parse-string body true)]
      (-> json
          :plan
          :points))))

(defn load-bearing-to 
  "Get the bearing in degrees from one
  airport to another. Uses skyvector.com"
  [icao-from icao-to]
  (when-let [degrees (-> (calculate-path icao-from icao-to)
                         first
                         :th)] ;; "true degrees"; :mh for magnetic 
    (Integer/parseInt degrees)))

(defn load-vor
  "Lookup a VOR/Navaid/airport. An ICAO for an airport
  must be provided for reference since some symbols may
  be reused. Uses skyvector.com"
  [icao-from id]
  (-> (calculate-path icao-from id)
      second))

(defn get-bearing-to
  "See load-bearing-to. This method might cache results"
  [icao-from icao-to]
  (load-bearing-to icao-from icao-to))

(defn get-vor
  "See get-vor. This method might cache results"
  [icao-from id]
  (load-vor icao-from id))

(defn get-exit-to
  "Get the semantic exit name to an airport based on
  the bearing and the provided SOP data. The data should
  be formatted as follows:
  {:north [[290 360] [0 15]]
  :east [[16 95]]
  :south [[96 215]]
  :west [[216 289]]}"
  [sop-exits bearing]
  (-> (filter 
        (fn [[_ intervals]]
          (some 
            #(let [[lo hi] %]
               (<= lo bearing hi))
            intervals))
        sop-exits)
      first  ; get the only remaining exit
      first)); get the exit name
        
