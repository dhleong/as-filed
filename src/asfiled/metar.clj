(ns ^{:author "Daniel Leong"
      :doc "METAR decoder"}
  asfiled.metar
  (:require [clojure.string :refer [split]]
            [clj-time
             [core :as t]
             [format :as f]]))

;;
;; Utils
;;

(defn- as-int
  "Best-effort conversion to an Int"
  [raw]
  (try
    (Integer/parseInt raw)
    (catch NumberFormatException e
      raw)))

(defn decode-time
  [token]
  (let [today (t/today-at-midnight)
        base (f/parse (f/formatter "ddHHmmZ") token)]
    (t/date-time 
      (t/year today)
      (t/month today)
      (t/day base)
      (t/hour base)
      (t/minute base))))

(defn decode-wind
  [token]
  (let [speed (re-find #"(\d\d)(G(\d\d))?KT$" token)
        dir (re-find #"VRB|(\d\d\d)V?(\d\d\d)?" token)]
    {:speed (as-int (second speed))
     :gust (as-int (nth speed 3 nil))
     :dir (if (= "VRB" (first dir)) 
            :variable
            (as-int (second dir)))
     :dir-variable (if (last dir)
                     (map as-int (rest dir)))}))

(def metar-parts
  {:time [#"[0-9]Z" decode-time]
   :wind [#"[0-9GVRB]+KT" decode-wind]})


;;
;; Public API
;;

(defn decode-metar
  "Decode a metar"
  [metar]
  (let [parts (split metar #" +")
        icao (first parts)]
    parts))
