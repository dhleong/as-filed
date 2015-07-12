(ns ^{:author "Daniel Leong"
      :doc "SOPs for nyartcc"}
  asfiled.nyartcc-sop
  (:require [clojure.string :refer [upper-case]]))

(def sop-klga
  {:exit-intervals
   {:north [[290 360] [0 15]]
    :east [[16 95]]
    :south [[96 215]]
    :west [[216 289]]}
   :exit-gates
   {:north ["GAYEL" "HAAYS" "NEION" "COATE" "SAX" "NYACK"]
    :east ["GREKI" "MERIT" "BAYYS" "CMK" "BDR"]
    :south ["SHIPP" "WAVEY" "DIXIE" "WHITE" "DPK"]
    :west ["NEWEL" "ELIOT" "ZIMMZ!" "PARKE" "LANNA" "BIGGY" "SBJ"]}})

(def sop-map
  {"KLGA" sop-klga})

(defn get-sop [facility-icao]
  (get sop-map (upper-case facility-icao)))
