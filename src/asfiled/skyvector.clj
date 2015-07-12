(ns ^{:author "Daniel Leong"
      :doc "Utility methods using skyvector.com"}
  asfiled.skyvector
  (:require [org.httpkit.client :as http] 
            [cheshire.core :refer [parse-string]]))

(def calculate-url "http://skyvector.com/api/dataLayer")

(defn load-bearing-to 
  "Get the bearing in degrees from one
  airport to another. Uses skyvector.com"
  [icao-from icao-to]
  (let [path (str icao-from " " icao-to)
        options {:query-params {:cmd "planPts" :d path}}
        {:keys [err body]} @(http/get calculate-url options)]
    (when-let [json (parse-string body true)]
      (-> json
          :plan
          :points
          first
          :th ;; "true degrees"; :mh for magnetic
          Integer/parseInt))))

(defn get-bearing-to
  "See load-bearing-to. This method might cache results"
  [icao-from icao-to]
  (load-bearing-to icao-from icao-to))

(def sop-exits-klga
  {:north [[290 360] [0 15]]
   :east [[16 95]]
   :south [[96 215]]
   :west [[216 289]]})

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
        
