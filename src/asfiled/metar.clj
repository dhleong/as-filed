(ns ^{:author "Daniel Leong"
      :doc "METAR decoder"}
  asfiled.metar
  (:require [clojure.string :refer [split]]
            [clj-time
             [core :as t]
             [format :as f]]))

;;
;; Constants
;;

(def weather-types
  {"BR" "Mist"
   "FG" "Fog"
   "DU" "Dust"
   "HZ" "Haze"
   "SA" "Sand"
   "PY" "Spray"
   "FU" "Smoke"
   ;; "qualifiers"
   "MI" "Shallow"
   "BC" "Patches"
   "BL" "Blowing"
   "DR" "Low Drifting"
   "SH" "Showers"
   "TS" "Thunderstorms"
   "FZ" "Freezing"
   "PR" "Partial"
   ;; precipitation
   "DZ" "Drizzle"
   "RA" "Rain"
   "SN" "Snow"
   "SG" "Snow Grains"
   "IC" "Ice Crystals"
   "PL" "Ice Pellets"
   "GR" "Hail"
   "GS" "Small Hail/Snow Pellets"})

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

(defn- as-number
  "Best-effort conversion to a (possibly fractional) number"
  [raw]
  (if (.contains raw "/")
    (let [[numer denom] (split raw #"/")]
      (/ (as-int numer) (as-int denom)))
    ;; safe fallback
    (as-int raw)))

;;
;; Decoder parts
;;

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

(defn decode-visibility
  [token]
  (-> token
      (.substring 0 (- (count token) 2))
      as-number))

(defn decode-weather
  [token]
  (let [raw (->> token (take-last 2) (apply str))
        desc (get weather-types raw)]
    (case (first token)
      \+ (str "Heavy " desc)
      \- (str "Light " desc)
      desc)))

(def metar-parts
  {:time [#"[0-9]+Z" decode-time]
   :wind [#"[0-9GVRB]+KT" decode-wind]
   :visibility [#"[0-9/]+SM" decode-visibility]
   :weather [#"(+|-)?[A-Z]{2}" decode-weather]})


;;
;; Public API
;;

(defn decode-metar
  "Decode a metar"
  [metar]
  (let [parts (split metar #" +")
        icao (first parts)]
    parts))
